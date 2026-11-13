# 管理后台服务

`aivideo-admin` 是独立 Spring Boot 服务，默认端口 `18089`。管理员与前台用户使用不同数据表、不同 Sa-Token loginType 和不同请求头；管理端令牌通过 `admin-token` 请求头传递。

## 初始化

先在 `aivideo` 数据库执行 `backend/sql/ddl.sql`。当 `t_admin` 为空时，设置环境变量 `AIVIDEO_ADMIN_PASSWORD` 并启动服务，会创建用户名为 `admin` 的初始管理员。用户名可通过 `AIVIDEO_ADMIN_USERNAME` 覆盖。未提供密码或仍为 `XXXX` 时不会创建弱口令管理员。

```powershell
$env:MYSQL_PASSWORD = "你的 MySQL 密码"
$env:REDIS_PASSWORD = "你的 Redis 密码"
$env:AIVIDEO_ADMIN_PASSWORD = "请替换为强密码"
Set-Location backend
mvn -pl aivideo-admin -am spring-boot:run
```

数据库和 Redis 使用与前台相同的 `MYSQL_*`、`REDIS_*` 环境变量。管理服务默认端口可通过 `ADMIN_SERVER_PORT` 修改。

管理后台前端位于 `frontend/admin-frontend/`，开发端口为 `25174`，默认代理管理 API 到 `18089`、文件资源到前台服务 `18088`：

```powershell
cd frontend/admin-frontend
npm install
npm run dev
```

## 接口

- `POST /api/admin/auth/login`：管理员登录，参数 `username`、`password`。
- `GET /api/admin/auth/me`：当前管理员。
- `POST /api/admin/auth/logout`：退出。
- `GET /api/admin/users`：用户分页，支持 `keyword`、`status`。
- `GET /api/admin/users/overview`：用户总数、今日新增、禁用数。
- `PUT /api/admin/users/{id}/status`：启用或禁用，参数 `status`。
- `GET /api/admin/feed`：全站生成记录，支持 `userId`、`type`、`keyword`。

除登录接口外，所有接口都必须携带 `admin-token`。账号启用/禁用会在数据库事务中锁定用户行；禁用后的用户不能继续使用前台登录态调用受保护接口。

## 移除会员功能后的数据库清理

会员开通、续期、取消、流水查询和会员统计已移除，用户资料不再返回会员字段。

- 新建库：直接执行最新 `backend/sql/ddl.sql` 和 `backend/sql/dml.sql`。
- 已有库：停止旧版用户服务和管理服务，备份需要保留的会员历史，在目标业务库的同一数据库连接中完整执行 `backend/sql/20261113_remove_membership.sql`，然后启动新版服务，并部署两个前端。
- 脚本删除 `t_member_record` 表，以及 `t_user.member_level`、`t_user.member_expire_at` 两列，可重复执行。MySQL DDL 会自动提交，删除的会员历史需要从备份恢复。
- 用户、管理员、生成记录和个人厂商密钥保留。不要在新版数据库上启动仍依赖会员字段的旧版服务。
