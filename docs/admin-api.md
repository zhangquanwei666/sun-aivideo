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
- `GET /api/admin/users`：用户分页，支持 `keyword`、`member`、`status`。
- `GET /api/admin/users/overview`：用户总数、有效会员、今日新增、禁用数。
- `PUT /api/admin/users/{id}/status`：启用或禁用，参数 `status`。
- `POST /api/admin/users/{id}/membership`：开通或续期，`days` 为空表示永久会员。
- `POST /api/admin/users/{id}/membership/revoke`：取消会员。
- `GET /api/admin/member-records`：会员变更流水。
- `GET /api/admin/feed`：全站生成记录，支持 `userId`、`type`、`keyword`。

除登录接口外，所有接口都必须携带 `admin-token`。会员变更会在数据库事务中锁定用户行，并记录操作管理员、变更前后到期时间和备注。
