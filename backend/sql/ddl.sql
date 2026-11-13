-- ============================================================================
-- Sun AI Video · DDL（建表脚本）
--
-- 用途    ：创建项目所需的全部数据表。应用启动时不会自动建表，需要手动执行本脚本。
-- 数据库  ：MySQL 5.7+ / 8.x，字符集 utf8mb4
-- 执行顺序：1. 先执行本文件 ddl.sql（建表）
--          2. 再执行 dml.sql（插入初始数据）
-- 重复执行：使用 CREATE TABLE IF NOT EXISTS，表已存在时跳过，不会删除或修改已有数据
--
-- 包含的表：
--   t_model_config        视频模型配置与单价（前端可选的模型、参数范围、每秒单价，用于计算费用）
--   t_video_task          视频生成任务（每条视频的请求参数、生成结果、各阶段耗时与费用）
--   t_image_model_config  文生图模型配置与单价（按张计价，支持人民币 / 美元）
--   t_image_task          文生图任务（每张图片的请求参数、生成结果、耗时与费用）
--   t_user                前台用户（注册登录、会员等级与到期时间）
--   t_admin               管理后台账号（与前台用户分开）
--   t_member_record       会员变更记录（开通、续期、取消）
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. 视频模型配置与单价表 t_model_config
--    用途：定义可调用的视频模型（如通义万相 wan2.7-t2v）及其支持的分辨率、画幅、时长，
--          以及按分辨率区分的每秒单价。生成视频时按「单价 × 计费秒数」计算费用。
--    维护：新增模型或调整单价直接改这张表即可，无需改代码。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_model_config (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    model_code        VARCHAR(64)    NOT NULL COMMENT '模型编码，调用厂商接口时使用，如 wan2.7-t2v',
    display_name      VARCHAR(64)    NOT NULL COMMENT '展示名称，前端下拉框显示',
    provider          VARCHAR(32)    NOT NULL COMMENT '厂商：DASHSCOPE（阿里云百炼）/ GEMINI（Google Gemini）',
    mode              VARCHAR(16)    NOT NULL COMMENT '支持的生成方式，逗号分隔：T2V 文生视频 / I2V 图生视频，如 T2V,I2V 表示两种都支持',
    protocol          VARCHAR(32)    NOT NULL COMMENT '请求协议（决定请求体格式）：WAN27_T2V / WAN27_I2V / WAN_LEGACY_I2V / GEMINI_OMNI',
    resolutions       VARCHAR(64)    NOT NULL COMMENT '支持的分辨率，逗号分隔，如 720P,1080P',
    ratios            VARCHAR(64)    NULL     COMMENT '支持的画幅比例，逗号分隔（百炼图生视频为空，画幅跟随图片）',
    min_duration      INT            NOT NULL COMMENT '最短时长（秒）',
    max_duration      INT            NOT NULL COMMENT '最长时长（秒）',
    default_duration  INT            NOT NULL COMMENT '默认时长（秒）',
    currency          VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '单价币种：CNY 人民币 / USD 美元（统计时按 aivideo.cost.usd-to-cny 折算成人民币）',
    price_480p        DECIMAL(10, 4) NULL     COMMENT '480P 每秒单价（按 currency），为空表示不支持',
    price_720p        DECIMAL(10, 4) NULL     COMMENT '720P 每秒单价（按 currency），为空表示不支持',
    price_1080p       DECIMAL(10, 4) NULL     COMMENT '1080P 每秒单价（按 currency），为空表示不支持',
    supports_audio    TINYINT        NOT NULL DEFAULT 0 COMMENT '是否支持 audio 参数（生成音频）：1 是 / 0 否',
    description       VARCHAR(255)   NULL     COMMENT '模型说明，前端展示',
    price_note        VARCHAR(255)   NULL     COMMENT '价格说明，如「官方原价」「单价待核实」',
    enabled           TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 / 0 停用（停用后前端不展示）',
    sort_order        INT            NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_model_code (model_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '视频模型配置与单价';


-- ----------------------------------------------------------------------------
-- 2. 视频生成任务表 t_video_task
--    用途：每次点击「生成」产生一条记录，保存请求参数、厂商任务 ID、视频地址，
--          以及生成耗时（润色 / 排队 / 生成 / 总耗时）和费用（视频费用 + 润色费用）。
--          前端的生成记录、任务详情、用量与费用统计都来自这张表。
--    状态流转：SUBMITTING → PENDING → RUNNING → SUCCEEDED / FAILED / CANCELED / TIMEOUT
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_video_task (
    id                      BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键（任务 ID）',
    user_id                 BIGINT         NULL     COMMENT '提交任务的用户，关联 t_user.id（加用户功能之前的历史任务为空）',
    credential_version_id BIGINT NULL COMMENT '生成凭证版本',
    llm_credential_version_id BIGINT NULL COMMENT '润色凭证版本',
    enhance_warning VARCHAR(255) NULL COMMENT '润色降级提示',
    provider                VARCHAR(32)    NOT NULL COMMENT '实际调用的厂商：DASHSCOPE / GEMINI / MOCK（未配置 API Key 时为 MOCK）',
    model_code              VARCHAR(64)    NOT NULL COMMENT '模型编码，关联 t_model_config.model_code',
    mode                    VARCHAR(8)     NOT NULL COMMENT '生成方式：T2V 文生视频 / I2V 图生视频',
    status                  VARCHAR(16)    NOT NULL COMMENT '任务状态：SUBMITTING/PENDING/RUNNING/SUCCEEDED/FAILED/CANCELED/TIMEOUT',

    -- 请求参数
    prompt                  VARCHAR(5000)  NULL     COMMENT '用户输入的原始提示词',
    final_prompt            TEXT           NULL     COMMENT '实际提交给视频模型的提示词（开启润色时为润色后的内容）',
    negative_prompt         VARCHAR(500)   NULL     COMMENT '反向提示词（不希望出现的内容）',
    image_url               VARCHAR(1024)  NULL     COMMENT '图生视频的首帧图片地址（公网地址或 /files/ 本地路径）',
    resolution              VARCHAR(8)     NOT NULL COMMENT '分辨率：480P / 720P / 1080P',
    ratio                   VARCHAR(8)     NULL     COMMENT '画幅比例，如 16:9（百炼图生视频为空，画幅跟随图片）',
    duration                INT            NOT NULL COMMENT '请求时长（秒）',
    enhance_prompt          TINYINT        NOT NULL DEFAULT 0 COMMENT '是否用大模型润色提示词：1 是 / 0 否',
    prompt_extend           TINYINT        NOT NULL DEFAULT 1 COMMENT '是否开启百炼智能改写（prompt_extend）：1 是 / 0 否',
    watermark               TINYINT        NOT NULL DEFAULT 0 COMMENT '是否添加「AI生成」水印：1 是 / 0 否',
    audio                   TINYINT        NULL     COMMENT '是否生成音频（仅部分模型支持）：1 是 / 0 否',
    seed                    BIGINT         NULL     COMMENT '随机种子，为空时随机',

    -- 生成结果
    provider_task_id        VARCHAR(128)   NULL     COMMENT '厂商返回的任务 ID，用于轮询结果',
    remote_video_url        VARCHAR(2048)  NULL     COMMENT '厂商返回的视频地址（百炼只保留 24 小时）',
    local_video_path        VARCHAR(512)   NULL     COMMENT '视频保存到本地后的访问路径（/files/videos/...），Gemini 直接返回视频内容时也存这里',
    error_code              VARCHAR(64)    NULL     COMMENT '失败时的错误码',
    error_message           VARCHAR(1024)  NULL     COMMENT '失败时的错误信息',

    -- 费用（元）
    billed_duration         INT            NULL     COMMENT '计费时长（秒），以厂商返回为准',
    currency                VARCHAR(8)     NULL     COMMENT '单价币种：CNY / USD，取自 t_model_config.currency',
    unit_price              DECIMAL(10, 4) NULL     COMMENT '视频每秒单价（按 currency），取自 t_model_config 对应分辨率的价格',
    video_cost              DECIMAL(12, 4) NOT NULL DEFAULT 0 COMMENT '视频生成费用（元）= 单价折算人民币 × 计费时长（失败不计费）',
    llm_input_tokens        INT            NOT NULL DEFAULT 0 COMMENT '提示词润色的输入 tokens',
    llm_output_tokens       INT            NOT NULL DEFAULT 0 COMMENT '提示词润色的输出 tokens',
    llm_cost                DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '提示词润色费用（按 tokens 计算）',
    total_cost              DECIMAL(12, 4) NOT NULL DEFAULT 0 COMMENT '总费用 = 视频生成费用 + 提示词润色费用',

    -- 耗时（毫秒）
    enhance_ms              BIGINT         NULL     COMMENT '提示词润色耗时',
    queue_ms                BIGINT         NULL     COMMENT '厂商排队耗时 = 开始执行时间 - 提交时间',
    generate_ms             BIGINT         NULL     COMMENT '厂商生成耗时 = 完成时间 - 开始执行时间',
    total_elapsed_ms        BIGINT         NULL     COMMENT '总耗时 = 厂商完成时间 - 用户点击生成的时间',
    provider_submit_time    DATETIME(3)    NULL     COMMENT '厂商侧：任务提交时间',
    provider_scheduled_time DATETIME(3)    NULL     COMMENT '厂商侧：任务开始执行时间',
    provider_end_time       DATETIME(3)    NULL     COMMENT '厂商侧：任务完成时间',
    finished_at             DATETIME(3)    NULL     COMMENT '本系统记录的任务结束时间',

    -- 逻辑删除（前端删除只是隐藏，数据与本地视频文件都保留）
    deleted                 TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已删除：0 正常 / 1 已删除（前端不再展示，费用统计仍计入）',
    deleted_at              DATETIME(3)    NULL     COMMENT '删除时间',

    created_at              DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（用户点击生成的时间）',
    updated_at              DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    KEY idx_video_task_credential (credential_version_id),
    KEY idx_video_task_llm_credential (llm_credential_version_id),
    KEY idx_video_task_user (user_id) COMMENT '按用户查询生成记录与统计费用',
    KEY idx_video_task_status (status) COMMENT '轮询进行中的任务、按状态筛选',
    KEY idx_video_task_created (created_at) COMMENT '按创建时间排序与统计'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '视频生成任务（含耗时与费用统计）';


-- ----------------------------------------------------------------------------
-- 3. 文生图模型配置与单价表 t_image_model_config
--    用途：定义可调用的文生图模型（如阿里云百炼 万相 2.7 图像、Google Gemini Nano Banana）
--          及其支持的清晰度、画幅、参考图张数和每张单价。
--          单价按 currency 计价（CNY / USD），美元价格按配置的汇率折算成人民币后计入统计。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_image_model_config (
    id                    BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    model_code            VARCHAR(64)    NOT NULL COMMENT '模型编码，调用厂商接口时使用，如 wan2.7-image、gemini-3-pro-image',
    display_name          VARCHAR(64)    NOT NULL COMMENT '展示名称，前端下拉框显示',
    provider              VARCHAR(32)    NOT NULL COMMENT '厂商：DASHSCOPE（阿里云百炼）/ GEMINI（Google Gemini）',
    sizes                 VARCHAR(32)    NOT NULL COMMENT '支持的清晰度，逗号分隔，如 1K,2K,4K',
    ratios                VARCHAR(128)   NOT NULL COMMENT '支持的画幅比例，逗号分隔，如 1:1,16:9,9:16',
    max_reference_images  INT            NOT NULL DEFAULT 0 COMMENT '最多可上传的参考图张数，0 表示不支持参考图',
    currency              VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '单价的计价币种：CNY / USD',
    price_1k              DECIMAL(10, 4) NULL     COMMENT '1K 每张单价（按 currency），为空表示不支持',
    price_2k              DECIMAL(10, 4) NULL     COMMENT '2K 每张单价（按 currency），为空表示不支持',
    price_4k              DECIMAL(10, 4) NULL     COMMENT '4K 每张单价（按 currency），为空表示不支持',
    description           VARCHAR(255)   NULL     COMMENT '模型说明，前端展示',
    price_note            VARCHAR(255)   NULL     COMMENT '价格说明',
    enabled               TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 / 0 停用（停用后前端不展示）',
    sort_order            INT            NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_image_model_code (model_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文生图模型配置与单价';


-- ----------------------------------------------------------------------------
-- 4. 文生图任务表 t_image_task
--    用途：每次在「文生图」模式下点击生成产生一条记录，保存请求参数、生成的图片（本地路径），
--          以及耗时（润色 / 生成 / 总耗时）和费用（图片费用 + 润色费用，统一为人民币）。
--          前端生成记录与图片和视频合并展示，生成的图片可作为图生视频的首帧。
--    状态流转：SUBMITTING → RUNNING → SUCCEEDED / FAILED
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_image_task (
    id                      BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键（任务 ID）',
    user_id                 BIGINT         NULL     COMMENT '提交任务的用户，关联 t_user.id（加用户功能之前的历史任务为空）',
    credential_version_id BIGINT NULL COMMENT '生成凭证版本',
    llm_credential_version_id BIGINT NULL COMMENT '润色凭证版本',
    enhance_warning VARCHAR(255) NULL COMMENT '润色降级提示',
    provider                VARCHAR(32)    NOT NULL COMMENT '实际调用的厂商：DASHSCOPE / GEMINI / MOCK（未配置 API Key 时为 MOCK）',
    model_code              VARCHAR(64)    NOT NULL COMMENT '模型编码，关联 t_image_model_config.model_code',
    status                  VARCHAR(16)    NOT NULL COMMENT '任务状态：SUBMITTING/RUNNING/SUCCEEDED/FAILED',

    -- 请求参数
    prompt                  VARCHAR(5000)  NOT NULL COMMENT '用户输入的原始提示词',
    final_prompt            TEXT           NULL     COMMENT '实际提交给模型的提示词（开启润色时为润色后的内容）',
    enhance_prompt          TINYINT        NOT NULL DEFAULT 0 COMMENT '是否用大模型润色提示词：1 是 / 0 否',
    reference_images        VARCHAR(4000)  NULL     COMMENT '参考图，逗号分隔的本地路径（/files/...）',
    aspect_ratio            VARCHAR(8)     NOT NULL COMMENT '画幅比例，如 16:9',
    image_size              VARCHAR(4)     NOT NULL COMMENT '清晰度：1K / 2K / 4K',
    image_count             INT            NOT NULL DEFAULT 1 COMMENT '生成张数',

    -- 生成结果
    image_paths             VARCHAR(2048)  NULL     COMMENT '生成的图片，逗号分隔的本地访问路径（/files/generated/...）',
    error_code              VARCHAR(64)    NULL     COMMENT '失败时的错误码',
    error_message           VARCHAR(1024)  NULL     COMMENT '失败时的错误信息',

    -- 费用
    currency                VARCHAR(8)     NULL     COMMENT '单价的计价币种：CNY / USD',
    unit_price              DECIMAL(10, 4) NULL     COMMENT '每张单价（按 currency），取自 t_image_model_config 对应清晰度的价格',
    image_cost              DECIMAL(12, 4) NOT NULL DEFAULT 0 COMMENT '图片费用（人民币）= 单价折算人民币 × 张数（失败不计费）',
    llm_input_tokens        INT            NOT NULL DEFAULT 0 COMMENT '提示词润色的输入 tokens',
    llm_output_tokens       INT            NOT NULL DEFAULT 0 COMMENT '提示词润色的输出 tokens',
    llm_cost                DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '提示词润色费用（人民币）',
    total_cost              DECIMAL(12, 4) NOT NULL DEFAULT 0 COMMENT '总费用（人民币）= 图片费用 + 提示词润色费用',
    provider_input_tokens   INT            NULL     COMMENT '厂商返回的输入 tokens（仅供参考）',
    provider_output_tokens  INT            NULL     COMMENT '厂商返回的输出 tokens（仅供参考）',

    -- 耗时（毫秒）
    enhance_ms              BIGINT         NULL     COMMENT '提示词润色耗时',
    generate_ms             BIGINT         NULL     COMMENT '厂商生成耗时（同步调用的请求耗时）',
    total_elapsed_ms        BIGINT         NULL     COMMENT '总耗时 = 完成时间 - 用户点击生成的时间',
    finished_at             DATETIME(3)    NULL     COMMENT '任务结束时间',

    -- 逻辑删除（前端删除只是隐藏，数据与本地图片文件都保留）
    deleted                 TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已删除：0 正常 / 1 已删除（前端不再展示，费用统计仍计入）',
    deleted_at              DATETIME(3)    NULL     COMMENT '删除时间',

    created_at              DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（用户点击生成的时间）',
    updated_at              DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    KEY idx_image_task_credential (credential_version_id),
    KEY idx_image_task_llm_credential (llm_credential_version_id),
    KEY idx_image_task_user (user_id) COMMENT '按用户查询生成记录与统计费用',
    KEY idx_image_task_status (status) COMMENT '启动时清理中断的任务、按状态统计',
    KEY idx_image_task_created (created_at) COMMENT '生成记录按时间倒序分页'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文生图任务（含耗时与费用统计）';


-- ----------------------------------------------------------------------------
-- 5. 前台用户表 t_user
--    用途：前台（视频生成站）的用户账号。所有人都可以注册，注册后为普通用户；
--          启用状态的用户配置个人厂商密钥后即可生成，会员字段供独立会员功能使用。
--    会员是否有效：member_level = 'VIP'，且 member_expire_at 为空（永久）或晚于当前时间。
--    关联：t_video_task.user_id、t_image_task.user_id、t_member_record.user_id → t_user.id
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键（用户 ID）',
    username          VARCHAR(32)    NOT NULL COMMENT '登录名，4~32 位字母、数字或下划线，唯一',
    password_hash     VARCHAR(100)   NOT NULL COMMENT 'BCrypt 加密后的密码（不保存明文）',
    nickname          VARCHAR(32)    NULL     COMMENT '昵称，为空时前端显示登录名',
    avatar            VARCHAR(512)   NULL     COMMENT '头像地址',
    member_level      VARCHAR(16)    NOT NULL DEFAULT 'NORMAL' COMMENT '会员等级：NORMAL 普通用户 / VIP 会员',
    member_expire_at  DATETIME       NULL     COMMENT '会员到期时间；member_level 为 VIP 且此字段为空表示永久会员',
    status            VARCHAR(16)    NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED 正常 / DISABLED 已禁用（禁止登录）',
    register_ip       VARCHAR(64)    NULL     COMMENT '注册时的 IP',
    last_login_at     DATETIME       NULL     COMMENT '最近一次登录时间',
    last_login_ip     VARCHAR(64)    NULL     COMMENT '最近一次登录 IP',
    deleted           TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已删除：0 正常 / 1 已删除',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '前台用户（含会员信息）';


-- ----------------------------------------------------------------------------
-- 6. 管理员表 t_admin
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


-- ----------------------------------------------------------------------------
-- 7. 会员变更记录表 t_member_record
--    用途：管理员每次为用户开通、续期、取消会员都记录一条，便于追溯谁在什么时候做了什么。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_member_record (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id           BIGINT         NOT NULL COMMENT '用户 ID，关联 t_user.id',
    action            VARCHAR(16)    NOT NULL COMMENT '动作：OPEN 开通 / 续期，REVOKE 取消会员',
    member_level      VARCHAR(16)    NOT NULL COMMENT '变更后的会员等级：NORMAL / VIP',
    days              INT            NULL     COMMENT '本次开通 / 续期的天数；为空表示永久（取消时为空）',
    expire_before     DATETIME       NULL     COMMENT '变更前的会员到期时间',
    expire_after      DATETIME       NULL     COMMENT '变更后的会员到期时间；为空表示永久或已取消',
    operator_id       BIGINT         NOT NULL COMMENT '操作的管理员 ID，关联 t_admin.id',
    operator_name     VARCHAR(32)    NOT NULL COMMENT '操作的管理员登录名（冗余，便于查看）',
    remark            VARCHAR(255)   NULL     COMMENT '备注，如收款方式、订单号',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_member_record_user (user_id) COMMENT '查询某个用户的会员记录'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员变更记录';


-- ============================================================================
-- 表结构变更记录
--   新建的库不需要执行下面的语句（上面的建表语句已经包含）；
--   已经按旧版本建过表的库，按顺序取消注释后单独执行一次。
-- ============================================================================

-- [2026-11-11] t_video_task 新增逻辑删除字段 deleted、deleted_at（支持在前端删除视频）
-- ALTER TABLE t_video_task
--     ADD COLUMN deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已删除：0 正常 / 1 已删除（前端不再展示，费用统计仍计入）' AFTER finished_at,
--     ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '删除时间' AFTER deleted;

-- [2026-11-11] 新增文生图功能：新建 t_image_model_config、t_image_task 两张表
--   直接执行上面第 3、4 段的两条 CREATE TABLE 语句即可（已存在时会跳过），然后执行 dml.sql 插入文生图模型。

-- [2026-11-11] 接入 Gemini Omni 视频生成：视频模型支持美元计价，一个模型可同时支持文生视频和图生视频
--   执行完下面两条 ALTER 后，再执行 dml.sql 插入 Gemini Omni Flash 模型（已有的模型会跳过）。
-- ALTER TABLE t_model_config
--     MODIFY COLUMN mode VARCHAR(16) NOT NULL COMMENT '支持的生成方式，逗号分隔：T2V 文生视频 / I2V 图生视频，如 T2V,I2V 表示两种都支持',
--     ADD COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '单价币种：CNY 人民币 / USD 美元（统计时按 aivideo.cost.usd-to-cny 折算成人民币）' AFTER default_duration;
-- ALTER TABLE t_video_task
--     ADD COLUMN currency VARCHAR(8) NULL COMMENT '单价币种：CNY / USD，取自 t_model_config.currency' AFTER billed_duration;

-- [2026-11-12] 新增用户登录与会员：新建 t_user、t_admin、t_member_record 三张表，任务表增加 user_id
--   1) 直接执行上面第 5、6、7 段的三条 CREATE TABLE 语句（已存在时会跳过）；
--   2) 执行下面两条 ALTER，给视频、图片任务加上所属用户；
--   3) 历史任务的 user_id 为空，只在管理后台可见；如需归到某个用户，见 dml.sql 第 3 段。
-- ALTER TABLE t_video_task
--     ADD COLUMN user_id BIGINT NULL COMMENT '提交任务的用户，关联 t_user.id（加用户功能之前的历史任务为空）' AFTER id,
--     ADD KEY idx_video_task_user (user_id) COMMENT '按用户查询生成记录与统计费用';
-- ALTER TABLE t_image_task
--     ADD COLUMN user_id BIGINT NULL COMMENT '提交任务的用户，关联 t_user.id（加用户功能之前的历史任务为空）' AFTER id,
--     ADD KEY idx_image_task_user (user_id) COMMENT '按用户查询生成记录与统计费用';


-- 用户自维护厂商凭证；现有库升级请执行 20261113_user_channel_credentials.sql
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
