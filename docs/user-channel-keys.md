# 用户厂商密钥：使用与部署

## 用户使用

登录用户前台后，打开“模型密钥”，分别保存自己的百炼和 Gemini API Key。每个用户每个厂商一份当前配置，万相图片、万相视频和千问共用用户百炼 Key。

保存只表示已配置，不会触发付费校验；实际调用仍可能因权限、模型开通、额度或网络失败。前端只显示末四位，密钥不能回显。用户仅配置 Gemini 时可以正常生成 Gemini 图片/视频，但不能启用千问润色。

更新 Key 后新任务使用新版本，已有任务继续使用原版本。停用只禁止新任务；存在未完成任务时拒绝删除。厂商侧撤销旧 Key 仍可能使已有任务失败。平台不会自动换用其他用户或平台 Key，也不会自动重复提交付费任务。

图片与视频的费用是参考单价估算，以用户的厂商账单为准。

## 数据库升级

- 新环境：执行 `backend/sql/ddl.sql`，再执行 `backend/sql/dml.sql`。
- 已有环境：停止接收新任务，等待旧任务及下载完成，再执行一次 `backend/sql/20261113_user_channel_credentials.sql`。其中 ALTER TABLE 不可重复执行。
- 升级同时覆盖用户服务与管理服务的代码，因为两个服务共享任务实体和数据库。
- 不自动迁移全局平台 Key 到任何用户；历史记录和费用保留，旧任务的凭证版本为空。未排空的旧任务不会使用用户新 Key 冒充原账户恢复。

配置表：`t_user_channel_credential`，唯一键 `(user_id, channel_code)`。密钥版本表：`t_user_channel_credential_version`。任务分别保存生成与润色的凭证版本 ID，不保存明文或密文。

更新或删除配置时清理不再被未完成任务引用的旧密文，保留版本元数据。后续每日清理也会移除已完成任务不再需要的旧密文。当前仍启用/可恢复使用的版本保留密文。

## 加密主密钥

用户服务必须配置 `AIVIDEO_CREDENTIAL_MASTER_KEY`，值为 Base64 编码的 32 字节随机密钥。示例（PowerShell，生成后请在密钥管理系统中安全保存）：

```powershell
$credentialMasterBytes = [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
$env:AIVIDEO_CREDENTIAL_MASTER_KEY = [Convert]::ToBase64String($credentialMasterBytes)
$env:AIVIDEO_CREDENTIAL_KEY_ID = "v1"
```

不要每次启动重新生成：所有用户服务实例必须使用相同主密钥，数据库备份恢复时也必须具备对应主密钥。不得写入仓库、前端环境变量、数据库或日志。管理服务不解密凭证，不需要此环境变量。

采用 AES-256-GCM，随机 nonce，每次加密关联用户和渠道，避免密文被跨用户替换。`AIVIDEO_CREDENTIAL_KEY_ID` 用于识别主密钥版本；当前实现一次加载一把主密钥，不支持直接替换主密钥后自动解密旧数据。主密钥轮换需专门执行重加密迁移，不能只修改环境变量。

`DASHSCOPE_API_KEY`、`GEMINI_API_KEY` 不再作为业务调用的凭证来源。平台继续维护接口地址、代理、超时、模型目录、单价和汇率。

开发需要模拟生成时显式设置 `AIVIDEO_MOCK_ENABLED=true`。默认关闭，生产环境禁止开启；不再使用 `aivideo.mock.when-no-key`。即使开启开发模拟，用户服务仍需要加密主密钥和 MySQL/Redis。

用户并发调用上限通过 `aivideo.credentials.max-concurrent-per-user` 配置，默认 4；按用户服务实例生效，现有全局线程池仍提供总并发和有界队列限制。多实例的全局配额需要后续分布式限流。

## 接口

以下接口全部要求用户 `satoken`，用户 ID 只取后端登录态，不接受客户端指定归属。

| 方法 | 地址 | 入参/出参 |
|---|---|---|
| GET | `/api/channel-credentials` | `channels`，含渠道、配置/启用状态、脱敏尾号、更新时间 |
| PUT | `/api/channel-credentials/{channel}` | `{ "apiKey": "完整密钥" }`，保存新版本并启用 |
| PUT | `/api/channel-credentials/{channel}/enabled` | `{ "enabled": true/false }` |
| DELETE | `/api/channel-credentials/{channel}` | 未完成任务仍在使用时拒绝删除 |
| GET | `/api/channel-credentials/status` | 当前用户润色可用状态和显式 Mock 状态 |

渠道仅支持 `DASHSCOPE`、`GEMINI`。没有原文读取接口，也不提供隐式付费验证。

模型接口的 `supported` 表示平台已接入，`available` 表示当前用户已配置并启用，不能视为权限/余额验证通过；兼容字段 `mockFallback` 恒为 false。公开 `/api/system/status` 不返回个人 Key 状态，个人能力需从受保护接口获取。

## 调用与恢复

创建任务和选择凭证版本在数据库事务中完成，并锁定用户行，与更新、删除操作串行。事务提交后才派发后台工作；队列满载时以独立事务将任务标记为失败，不调用厂商。

后台工作只根据任务的 `user_id`、渠道和凭证版本恢复密钥，不依赖 HTTP 登录线程。视频查询、Gemini 鉴权下载继续使用同一凭证。携带用户密钥的 Gemini 下载只允许配置中的 API 同源地址，且禁止自动重定向。

超过配置时长仍停留在 SUBMITTING 的视频会标记提交中断或超时，提示先检查厂商任务与账单，不自动重复提交。图片中断处理沿用启动时标记失败的行为；当前图片启动清理仍按单实例部署设计，多实例任务租约不属于本次改造。

本次不改变 `/files/**` 公开静态资源的现有行为，用户凭证隔离与作品文件私密是不同功能。
