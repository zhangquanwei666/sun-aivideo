-- 现有数据库升级：先停止新任务、等待旧任务及下载结束，再执行本脚本。
-- ALTER TABLE 部分仅执行一次。新建库执行 ddl.sql 即可，不再执行本脚本。
CREATE TABLE IF NOT EXISTS t_user_channel_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel_code VARCHAR(32) NOT NULL COMMENT 'DASHSCOPE / GEMINI',
    enabled TINYINT NOT NULL DEFAULT 1,
    current_version_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel (user_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户厂商凭证配置，不保存明文';

CREATE TABLE IF NOT EXISTS t_user_channel_credential_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credential_id BIGINT NOT NULL,
    ciphertext TEXT NULL COMMENT 'AES-GCM nonce+密文+认证标签；清理后为空',
    encryption_key_id VARCHAR(64) NOT NULL,
    key_suffix VARCHAR(4) NOT NULL COMMENT '仅用于脱敏展示',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_credential_version (credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变凭证版本，供后台任务绑定';

ALTER TABLE t_video_task
    ADD COLUMN credential_version_id BIGINT NULL COMMENT '生成凭证版本，历史记录为空',
    ADD COLUMN llm_credential_version_id BIGINT NULL COMMENT '千问润色凭证版本',
    ADD COLUMN enhance_warning VARCHAR(255) NULL COMMENT '润色降级提示',
    ADD KEY idx_video_credential (credential_version_id),
    ADD KEY idx_video_llm_credential (llm_credential_version_id);
ALTER TABLE t_image_task
    ADD COLUMN credential_version_id BIGINT NULL COMMENT '生成凭证版本，历史记录为空',
    ADD COLUMN llm_credential_version_id BIGINT NULL COMMENT '千问润色凭证版本',
    ADD COLUMN enhance_warning VARCHAR(255) NULL COMMENT '润色降级提示',
    ADD KEY idx_image_credential (credential_version_id),
    ADD KEY idx_image_llm_credential (llm_credential_version_id);
