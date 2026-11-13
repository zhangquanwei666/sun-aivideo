# Sun AI Video

前后端分离的 AI 短视频生成项目：视频生成支持阿里云百炼（**通义万相**）和 Google Gemini（**Omni Flash**）两家切换；**文生图**支持阿里云百炼（万相 2.7 图像）和 Google Gemini（Nano Banana）两家切换，生成的图片可以一键作为视频首帧；用户在“模型密钥”中维护自己的厂商 Key；用**千问**（LangChain4j）润色提示词，并统计每条视频 / 图片的**生成耗时**和**参考费用**。

```
sun-aivideo/
├── backend/                 Maven 父工程（Spring Boot 3.5 · Java 21）
│   ├── aivideo-common/      通用：枚举、请求 / 响应基类、业务异常 BizException
│   ├── aivideo-ai-core/          AI 核心：统一模型、策略接口、工厂、Mock、提示词润色编排
│   ├── aivideo-ai-channel-dashscope/ 阿里云百炼：万相视频、万相图片、千问润色及客户端配置
│   ├── aivideo-ai-channel-gemini/   Google Gemini：Omni Flash 视频、Nano Banana 图片及客户端配置
│   ├── aivideo-dao/         数据访问（MyBatis-Plus）：实体、Mapper 接口、Mapper XML
│   ├── aivideo-server/      前台服务：用户登录、图片/视频生成、费用与耗时统计
│   ├── aivideo-admin/       管理服务：管理员登录、用户管理、全站生成记录
│   └── sql/                 数据库脚本（手动执行）
│       ├── ddl.sql          DDL：建表（视频：t_model_config、t_video_task；文生图：t_image_model_config、t_image_task）
│       └── dml.sql          DML：初始化数据（视频模型与单价、文生图模型与单价）
└── frontend/
    ├── user-frontend/       用户前台 Vue 3.5 · Vite 6（图片/视频生成）
    └── admin-frontend/      管理后台 Vue 3.5 · Vite 6（用户、生成记录）
```

用户密钥的部署、数据库结构及接口详见 [用户厂商密钥](docs/user-channel-keys.md)。已有环境已完成凭证表和任务字段升级；用户服务的加密主密钥见配置文件。

会员功能已移除。已有数据库的清理脚本为 [20261113_remove_membership.sql](backend/sql/20261113_remove_membership.sql)，发布与执行顺序见 [管理后台说明](docs/admin-api.md)。新建库的 `ddl.sql` 已同步移除相关表和字段。

模块依赖：

```
aivideo-server
├──► aivideo-ai-channel-dashscope ──► aivideo-ai-core
├──► aivideo-ai-channel-gemini ─────► aivideo-ai-core
├──► aivideo-ai-core ──► aivideo-common
└──► aivideo-dao ──► aivideo-common

aivideo-admin ──► aivideo-dao ──► aivideo-common
```

`aivideo-ai-core` 和 `aivideo-dao` 互不依赖，也不依赖 server。两个厂商模块依赖 `aivideo-ai-core`，彼此不依赖；HTTP、JSON 和 LangChain4j 等调用依赖放在对应厂商模块。前台服务默认端口为 18088，管理服务默认端口为 18089。在 IDEA 中添加 `backend/pom.xml` 作为 Maven 项目即可识别七个模块。

## 技术选型

| 项 | 选择 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 3.5.6 + Java 21 | |
| 数据访问 | MyBatis-Plus 3.5.7 + Mapper XML | 单表增删改用 `BaseMapper`，自定义查询统一写在 XML（不写注解 SQL）。各环境均为 MySQL（uat 使用阿里云 RDS） |
| 大模型 | LangChain4j 1.8.0（`langchain4j-open-ai`） | 策略 + 工厂：`QwenLlmStrategy` 通过百炼的 OpenAI 兼容接口调用千问 |
| 视频模型 | 百炼万相 HTTP API、Gemini Omni Flash（Interactions API），均用 Spring `RestClient` | 策略 + 工厂：`DashScopeWanVideoStrategy`、`GeminiOmniVideoStrategy`、`MockVideoStrategy`。LangChain4j 和 AgentScope 都没有封装视频生成 |
| 文生图 | 百炼万相 2.7 图像、Gemini Nano Banana（HTTP 同步接口） | 策略 + 工厂：`DashScopeWanImageStrategy`、`GeminiNanoBananaStrategy`、`MockImageStrategy` |
| 前端 | Vue 3 + Vite 6 | 无 UI 库，自写暗色组件 |

## 后端模块与分层

### aivideo-common（`com.sun.aivideo.common`）

不依赖 Spring，各模块共用。

```
├── base/         BaseRequest（入参基类）、BasePageRequest（分页入参基类）、BaseResponse（出参基类）
├── enums/        TaskStatus、VideoMode、ApiProtocol、VideoProvider、VideoTaskState、LlmProvider
└── exception/    BizException（业务异常）
```

入参和出参的继承关系：

| 基类 | 子类 |
|---|---|
| `BaseRequest`（`requestId`，可选） | `VideoGenerateDTO`、`VideoDetailDTO`、`ImageUploadDTO` |
| `BasePageRequest`（继承 `BaseRequest`，带 `pageNum` / `pageSize` 及校验） | `VideoTaskQueryDTO` |
| `BaseResponse`（`code` / `message`） | `VideoTaskVO`、`PageVO<T>`、`VideoStatsVO`、`ModelListVO`、`UploadVO`、`SystemStatusVO` |

`ModelConfigVO`、`VideoTaskVO.CostVO` 这类只作为列表元素或嵌套字段的对象，不单独返回，所以不继承 `BaseResponse`。

### aivideo-ai-core（`com.sun.aivideo.ai`）：AI 调用公共核心

提供统一请求 / 结果模型、策略接口、工厂、调用凭证上下文、Mock 和提示词润色编排，不依赖具体厂商模块、数据库和 Web 层。真实模型调用放在对应的厂商模块。

```
├── video/
│   ├── VideoGenerationStrategyFactory    工厂：收集所有策略，按 VideoProvider 取用；凭证由调用方逐次传入
│   ├── strategy/
│   │   ├── VideoGenerationStrategy       策略接口：provider() / submit() / query()
│   │   └── MockVideoStrategy             本地模拟
│   └── model/                            VideoGenerateRequest、VideoSubmitResult、VideoTaskResult
├── llm/
│   ├── LlmStrategyFactory                工厂：按 aivideo.llm.provider 选择策略；Mock 需显式开启
│   ├── PromptEnhancer                    提示词润色，通过工厂拿到策略后调用
│   ├── strategy/
│   │   ├── LlmStrategy                   策略接口：provider() / modelName() / chat()
│   │   └── MockLlmStrategy               本地模拟（不计费）
│   └── model/                            LlmChatRequest、LlmChatResponse、PromptEnhanceResult
├── image/        文生图策略接口、工厂、统一模型、MockImageStrategy
├── auth/         ChannelCredentials 调用凭证上下文、ProviderErrors 厂商错误转换
├── config/       AiCoreConfig、LlmProperties、MockVideoProperties
└── exception/    AiClientException
```

厂商模块各自管理调用实现、配置项与 HTTP 客户端：

```text
aivideo-ai-channel-dashscope/（com.sun.aivideo.ai.dashscope）
├── config/       DashScopeProperties、DashScopeClientConfig
├── video/        DashScopeWanVideoStrategy
├── image/        DashScopeWanImageStrategy
└── llm/          QwenLlmStrategy（LangChain4j）

aivideo-ai-channel-gemini/（com.sun.aivideo.ai.gemini）
├── config/       GeminiProperties、GeminiClientConfig
├── video/        GeminiOmniVideoStrategy
└── image/        GeminiNanoBananaStrategy
```

`aivideo-server` 引入 AI 核心和两个厂商模块，Spring 自动扫描并注册策略。配置仍统一放在 server 的 `application*.yml`，接口地址、代理和超时配置保留；API Key 改由用户在前台维护，不再从全局环境变量读取。

扩展方式：
- **新增视频厂商**（可灵、火山引擎 Seedance 等）：
  1. 在 `VideoProvider` 中加一个枚举值，并写上中文名，例如 `KLING("快手可灵")`；
  2. 新建 `aivideo-ai-channel-厂商名` Maven 模块，依赖 `aivideo-ai-core`，在父工程注册并由 server 引入；在厂商包内实现 `VideoGenerationStrategy` 并加上 `@Component`，按接口的凭证参数设置本次调用的鉴权信息；
  3. 往 `t_model_config` 插入该厂商的模型和单价（`provider` 填枚举名）。

  工厂会自动收录新策略，前端的**厂商下拉框**也会自动出现这个厂商，server 不用改。只往表里插了模型、还没写策略的厂商，不会出现在前端。
- **厂商与模型的前端选择**：先选厂商，再选该厂商下的模型。平台显示已接入厂商，按当前用户的凭证配置判断可用性。缺少或停用个人 Key 时提示前往“模型密钥”配置；不会自动回退 Mock 或使用平台 Key。
- **新增大模型**（DeepSeek 等）：在 `LlmProvider` 中加一个枚举值，实现 `LlmStrategy` 并加上 `@Component`，再把 `aivideo.llm.provider` 改成新的厂商。

### aivideo-dao（`com.sun.aivideo.dao` / `entity`）：MyBatis-Plus + XML

```
├── entity/                    VideoTask、ModelConfig（@TableName / @TableField 用于 BaseMapper 单表操作）
├── dao/
│   ├── VideoTaskMapper        extends BaseMapper<VideoTask>
│   ├── ModelConfigMapper      extends BaseMapper<ModelConfig>
│   ├── bo/                    VideoStatsBO、ModelStatBO（统计查询结果）
│   └── config/                MybatisPlusConfig（@MapperScan、分页插件、时间自动填充）
└── resources/
    └── mapper/
        ├── VideoTaskMapper.xml     namespace = com.sun.aivideo.dao.VideoTaskMapper
        └── ModelConfigMapper.xml   namespace = com.sun.aivideo.dao.ModelConfigMapper
```

建表和初始化数据的 SQL 不在这个模块里，统一放在 `backend/sql/`（见下文"环境"一节）。

Mapper 接口与 XML 的对应关系（XML 位置由 `mybatis-plus.mapper-locations: classpath*:mapper/**/*.xml` 指定）：

| Mapper 方法 | XML id | resultMap | 说明 |
|---|---|---|---|
| `VideoTaskMapper#selectTaskPage` | `selectTaskPage` | `BaseResultMap` | 分页 + 状态 / 模型 / 关键词筛选（分页由 MyBatis-Plus 插件处理） |
| `VideoTaskMapper#selectActiveTasks` | `selectActiveTasks` | `BaseResultMap` | 需要轮询厂商的任务 |
| `VideoTaskMapper#selectStats` | `selectStats` | `VideoStatsResultMap` | 费用与耗时汇总 |
| `VideoTaskMapper#selectStatsByModel` | `selectStatsByModel` | `ModelStatResultMap` | 按模型分组统计 |
| `ModelConfigMapper#selectEnabledList` | `selectEnabledList` | `BaseResultMap` | 已启用的模型 |
| `ModelConfigMapper#selectByModelCode` | `selectByModelCode` | `BaseResultMap` | 按编码查模型 |
| `insert` / `updateById` / `selectById` / `selectList` | — | — | MyBatis-Plus `BaseMapper` 自带 |

### aivideo-server（`com.sun.aivideo`）

启动模块，配置文件 `application*.yml` 在这里。

```
├── controller/   VideoController、ModelController、FileController、SystemController
├── service/      接口 + impl/；VideoTaskWorker（通过策略工厂提交、轮询任务）
├── model/
│   ├── dto/      入参：VideoGenerateDTO、VideoTaskQueryDTO
│   ├── vo/       出参：VideoTaskVO、VideoStatsVO、ModelConfigVO …
│   └── bo/       业务对象：VideoCostBO
├── converter/    Entity → VO
├── handler/      GlobalExceptionHandler
├── job/          VideoTaskPollingJob 定时轮询
└── config/       AiVideoProperties、EnvironmentChecker、WebConfig、AsyncConfig、JacksonConfig
```

## 生成流程与统计口径

1. `POST /api/videos`：校验参数，写入任务（状态 `SUBMITTING`），立即返回。
2. 后台线程：可选的千问润色（记录耗时和 tokens），然后调用百炼创建任务，状态变为 `PENDING`。
3. 前端每 10 秒刷新任务状态；后端定时任务在每轮查询结束后间隔 10 秒轮询厂商（`aivideo.polling.interval-ms: 10000`），成功后：
   - **视频费用** = 模型单价（元/秒，按分辨率） × 百炼返回的计费秒数 `usage.duration`
   - **润色费用** = 输入 tokens × 输入单价 + 输出 tokens × 输出单价
   - **总费用** = 视频费用 + 润色费用。任务失败时视频不计费，只保留润色费用。
   - **耗时**：排队 = `scheduled_time - submit_time`；生成 = `end_time - scheduled_time`；总耗时 = 百炼 `end_time` - 用户点击生成的时间
   - 自动把视频下载到 `<工作目录>/data/files/videos/`（本地运行时为 `backend/aivideo-server/data/`），因为百炼的 `video_url` 只有 24 小时有效。

单价存在 `t_model_config` 表里（初始数据见 `backend/sql/dml.sql`），可以直接在库里改。

## 环境

| 环境 | 后端 profile | 前端 | 数据库 | 没有 API Key 时 | 日志 |
|---|---|---|---|---|---|
| 测试 / 本地开发 | `uat`（**后端默认**） | 开发 `npm run dev`，打包 `npm run build:uat` | 阿里云 RDS `aivideo` | 提示配置个人 Key；Mock 需显式开启 | 控制台，debug |
| 生产 | `pro` | `npm run build:pro` | 全部来自环境变量 | 提示配置个人 Key | 文件，保留 30 天 |

本地开发直接使用 `uat` 环境，不再单独设置本地环境。

所有环境启动时都**不会自动建表**，启动过程也不连数据库。首次使用前，请在对应的库中手动执行：

| 顺序 | 文件 | 类型 | 做什么 |
|---|---|---|---|
| 1 | `backend/sql/ddl.sql` | DDL | 建表：`t_model_config`（视频模型配置与单价）、`t_video_task`（视频生成任务，含耗时与费用） |
| 2 | `backend/sql/dml.sql` | DML | 初始化数据：插入 4 个通义万相模型及各分辨率单价（`INSERT IGNORE`，重复执行不会覆盖已有数据） |

两个脚本都可以重复执行：建表用 `CREATE TABLE IF NOT EXISTS`，数据用 `INSERT IGNORE`。每条语句和每个字段在脚本里都有注释说明用途。以后新增或修改表结构写进 `ddl.sql`，新增初始数据写进 `dml.sql`。

后端配置文件在 `backend/aivideo-server/src/main/resources/`：`application.yml` 放公共配置，`application-{uat,pro}.yml` 放各环境的差异。AI 配置项也写在这些文件里：`aivideo.llm.*`、`aivideo.mock.*` 由 AI 核心模块的 Properties 类读取，`aivideo.dashscope.*`、`aivideo.gemini.*` 分别由对应厂商模块的 Properties 类读取。用户前台配置在 `frontend/user-frontend/.env.*`，管理前端配置在 `frontend/admin-frontend/.env.*`。

切换后端环境：

```bash
# 方式一：环境变量
$env:SPRING_PROFILES_ACTIVE = "pro"
```

```bash
# 方式二：启动参数
java -jar sun-aivideo-server.jar --spring.profiles.active=pro
```

### 环境变量

| 变量 | uat | pro | 说明 |
|---|---|---|---|
| `AIVIDEO_CREDENTIAL_MASTER_KEY` | 可选覆盖 | 可选覆盖 | 默认使用 application.yml 中已生成的固定主密钥；覆盖时需为 Base64 编码的 32 字节密钥 |
| `AIVIDEO_CREDENTIAL_KEY_ID` | 默认 `v1` | 默认 `v1` | 主密钥版本标识 |
| `AIVIDEO_MOCK_ENABLED` | 默认 `false` | 禁止 `true` | 显式开发模拟模式，缺少个人 Key 不自动模拟 |
| `GEMINI_PROXY_HOST` / `GEMINI_PROXY_PORT` | 可选 | 可选 | 国内直连 Google 不通时的 HTTP 代理，例如 `127.0.0.1` / `7890`；也可以把 `aivideo.gemini.base-url` 改成中转地址 |
| `MYSQL_PASSWORD` | **必填** | **必填** | uat 未配置时为占位符 `XXXX`，会连接失败 |
| `MYSQL_HOST` | 默认 `rm-bp12mcri2l7712aw2io.mysql.rds.aliyuncs.com` | **必填** | |
| `MYSQL_USER` | 默认 `aivideo` | **必填** | |
| `MYSQL_DB` / `MYSQL_PORT` | 默认 `aivideo` / `3306` | 默认 `aivideo` / `3306` | |
| `REDIS_PASSWORD` | **必填** | **必填** | uat 未配置时为占位符 `XXXX`，用于 Sa-Token 会话存储 |
| `REDIS_HOST` / `REDIS_PORT` | 默认阿里云 Redis / `6379` | **必填** | |
| `REDIS_USERNAME` | 默认 `aivideo` | **必填** | |
| `AIVIDEO_DATA_DIR` | 默认 `./data` | 默认 `/data/aivideo` | 上传的图片和下载的视频存放目录 |
| `AIVIDEO_LOG_FILE` | — | 默认 `/data/aivideo/logs/sun-aivideo.log` | |
| `AIVIDEO_CORS_ORIGINS` | — | 可选 | 跨域部署时的前端域名，逗号分隔 |
| `SERVER_PORT` | 18088 | 18088 | 后端端口 |

pro 启动时会检查配置：加密主密钥未配置或格式不正确、数据库密码为空或仍是 `XXXX`、缺少 `MYSQL_HOST` / `MYSQL_USER` 时，直接启动失败。个人 Key 缺失只影响该用户的模型调用。

## 运行

### 本地开发（UAT 环境）

启动前请确认表已经手动建好（见上文"环境"一节），并且 RDS 白名单里有本机的公网 IP。

在 IDEA 中运行 `aivideo-server` 模块的 `AiVideoApplication`：Active profiles 不填（默认就是 `uat`），在 Environment variables 中填 `MYSQL_PASSWORD` 和 `REDIS_PASSWORD`，工作目录设为 `backend/aivideo-server`。加密主密钥默认读取 `application.yml` 中已生成的固定值，也可用 `AIVIDEO_CREDENTIAL_MASTER_KEY` 覆盖。

命令行启动（PowerShell，在项目根目录执行；会自动编译前台服务依赖的模块）：

```powershell
$env:MYSQL_PASSWORD = "你的 MySQL 密码"
$env:REDIS_PASSWORD = "你的 Redis 密码"
Set-Location backend
mvn -pl aivideo-server -am spring-boot:run
```

前端需要 Node 18 及以上版本（项目里写了 `.nvmrc`，对应 24.16.0）：

```bash
nvm use 24.16.0
cd frontend/user-frontend
npm install
npm run dev
```

打开 http://localhost:25173 。`/api` 和 `/files` 会代理到 `VITE_PROXY_TARGET`（默认 `http://localhost:18088`）。

如需同时启动管理后台服务，请另开一个 PowerShell 窗口，在项目根目录执行：

```powershell
$env:MYSQL_PASSWORD = "你的 MySQL 密码"
$env:REDIS_PASSWORD = "你的 Redis 密码"
$env:AIVIDEO_ADMIN_PASSWORD = "首次初始化管理员的强密码"
Set-Location backend
mvn -pl aivideo-admin -am spring-boot:run
```

管理员已经初始化后，`AIVIDEO_ADMIN_PASSWORD` 可以不再设置。管理前端使用 `npm run dev:admin` 启动，访问 http://localhost:25174 。

### UAT 部署

后端用默认的 `uat` 环境启动（同上，设置好 `MYSQL_PASSWORD`、`REDIS_PASSWORD`，主密钥默认读取配置文件）；前端用 `npm run build:uat` 打包，部署方式与生产相同（见下文 Nginx 配置）。

### PRO 生产部署

1. 在生产库手动执行 `backend/sql/ddl.sql`（建表），再执行 `backend/sql/dml.sql`（初始数据）。
2. 打包后端，并用生产配置启动：

```bash
cd backend
mvn clean package -DskipTests
```

```bash
SPRING_PROFILES_ACTIVE=pro AIVIDEO_CREDENTIAL_MASTER_KEY=已安全保存的Base64主密钥 MYSQL_HOST=xxx MYSQL_USER=xxx MYSQL_PASSWORD=xxx REDIS_HOST=xxx REDIS_USERNAME=xxx REDIS_PASSWORD=xxx java -jar aivideo-server/target/sun-aivideo-server.jar
```

3. 在 `frontend/user-frontend` 执行 `npm run build:pro`，产物在 `frontend/user-frontend/dist`。推荐用 Nginx 和后端同域部署，这样 `/files` 下的视频和图片也能直接访问，不需要配置跨域：

```nginx
server {
    listen 80;
    server_name video.example.com;
    root /usr/share/nginx/html/sun-aivideo;   # frontend/user-frontend/dist
    client_max_body_size 16m;

    location / { try_files $uri $uri/ /index.html; }
    location /api/   { proxy_pass http://127.0.0.1:18088; }
    location /files/ { proxy_pass http://127.0.0.1:18088; }
}
```

UAT 的前端用 `npm run build:uat` 打包，部署方式相同。

## 接口

| 方法 | 路径 | 入参 | 出参 | 说明 |
|---|---|---|---|---|
| POST | `/api/videos` | `VideoGenerateDTO`（JSON） | `VideoTaskVO` | 创建视频生成任务 |
| GET | `/api/videos/{id}` | `VideoDetailDTO` | `VideoTaskVO` | 任务详情（状态、视频地址、费用、耗时） |
| GET | `/api/videos?pageNum=&pageSize=&status=&modelCode=&keyword=` | `VideoTaskQueryDTO` | `PageVO<VideoTaskVO>` | 分页查询 |
| GET | `/api/videos/stats` | — | `VideoStatsVO` | 费用与耗时汇总，含按模型统计（包含已删除的任务） |
| DELETE | `/api/videos/{id}` | `VideoDeleteDTO` | `BaseResponse` | 删除视频（逻辑删除）：只能删除已结束的任务，删除后列表和详情不再返回 |
| GET | `/api/models` | — | `ModelListVO` | 可选的视频厂商（含是否已配置 Key）与模型（含参数范围、单价） |
| POST | `/api/images` | `ImageGenerateDTO`（JSON） | `ImageTaskVO` | 创建文生图任务（可带参考图） |
| GET | `/api/images/{id}` | `ImageIdDTO` | `ImageTaskVO` | 图片任务详情（状态、图片地址、费用、耗时） |
| DELETE | `/api/images/{id}` | `ImageIdDTO` | `BaseResponse` | 删除图片（逻辑删除，规则同视频） |
| GET | `/api/images/stats` | — | `ImageStatsVO` | 文生图费用与耗时汇总，含按模型统计 |
| GET | `/api/image-models` | — | `ImageModelListVO` | 可选的文生图厂商与模型（含清晰度、画幅、参考图张数、单价） |
| GET | `/api/feed?pageNum=&pageSize=` | `FeedQueryDTO` | `PageVO<FeedItemVO>` | 生成记录：视频与图片合并，按时间倒序（不含已删除） |
| POST | `/api/files/images` | `ImageUploadDTO`（multipart，字段名 `file`） | `UploadVO` | 上传图生视频的首帧图片 |
| GET | `/api/system/status` | — | `SystemStatusVO` | 当前环境、是否为 Mock 模式、润色是否可用 |

响应格式：所有响应都继承 `BaseResponse`，`code` / `message` 和业务字段平铺在同一层，`code = 0` 表示成功。

```json
// 成功：POST /api/files/images
{ "code": 0, "message": "success", "url": "/files/images/20261111/xxx.png", "fileName": "a.png", "size": 1024 }

// 成功：GET /api/models
{ "code": 0, "message": "success",
  "providers": [ { "code": "DASHSCOPE", "name": "阿里云百炼", "available": true, "mockFallback": true, "modelCount": 4 } ],
  "models": [ { "modelCode": "wan2.7-t2v", "displayName": "万相 2.7 文生视频", "provider": "DASHSCOPE", "providerName": "阿里云百炼", "...": "..." } ] }

// 失败：任何接口
{ "code": 400, "message": "每页最多 100 条" }
```

分页结果里 `records` 的元素不带 `code` / `message`。

`POST /api/videos` 请求示例（`requestId` 可选，来自 `BaseRequest`）：

```json
{
  "requestId": "req-001",
  "modelCode": "wan2.7-t2v",
  "prompt": "雨夜的霓虹街头，一只橘猫踩着水洼奔跑，电影感",
  "resolution": "720P",
  "ratio": "9:16",
  "duration": 5,
  "enhancePrompt": true,
  "promptExtend": true
}
```

图生视频把 `modelCode` 换成 `wan2.7-i2v` / `wan2.6-i2v` / `wan2.6-i2v-flash` / `wan2.2-i2v-plus` / `wan2.2-i2v-flash`，再传 `imageUrl`。它可以是公网地址，也可以是上传接口返回的 `/files/...` 路径；本地路径由后端转成 base64 提交。

## 文生图（阿里云百炼 / Google Gemini）

- **入口**：生成页输入框的"生成方式"选择**文生图**，再选厂商（阿里云百炼 / Google Gemini）、模型、清晰度（1K / 2K / 4K）和画幅；可上传参考图（数量上限由模型决定）。
- **初始模型**（`dml.sql`，可直接改表）：

  | 厂商 | 模型 | 单价（每张） |
  |---|---|---|
  | 阿里云百炼 | 万相 2.7 图像 `wan2.7-image` | ¥0.2（1K / 2K） |
  | 阿里云百炼 | 万相 2.7 图像 Pro `wan2.7-image-pro` | ¥0.5（1K / 2K / 4K） |
  | Google Gemini | Nano Banana 2 `gemini-3.1-flash-image` | $0.067 / $0.101 / $0.151（1K / 2K / 4K） |
  | Google Gemini | Nano Banana Pro `gemini-3-pro-image` | $0.134 / $0.134 / $0.24（1K / 2K / 4K） |

- **计价**：图片按张计费。美元计价的模型按 `aivideo.cost.usd-to-cny`（默认 7.2）折算成人民币后计入统计。
- **流程**：提交后后台线程执行：可选的千问润色 → 同步调用厂商生成 → 图片立刻保存到本地 `/files/generated/...`（百炼返回的地址 24 小时失效）→ 记录费用和耗时。图片是同步生成的，不需要定时轮询；服务重启时会把中断的任务标记为失败。
- **用作首帧**：图片卡片上的"生成视频"按钮，或详情里的"用作首帧生成视频"，会把这张图设为图生视频的首帧。
- **网络**：国内直连 Google 不通时，配置 `GEMINI_PROXY_HOST` / `GEMINI_PROXY_PORT` 走本机代理，或把 `aivideo.gemini.base-url` 换成中转地址。
- **新增文生图厂商**：在 `ImageProvider` 加枚举值，实现 `ImageGenerationStrategy` 并加 `@Component`，再往 `t_image_model_config` 插入模型和单价。

## Gemini 视频生成（Omni Flash）

- **为什么不是 Veo**：Gemini API 中的 Veo 已于 2026-10-22 下线，官方替代为 Omni Flash，所以接入的是 Omni Flash。
- **入口**：生成页选择"文生视频"或"图生视频"，厂商选 **Google Gemini**，模型选 **Gemini Omni Flash**。同一个模型同时支持两种方式（`t_model_config.mode` 填 `T2V,I2V`）。
- **参数**：720P / 1080P，画幅 16:9 / 9:16（图生视频也可以选），3~15 秒；**带首帧图片时最长 10 秒**（前端时长列表自动收窄，后端也会校验）。视频自带原生音频。
- **计价**：`currency = USD`，720P $0.07/秒、1080P $0.104/秒（单价待核实，以 Google 官方定价为准，可直接改表）。按 `aivideo.cost.usd-to-cny` 折算成人民币后计入费用统计；详情和用量页的单价显示美元原价。
- **调用方式**：`POST {video-base-url}/interactions`，同步等待生成完成（读取超时 `video-read-timeout-seconds`，默认 600 秒）。返回的视频内容（base64 或下载地址）**直接保存到本地** `/files/videos/...`，后续轮询直接从内存取结果；服务重启导致缓存丢失时，改用 `GET /interactions/{id}` 查询状态。
- **网络**：与文生图一样，国内需配置 `GEMINI_PROXY_HOST` / `GEMINI_PROXY_PORT`，或把 `aivideo.gemini.video-base-url` 换成中转地址。

## 删除视频（逻辑删除）

- 前端：视频 / 图片卡片悬停时右上角出现删除按钮，详情面板里也有"删除"按钮，确认后删除。图片的删除规则与视频相同。
- 规则：
  - 只能删除**已结束**的任务（成功 / 失败 / 取消 / 超时）。生成中的任务不能删除，避免后台还在轮询计费、记录却已经隐藏。
  - 删除只把 `t_video_task.deleted` 置为 1，并记录 `deleted_at`，数据库记录和本地视频文件都保留。
  - 删除后：生成记录、任务明细和详情接口都不再返回这条任务；**用量与费用统计仍然计入**，因为参考费用需要保留，实际扣费以厂商账单为准。
- 实现：实体 `VideoTask.deleted` 加了 `@TableLogic`，`BaseMapper` 的方法会自动带上 `deleted = 0` 条件；XML 里的自定义查询都显式写了 `deleted = 0`，删除操作由 XML 中的 `softDeleteById` 完成。
- 已经建过表的库需要先加字段，执行 `backend/sql/ddl.sql` 末尾"表结构变更记录"里的 `ALTER TABLE` 语句。

## 注意

- 单价取自百炼华北2（北京）官方原价，不含限时优惠。`wan2.7-i2v`、`wan2.6-i2v-flash` 和三个万相 2.2 模型的单价还没有核实，请以百炼控制台为准并在 `t_model_config` 中修改。

## 万相 2.2 低价模型

用来降低视频成本，和万相 2.7 共用当前用户的百炼 Key。

| 模型 | 生成方式 | 分辨率 | 单价（元/秒，待核实） | 5 秒 480P 约 |
|---|---|---|---|---|
| `wan2.2-t2v-plus` | 文生视频 | 480P / 1080P | 0.14 / 0.70 | ¥0.7 |
| `wan2.2-i2v-plus` | 图生视频 | 480P / 1080P | 0.14 / 0.70 | ¥0.7 |
| `wan2.2-i2v-flash` | 图生视频 | 480P / 720P / 1080P | 0.10 / 0.20 / 0.48 | ¥0.5 |

- 限制：时长**固定 5 秒**、**无声**；文生视频的画幅只有 16:9 / 9:16 / 1:1。
- 协议：文生视频是百炼旧版接口，不传 `resolution` / `ratio`，而是传 `parameters.size`（如 `832*480`），对应协议 `WAN_LEGACY_T2V`；图生视频沿用 `WAN_LEGACY_I2V`（`input.img_url` + `resolution`）。
- 已有的库执行一次 `backend/sql/dml.sql` 即可插入这三个模型（`INSERT IGNORE`，不影响已有数据，不需要改表结构）。
- 千问润色的单价在 `application.yml` 的 `aivideo.llm.*-price-per-million` 中配置。
