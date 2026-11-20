-- ============================================================================
-- Sun AI Video · DDL（建表脚本）
--
-- 用途    ：创建项目所需的全部数据表。应用启动时不会自动建表，需要手动执行本脚本。
-- 数据库  ：MySQL 8.0+，字符集 utf8mb4
-- 执行顺序：1. 先执行本文件 ddl.sql（建表）
--          2. 再执行 dml.sql（插入初始数据）
-- 重复执行：使用 CREATE TABLE IF NOT EXISTS，表已存在时跳过，不会删除或修改已有数据
-- 已有模型分表的库：执行 20261120_merge_model_config.sql 合表、补齐默认模型并清理旧图片表
--
-- 包含的表：
--   t_model_config        视频和图片模型配置（模型目录、生成参数与单价）
--   t_generation_task     图片与视频生成任务（请求参数、生成结果、耗时与费用）
--   t_user                前台用户（注册登录、账号状态）
--   t_admin               管理后台账号（与前台用户分开）
--   t_user_channel_credential 用户厂商密钥及版本
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. 图片与视频模型配置表 t_model_config
--    按 model_type 区分模型，模型编码在各类型内唯一。
--    视频按分辨率每秒计价，图片按清晰度每张计价。各自的专属字段可为空。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_model_config (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    model_type        VARCHAR(16)    NOT NULL COMMENT '模型类型：VIDEO 视频 / IMAGE 图片',
    model_code        VARCHAR(64)    NOT NULL COMMENT '模型编码，调用厂商接口时使用，如 wan2.7-t2v',
    display_name      VARCHAR(64)    NOT NULL COMMENT '展示名称，前端下拉框显示',
    provider          VARCHAR(32)    NOT NULL COMMENT '厂商：DASHSCOPE（阿里云百炼）/ GEMINI（Google Gemini）',
    ratios            VARCHAR(128)   NULL     COMMENT '支持的画幅比例，逗号分隔（为空时按模型默认行为处理）',
    currency          VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '单价币种：CNY 人民币 / USD 美元（统计时按 aivideo.cost.usd-to-cny 折算成人民币）',
    mode              VARCHAR(16)    NULL     COMMENT '视频生成方式，逗号分隔：T2V 文生视频 / I2V 图生视频',
    protocol          VARCHAR(32)    NULL     COMMENT '视频请求协议（决定请求体格式）',
    resolutions       VARCHAR(64)    NULL     COMMENT '支持的视频分辨率，逗号分隔，如 720P,1080P',
    min_duration      INT            NULL     COMMENT '视频最短时长（秒）',
    max_duration      INT            NULL     COMMENT '视频最长时长（秒）',
    default_duration  INT            NULL     COMMENT '视频默认时长（秒）',
    price_480p        DECIMAL(10, 4) NULL     COMMENT '480P 每秒单价（按 currency），为空表示不支持',
    price_720p        DECIMAL(10, 4) NULL     COMMENT '720P 每秒单价（按 currency），为空表示不支持',
    price_1080p       DECIMAL(10, 4) NULL     COMMENT '1080P 每秒单价（按 currency），为空表示不支持',
    supports_audio    TINYINT        NULL     COMMENT '是否支持视频 audio 参数：1 是 / 0 否',
    sizes                 VARCHAR(32)    NULL COMMENT '支持的图片清晰度，逗号分隔，如 1K,2K,4K',
    max_reference_images  INT            NULL COMMENT '图片模型支持的最多参考图张数，0 表示不支持参考图',
    price_1k              DECIMAL(10, 4) NULL COMMENT '1K 每张单价（按 currency），为空表示不支持',
    price_2k              DECIMAL(10, 4) NULL COMMENT '2K 每张单价（按 currency），为空表示不支持',
    price_4k              DECIMAL(10, 4) NULL COMMENT '4K 每张单价（按 currency），为空表示不支持',
    description       VARCHAR(255)   NULL     COMMENT '模型说明，前端展示',
    price_note        VARCHAR(255)   NULL     COMMENT '价格说明，如「官方原价」「单价待核实」',
    enabled           TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 / 0 停用（停用后前端不展示）',
    sort_order        INT            NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_model_type_code (model_type, model_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图片与视频模型配置及单价';


-- ----------------------------------------------------------------------------
-- 2. 图片与视频生成任务表
--    联合主键 (task_type, id) 保留历史 ID。旧分表执行 20261120_merge_generation_tasks.sql。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_generation_task (
    task_type                 VARCHAR(8) NOT NULL COMMENT 'VIDEO 视频 / IMAGE 图片',
    id                        BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务 ID，与 task_type 组成主键',
    user_id                   BIGINT NULL COMMENT '任务所属用户，历史记录可为空',
    credential_version_id     BIGINT NULL COMMENT '生成凭证版本',
    llm_credential_version_id BIGINT NULL COMMENT '润色凭证版本',
    provider                  VARCHAR(32) NOT NULL COMMENT '实际调用的厂商：DASHSCOPE / GEMINI / MOCK',
    model_code                VARCHAR(64) NOT NULL COMMENT '模型编码，按 task_type 对应 t_model_config.model_type',
    status                    VARCHAR(16)    NOT NULL COMMENT '任务状态：SUBMITTING/PENDING/RUNNING/SUCCEEDED/FAILED/CANCELED/TIMEOUT',

    -- 共享参数、结果与费用
    prompt                    VARCHAR(5000)  NULL     COMMENT '用户输入的原始提示词',
    final_prompt              TEXT NULL COMMENT '实际提交给模型的提示词',
    enhance_prompt            TINYINT        NOT NULL DEFAULT 0 COMMENT '是否用大模型润色提示词：1 是 / 0 否',
    enhance_warning           VARCHAR(255) NULL COMMENT '润色降级提示',
    error_code                VARCHAR(64)    NULL     COMMENT '失败时的错误码',
    error_message             VARCHAR(1024)  NULL     COMMENT '失败时的错误信息',
    currency                  VARCHAR(8)     NULL     COMMENT '单价币种：CNY / USD，取自 t_model_config.currency',
    unit_price                DECIMAL(10, 4) NULL COMMENT '按 currency 计价，视频每秒，图片每张',
    llm_input_tokens          INT            NOT NULL DEFAULT 0 COMMENT '提示词润色的输入 tokens',
    llm_output_tokens         INT            NOT NULL DEFAULT 0 COMMENT '提示词润色的输出 tokens',
    llm_cost                  DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '提示词润色费用（按 tokens 计算）',
    total_cost                DECIMAL(12, 4) NOT NULL DEFAULT 0 COMMENT '生成费用与润色费用之和（人民币）',
    enhance_ms                BIGINT         NULL     COMMENT '提示词润色耗时',
    generate_ms               BIGINT         NULL     COMMENT '厂商生成耗时 = 完成时间 - 开始执行时间',
    total_elapsed_ms          BIGINT         NULL     COMMENT '总耗时 = 厂商完成时间 - 用户点击生成的时间',
    finished_at               DATETIME(3)    NULL     COMMENT '本系统记录的任务结束时间',
    deleted                   TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已删除：0 正常 / 1 已删除（前端不再展示，费用统计仍计入）',
    deleted_at                DATETIME(3)    NULL     COMMENT '删除时间',
    created_at                DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（用户点击生成的时间）',
    updated_at                DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    -- 视频专属字段
    mode                      VARCHAR(8)     NULL COMMENT '生成方式：T2V 文生视频 / I2V 图生视频',
    negative_prompt           VARCHAR(500)   NULL     COMMENT '反向提示词（不希望出现的内容）',
    image_url                 VARCHAR(1024)  NULL     COMMENT '图生视频的首帧图片地址（公网地址或 /files/ 本地路径）',
    resolution                VARCHAR(8)     NULL COMMENT '分辨率：480P / 720P / 1080P',
    ratio                     VARCHAR(8)     NULL     COMMENT '画幅比例，如 16:9（百炼图生视频为空，画幅跟随图片）',
    duration                  INT            NULL COMMENT '请求时长（秒）',
    prompt_extend             TINYINT        NULL COMMENT '是否开启百炼智能改写（prompt_extend）：1 是 / 0 否',
    watermark                 TINYINT        NULL COMMENT '是否添加「AI生成」水印：1 是 / 0 否',
    audio                     TINYINT        NULL     COMMENT '是否生成音频（仅部分模型支持）：1 是 / 0 否',
    seed                      BIGINT         NULL     COMMENT '随机种子，为空时随机',
    provider_task_id          VARCHAR(128)   NULL     COMMENT '厂商返回的任务 ID，用于轮询结果',
    remote_video_url          VARCHAR(2048)  NULL     COMMENT '厂商返回的视频地址（百炼只保留 24 小时）',
    local_video_path          VARCHAR(512)   NULL     COMMENT '视频保存到本地后的访问路径（/files/videos/...），Gemini 直接返回视频内容时也存这里',
    billed_duration           INT            NULL     COMMENT '计费时长（秒），以厂商返回为准',
    video_cost                DECIMAL(12, 4) NULL COMMENT '视频生成费用（元）= 单价折算人民币 × 计费时长（失败不计费）',
    queue_ms                  BIGINT         NULL     COMMENT '厂商排队耗时 = 开始执行时间 - 提交时间',
    provider_submit_time      DATETIME(3)    NULL     COMMENT '厂商侧：任务提交时间',
    provider_scheduled_time   DATETIME(3)    NULL     COMMENT '厂商侧：任务开始执行时间',
    provider_end_time         DATETIME(3)    NULL     COMMENT '厂商侧：任务完成时间',

    -- 图片专属字段
    reference_images          TEXT NULL COMMENT '参考图，逗号分隔的本地路径',
    aspect_ratio              VARCHAR(8)     NULL COMMENT '画幅比例，如 16:9',
    image_size                VARCHAR(4)     NULL COMMENT '清晰度：1K / 2K / 4K',
    image_count               INT            NULL COMMENT '生成张数',
    image_paths               VARCHAR(2048)  NULL     COMMENT '生成的图片，逗号分隔的本地访问路径（/files/generated/...）',
    image_cost                DECIMAL(12, 4) NULL COMMENT '图片费用（人民币）= 单价折算人民币 × 张数（失败不计费）',
    provider_input_tokens     INT            NULL     COMMENT '厂商返回的输入 tokens（仅供参考）',
    provider_output_tokens    INT            NULL     COMMENT '厂商返回的输出 tokens（仅供参考）',

    PRIMARY KEY (task_type, id),
    KEY idx_generation_task_id (id),
    KEY idx_generation_task_type_status (task_type, status, created_at),
    KEY idx_generation_task_user_type (user_id, task_type),
    KEY idx_generation_task_feed (deleted, created_at, id),
    KEY idx_generation_task_user_feed (user_id, deleted, created_at, id),
    KEY idx_generation_task_credential (credential_version_id, status),
    KEY idx_generation_task_llm_credential (llm_credential_version_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = DYNAMIC COMMENT = '图片与视频生成任务';


-- ----------------------------------------------------------------------------
-- 3. 前台用户表 t_user
--    用途：前台（视频生成站）的用户账号。所有人都可以注册；
--          启用状态的用户配置个人厂商密钥后即可生成。
--    关联：t_generation_task.user_id、t_user_channel_credential.user_id → t_user.id
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键（用户 ID）',
    username          VARCHAR(32)    NOT NULL COMMENT '登录名，4~32 位字母、数字或下划线，唯一',
    password_hash     VARCHAR(100)   NOT NULL COMMENT 'BCrypt 加密后的密码（不保存明文）',
    nickname          VARCHAR(32)    NULL     COMMENT '昵称，为空时前端显示登录名',
    avatar            VARCHAR(512)   NULL     COMMENT '头像地址',
    status            VARCHAR(16)    NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED 正常 / DISABLED 已禁用（禁止登录）',
    register_ip       VARCHAR(64)    NULL     COMMENT '注册时的 IP',
    last_login_at     DATETIME       NULL     COMMENT '最近一次登录时间',
    last_login_ip     VARCHAR(64)    NULL     COMMENT '最近一次登录 IP',
    deleted           TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已删除：0 正常 / 1 已删除',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '前台用户';


-- ----------------------------------------------------------------------------
-- 4. 管理员表 t_admin
--    用途：管理后台（aivideo-admin）的账号，与前台用户完全分开，不能登录前台。
--    初始化：表为空时，管理后台启动会读取环境变量 AIVIDEO_ADMIN_PASSWORD 自动创建 admin 账号，
--            密码不写在 SQL 里（仓库是公开的）。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_admin (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键（管理员 ID）',
    username          VARCHAR(32)    NOT NULL COMMENT '登录名，唯一',
    password_hash     VARCHAR(100)   NOT NULL COMMENT 'BCrypt 加密后的密码（不保存明文）',
    nickname          VARCHAR(32)    NULL     COMMENT '昵称',
    status            VARCHAR(16)    NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED 正常 / DISABLED 已禁用',
    last_login_at     DATETIME       NULL     COMMENT '最近一次登录时间',
    last_login_ip     VARCHAR(64)    NULL     COMMENT '最近一次登录 IP',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_admin_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理后台账号';

-- 每行是一个密钥版本。仅当前行使用 current_slot = 1，历史行使用 NULL。
-- 已合并但仍使用旧表名的环境执行 20261120_rename_channel_credential.sql。
CREATE TABLE IF NOT EXISTS t_user_channel_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel_code VARCHAR(32) NOT NULL COMMENT 'DASHSCOPE / GEMINI',
    current_slot TINYINT NULL COMMENT '1 当前版本，NULL 历史版本',
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '当前版本是否允许新任务使用',
    ciphertext TEXT NULL COMMENT 'AES-GCM nonce+密文+认证标签；清理后为空',
    encryption_key_id VARCHAR(64) NOT NULL,
    key_suffix VARCHAR(4) NOT NULL COMMENT '仅用于脱敏展示',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel_current (user_id, channel_code, current_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户厂商密钥版本，保留任务绑定';
