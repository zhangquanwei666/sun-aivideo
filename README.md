# Sun AI Video

前后端分离的 AI 短视频生成项目：调用阿里云百炼的**通义万相**生成视频，用**千问**（LangChain4j）润色提示词，并统计每条视频的**生成耗时**和**费用**。

```
sun-aivideo/
├── backend/                 Maven 父工程（Spring Boot 3.5 · Java 21）
│   ├── aivideo-common/      通用：枚举、请求 / 响应基类、业务异常 BizException
│   ├── aivideo-ai/          AI 调用（策略 + 工厂）：视频生成策略、大模型策略、提示词润色
│   ├── aivideo-dao/         数据访问（MyBatis-Plus）：实体、Mapper 接口、Mapper XML
│   ├── aivideo-server/      Web 服务：controller / service、费用与耗时统计（启动模块）
│   └── sql/                 数据库脚本（手动执行）
│       ├── ddl.sql          DDL：建表（t_model_config、t_video_task）
│       └── dml.sql          DML：初始化数据（视频模型与单价）
└── frontend/                Vue 3.5 · Vite 6 · Vue Router · Axios（暗色风格）
```

模块依赖：

```
aivideo-server ──► aivideo-ai ──► aivideo-common
       │                              ▲
       ├──► aivideo-dao               │
       └──────────────────────────────┘
```

`aivideo-ai` 和 `aivideo-dao` 互不依赖，也不依赖 server。在 IDEA 中添加 `backend/pom.xml` 作为 Maven 项目即可识别四个模块。

## 技术选型

| 项 | 选择 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 3.5.6 + Java 21 | |
| 数据访问 | MyBatis-Plus 3.5.7 + Mapper XML | 单表增删改用 `BaseMapper`，自定义查询统一写在 XML（不写注解 SQL）。各环境均为 MySQL（uat 使用阿里云 RDS） |
| 大模型 | LangChain4j 1.8.0（`langchain4j-open-ai`） | 策略 + 工厂：`QwenLlmStrategy` 通过百炼的 OpenAI 兼容接口调用千问 |
| 视频模型 | 百炼万相 HTTP API（Spring `RestClient`） | 策略 + 工厂：`DashScopeWanVideoStrategy`。LangChain4j 和 AgentScope 都没有封装视频生成 |
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

### aivideo-ai（`com.sun.aivideo.ai`）：策略 + 工厂

只负责调用 AI，不依赖数据库和 Web 层。

```
├── video/
│   ├── VideoGenerationStrategyFactory    工厂：收集所有策略，按 VideoProvider 取用；无 API Key 时返回 Mock
│   ├── strategy/
│   │   ├── VideoGenerationStrategy       策略接口：provider() / submit() / query()
│   │   ├── DashScopeWanVideoStrategy     百炼万相
│   │   └── MockVideoStrategy             本地模拟
│   └── model/                            VideoGenerateRequest、VideoSubmitResult、VideoTaskResult
├── llm/
│   ├── LlmStrategyFactory                工厂：按 aivideo.llm.provider 选择策略，不可用时退回 Mock
│   ├── PromptEnhancer                    提示词润色，通过工厂拿到策略后调用
│   ├── strategy/
│   │   ├── LlmStrategy                   策略接口：provider() / available() / modelName() / chat()
│   │   ├── QwenLlmStrategy               通义千问（LangChain4j）
│   │   └── MockLlmStrategy               本地模拟（不计费）
│   └── model/                            LlmChatRequest、LlmChatResponse、PromptEnhanceResult
├── config/       DashScopeProperties、LlmProperties、MockVideoProperties、AiClientConfig（RestClient）
└── exception/    AiClientException
```

扩展方式：
- **新增视频厂商**（可灵、火山引擎 Seedance、Google Veo 等）：
  1. 在 `VideoProvider` 中加一个枚举值，并写上中文名，例如 `KLING("快手可灵")`；
  2. 实现 `VideoGenerationStrategy` 并加上 `@Component`，在 `available()` 里判断该厂商的 API Key 是否已配置；
  3. 往 `t_model_config` 插入该厂商的模型和单价（`provider` 填枚举名）。

  工厂会自动收录新策略，前端的**厂商下拉框**也会自动出现这个厂商，server 不用改。只往表里插了模型、还没写策略的厂商，不会出现在前端。
- **厂商与模型的前端选择**：先选厂商，再选该厂商下的模型。每个厂商单独判断能否真实调用：已配置 Key 的走真实调用；没配置 Key 时，如果允许 Mock 就回退到 Mock（任务的"厂商"会记录为"本地模拟"），否则提交失败并提示未配置 Key。
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
3. 定时任务每 15 秒轮询一次百炼（`aivideo.polling.interval-ms: 15000`），成功后：
   - **视频费用** = 模型单价（元/秒，按分辨率） × 百炼返回的计费秒数 `usage.duration`
   - **润色费用** = 输入 tokens × 输入单价 + 输出 tokens × 输出单价
   - **总费用** = 视频费用 + 润色费用。任务失败时视频不计费，只保留润色费用。
   - **耗时**：排队 = `scheduled_time - submit_time`；生成 = `end_time - scheduled_time`；总耗时 = 百炼 `end_time` - 用户点击生成的时间
   - 自动把视频下载到 `<工作目录>/data/files/videos/`（本地运行时为 `backend/aivideo-server/data/`），因为百炼的 `video_url` 只有 24 小时有效。

单价存在 `t_model_config` 表里（初始数据见 `backend/sql/dml.sql`），可以直接在库里改。

## 环境

| 环境 | 后端 profile | 前端 | 数据库 | 没有 API Key 时 | 日志 |
|---|---|---|---|---|---|
| 测试 / 本地开发 | `uat`（**后端默认**） | 开发 `npm run dev`，打包 `npm run build:uat` | 阿里云 RDS `aivideo` | 走 Mock | 控制台，debug |
| 生产 | `pro` | `npm run build:pro` | 全部来自环境变量 | **启动失败** | 文件，保留 30 天 |

本地开发直接使用 `uat` 环境，不再单独设置本地环境。

所有环境启动时都**不会自动建表**，启动过程也不连数据库。首次使用前，请在对应的库中手动执行：

| 顺序 | 文件 | 类型 | 做什么 |
|---|---|---|---|
| 1 | `backend/sql/ddl.sql` | DDL | 建表：`t_model_config`（视频模型配置与单价）、`t_video_task`（视频生成任务，含耗时与费用） |
| 2 | `backend/sql/dml.sql` | DML | 初始化数据：插入 4 个通义万相模型及各分辨率单价（`INSERT IGNORE`，重复执行不会覆盖已有数据） |

两个脚本都可以重复执行：建表用 `CREATE TABLE IF NOT EXISTS`，数据用 `INSERT IGNORE`。每条语句和每个字段在脚本里都有注释说明用途。以后新增或修改表结构写进 `ddl.sql`，新增初始数据写进 `dml.sql`。

后端配置文件在 `backend/aivideo-server/src/main/resources/`：`application.yml` 放公共配置，`application-{uat,pro}.yml` 放各环境的差异。AI 模块的配置项（`aivideo.dashscope.*`、`aivideo.llm.*`、`aivideo.mock.*`）也写在这些文件里，由 `aivideo-ai` 中的 Properties 类读取。前端配置在 `frontend/.env.development`、`.env.uat`、`.env.production`。左侧导航栏会显示当前后端环境（UAT / PRO）。

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
| `DASHSCOPE_API_KEY` | 可选 | **必填** | 百炼 API Key，uat 不配时走 Mock |
| `MYSQL_PASSWORD` | **必填** | **必填** | uat 未配置时为占位符 `XXXX`，会连接失败 |
| `MYSQL_HOST` | 默认 `rm-bp12mcri2l7712aw2io.mysql.rds.aliyuncs.com` | **必填** | |
| `MYSQL_USER` | 默认 `aivideo` | **必填** | |
| `MYSQL_DB` / `MYSQL_PORT` | 默认 `aivideo` / `3306` | 默认 `aivideo` / `3306` | |
| `AIVIDEO_DATA_DIR` | 默认 `./data` | 默认 `/data/aivideo` | 上传的图片和下载的视频存放目录 |
| `AIVIDEO_LOG_FILE` | — | 默认 `/data/aivideo/logs/sun-aivideo.log` | |
| `AIVIDEO_CORS_ORIGINS` | — | 可选 | 跨域部署时的前端域名，逗号分隔 |
| `SERVER_PORT` | 18088 | 18088 | 后端端口 |

pro 启动时会检查配置：没配 `DASHSCOPE_API_KEY`、数据库密码为空或仍是 `XXXX`、缺少 `MYSQL_HOST` / `MYSQL_USER` 时，直接启动失败并提示缺哪一项。

## 运行

### 本地开发（UAT 环境）

启动前请确认表已经手动建好（见上文"环境"一节），并且 RDS 白名单里有本机的公网 IP。

在 IDEA 中运行 `aivideo-server` 模块的 `AiVideoApplication`：Active profiles 不填（默认就是 `uat`），在 Environment variables 中填 `MYSQL_PASSWORD`（需要真实调用百炼时再加 `DASHSCOPE_API_KEY`），工作目录设为 `backend/aivideo-server`。

命令行启动（在 `backend` 父工程目录执行即可，会自动编译所有模块）：

```bash
$env:MYSQL_PASSWORD = "你的密码"
```

```bash
cd backend
mvn spring-boot:run
```

前端需要 Node 18 及以上版本（项目里写了 `.nvmrc`，对应 24.16.0）：

```bash
nvm use 24.16.0
cd frontend
npm install
npm run dev
```

打开 http://localhost:25173 。`/api` 和 `/files` 会代理到 `VITE_PROXY_TARGET`（默认 `http://localhost:18088`）。

### UAT 部署

后端用默认的 `uat` 环境启动（同上，设置好 `MYSQL_PASSWORD` 和 `DASHSCOPE_API_KEY`）；前端用 `npm run build:uat` 打包，部署方式与生产相同（见下文 Nginx 配置）。

### PRO 生产部署

1. 在生产库手动执行 `backend/sql/ddl.sql`（建表），再执行 `backend/sql/dml.sql`（初始数据）。
2. 打包后端，并用生产配置启动：

```bash
cd backend
mvn clean package -DskipTests
```

```bash
SPRING_PROFILES_ACTIVE=pro DASHSCOPE_API_KEY=sk-xxx MYSQL_HOST=xxx MYSQL_USER=xxx MYSQL_PASSWORD=xxx java -jar aivideo-server/target/sun-aivideo-server.jar
```

3. 打包前端：执行 `npm run build:pro`，产物在 `frontend/dist`。推荐用 Nginx 和后端同域部署，这样 `/files` 下的视频和图片也能直接访问，不需要配置跨域：

```nginx
server {
    listen 80;
    server_name video.example.com;
    root /usr/share/nginx/html/sun-aivideo;   # frontend/dist
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
| GET | `/api/videos/stats` | — | `VideoStatsVO` | 费用与耗时汇总，含按模型统计 |
| GET | `/api/models` | — | `ModelListVO` | 可选的厂商（含是否已配置 Key）与模型（含参数范围、单价） |
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

图生视频把 `modelCode` 换成 `wan2.7-i2v` / `wan2.6-i2v` / `wan2.6-i2v-flash`，再传 `imageUrl`。它可以是公网地址，也可以是上传接口返回的 `/files/...` 路径；本地路径由后端转成 base64 提交。

## 注意

- 单价取自百炼华北2（北京）官方原价，不含限时优惠。`wan2.7-i2v` 和 `wan2.6-i2v-flash` 的单价还没有核实，请以百炼控制台为准并在 `t_model_config` 中修改。
- 千问润色的单价在 `application.yml` 的 `aivideo.llm.*-price-per-million` 中配置。
