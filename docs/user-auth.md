# 用户登录与数据关联

沿用现有 t_user、UserMapper 和任务 user_id 设计。用户名唯一，密码 BCrypt 哈希；普通用户注册后可生成图片和视频。会员字段保留，本次不增加会员付费门槛，管理后台未纳入本次实现。

## 业务规则
- 注册、登录公开；系统状态公开；其他 /api/** 必须携带 satoken 请求头。
- 注册成功即创建登录会话并返回令牌；用户写入与会话创建处于同一注册事务，避免半成功账号。登录有效期 7 天，退出时删除当前会话，登录态使用 Redis。
- 每次受保护请求检查用户是否存在及 ENABLED 状态，禁用账号不能继续使用旧令牌。
- 任务创建时从登录态写入 user_id，客户端不能指定归属。
- 列表、详情、删除、批量详情、图片及视频费用统计按当前用户隔离。
- 历史 user_id 为 NULL 的任务不向普通用户展示，不自动认领。
- 当前 /files/** 仍是原有公开静态资源地址，API 隔离不等于文件私密；不要把公开文件链接当成访问控制。

## 接口
POST /api/auth/register：{username,password}，直接返回 token、username，无需再次调用登录。
POST /api/auth/login：{username,password}，返回 token、username。
GET /api/auth/me：返回 id、username、memberLevel，不返回密码哈希。
POST /api/auth/logout：注销当前令牌。
统一业务响应 code=0 成功、401 需登录、403 账号禁用。

## 部署
当前数据库已完成用户鉴权结构升级。新环境按顺序执行 `backend/sql/ddl.sql`、`backend/sql/dml.sql` 初始化完整表结构和模型数据。
Redis 默认 r-uf67dp115l16ui42nypd.redis.cn-shanghai.rds.aliyuncs.com:6379，用户名 aivideo，密码 XXXX。通过 REDIS_PASSWORD 注入真实密码；可用 REDIS_HOST、REDIS_PORT、REDIS_USERNAME 覆盖。
确认 MySQL、Redis 网络与白名单可达。前后端建议同域反向代理，线上使用 HTTPS。

## 验收
注册两个用户，分别创建任务；互查任务 ID 应失败，列表与费用互不混入；退出或移除令牌后接口返回 401；禁用账号后旧令牌失效；重启后 Redis 会话仍有效。
