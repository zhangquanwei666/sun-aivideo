-- 模型配置合表与初始化，适用 MySQL 5.7 / 8.x。
-- 在目标业务数据库的同一连接中执行。已有库先停止旧版服务及配置写入，备份两张模型配置表。
-- 使用遇到错误即停止的方式执行。DDL 会自动提交，回退旧版时需要恢复备份。
-- 支持旧分表、已合表以及尚未创建模型目录的数据库，可重复执行。

-- 1. 创建统一模型配置表。已有视频配置表会在下方原地升级，保留视频配置 ID。
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

-- 旧表已有记录均为视频，添加分类时以 VIDEO 填充这些记录。
SET @aivideo_has_model_type = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_model_config'
      AND COLUMN_NAME = 'model_type'
);

SET @aivideo_model_config_ddl = IF(@aivideo_has_model_type = 0,
    'ALTER TABLE t_model_config
        ADD COLUMN model_type VARCHAR(16) NOT NULL DEFAULT ''VIDEO'' COMMENT ''模型类型：VIDEO 视频 / IMAGE 图片'' AFTER id,
        MODIFY COLUMN ratios VARCHAR(128) NULL COMMENT ''支持的画幅比例，逗号分隔（为空时按模型默认行为处理）'',
        MODIFY COLUMN mode VARCHAR(16) NULL COMMENT ''视频生成方式，逗号分隔：T2V 文生视频 / I2V 图生视频'',
        MODIFY COLUMN protocol VARCHAR(32) NULL COMMENT ''视频请求协议（决定请求体格式）'',
        MODIFY COLUMN resolutions VARCHAR(64) NULL COMMENT ''支持的视频分辨率，逗号分隔，如 720P,1080P'',
        MODIFY COLUMN min_duration INT NULL COMMENT ''视频最短时长（秒）'',
        MODIFY COLUMN max_duration INT NULL COMMENT ''视频最长时长（秒）'',
        MODIFY COLUMN default_duration INT NULL COMMENT ''视频默认时长（秒）'',
        MODIFY COLUMN supports_audio TINYINT NULL COMMENT ''是否支持视频 audio 参数：1 是 / 0 否'',
        ADD COLUMN sizes VARCHAR(32) NULL COMMENT ''支持的图片清晰度，逗号分隔，如 1K,2K,4K'' AFTER supports_audio,
        ADD COLUMN max_reference_images INT NULL COMMENT ''图片模型支持的最多参考图张数，0 表示不支持参考图'' AFTER sizes,
        ADD COLUMN price_1k DECIMAL(10,4) NULL COMMENT ''1K 每张单价（按 currency），为空表示不支持'' AFTER max_reference_images,
        ADD COLUMN price_2k DECIMAL(10,4) NULL COMMENT ''2K 每张单价（按 currency），为空表示不支持'' AFTER price_1k,
        ADD COLUMN price_4k DECIMAL(10,4) NULL COMMENT ''4K 每张单价（按 currency），为空表示不支持'' AFTER price_2k,
        DROP INDEX uk_model_code,
        ADD UNIQUE KEY uk_model_type_code (model_type, model_code),
        COMMENT = ''图片与视频模型配置及单价''',
    'SELECT 1'
);
PREPARE aivideo_merge_models FROM @aivideo_model_config_ddl;
EXECUTE aivideo_merge_models;
DEALLOCATE PREPARE aivideo_merge_models;

-- 完成回填后移除临时默认值，后续新增配置必须显式指定类型。
SET @aivideo_model_config_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_model_config'
      AND COLUMN_NAME = 'model_type' AND COLUMN_DEFAULT IS NOT NULL
), 'ALTER TABLE t_model_config ALTER COLUMN model_type DROP DEFAULT', 'SELECT 1');
PREPARE aivideo_merge_models FROM @aivideo_model_config_ddl;
EXECUTE aivideo_merge_models;
DEALLOCATE PREPARE aivideo_merge_models;

-- 2. 导入旧图片模型，包括已停用的配置。保留单价、启停状态和时间戳，按旧 ID 顺序分配新 ID。
SET @aivideo_has_old_image_table = EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_image_model_config'
      AND TABLE_TYPE = 'BASE TABLE'
);

SET @aivideo_model_sql = IF(@aivideo_has_old_image_table,
    'INSERT INTO t_model_config (
    model_type, model_code, display_name, provider, sizes, ratios, max_reference_images, currency,
    price_1k, price_2k, price_4k, description, price_note, enabled, sort_order, created_at, updated_at
)
SELECT ''IMAGE'', old_image.model_code, old_image.display_name, old_image.provider,
       old_image.sizes, old_image.ratios, old_image.max_reference_images, old_image.currency,
       old_image.price_1k, old_image.price_2k, old_image.price_4k, old_image.description,
       old_image.price_note, old_image.enabled, old_image.sort_order, old_image.created_at, old_image.updated_at
FROM t_image_model_config AS old_image
LEFT JOIN t_model_config AS current_image
    ON current_image.model_type = ''IMAGE'' AND current_image.model_code = old_image.model_code
WHERE current_image.id IS NULL
ORDER BY old_image.id',
    'SELECT 1'
);
PREPARE aivideo_model_stmt FROM @aivideo_model_sql;
EXECUTE aivideo_model_stmt;
DEALLOCATE PREPARE aivideo_model_stmt;

-- 3. 补齐默认模型。迁入的配置和已有配置按 (model_type, model_code) 优先保留。
INSERT IGNORE INTO t_model_config
(model_type, model_code, display_name, provider, mode, protocol, resolutions, ratios, min_duration, max_duration, default_duration,
 currency, price_480p, price_720p, price_1080p, supports_audio, description, price_note, enabled, sort_order)
VALUES
-- 万相 2.7 文生视频：输入文字生成视频，支持 5 种画幅，720P ¥0.6/秒，1080P ¥1.0/秒
('VIDEO', 'wan2.7-t2v', '万相 2.7 文生视频', 'DASHSCOPE', 'T2V', 'WAN27_T2V', '720P,1080P', '16:9,9:16,1:1,4:3,3:4', 2, 15, 5,
 'CNY', NULL, 0.6000, 1.0000, 0, '文字直接生成视频，支持多种画幅', '官方原价', 1, 10),
-- 万相 2.7 图生视频：以首帧图片生成视频，画幅跟随图片（单价待核实）
('VIDEO', 'wan2.7-i2v', '万相 2.7 图生视频', 'DASHSCOPE', 'I2V', 'WAN27_I2V', '720P,1080P', NULL, 2, 15, 5,
 'CNY', NULL, 0.6000, 1.0000, 0, '以首帧图片生成视频，画幅跟随图片', '单价待核实，请以百炼控制台为准', 1, 20),
-- 万相 2.6 图生视频：以首帧图片生成视频，支持多镜头，720P ¥0.6/秒，1080P ¥1.0/秒
('VIDEO', 'wan2.6-i2v', '万相 2.6 图生视频', 'DASHSCOPE', 'I2V', 'WAN_LEGACY_I2V', '720P,1080P', NULL, 2, 15, 5,
 'CNY', NULL, 0.6000, 1.0000, 0, '以首帧图片生成视频，支持多镜头', '官方原价', 1, 30),
-- 万相 2.6 图生视频 Flash：生成更快，可选是否生成音频（单价待核实）
('VIDEO', 'wan2.6-i2v-flash', '万相 2.6 图生视频 Flash', 'DASHSCOPE', 'I2V', 'WAN_LEGACY_I2V', '720P,1080P', NULL, 2, 15, 5,
 'CNY', NULL, 0.6000, 1.0000, 1, '更快的图生视频，可选是否生成音频', '单价待核实，请以百炼控制台为准', 1, 40),
-- Gemini Omni Flash：Google 的视频模型（Veo 已在 Gemini API 下线），同时支持文生视频和首帧图生视频，
--   自带原生音频；3~15 秒，带首帧图片时最长 10 秒；720P $0.07/秒，1080P $0.104/秒（单价待核实）
('VIDEO', 'gemini-omni-1.2-flash', 'Gemini Omni Flash', 'GEMINI', 'T2V,I2V', 'GEMINI_OMNI', '720P,1080P', '16:9,9:16', 3, 15, 8,
 'USD', NULL, 0.0700, 0.1040, 0, '文字或首帧图片生成视频，自带原生音频', '美元计价，单价待核实，请以 Google 官方定价为准', 1, 50),
-- 万相 2.2 系列：低价模型，时长固定 5 秒、无声视频，支持 480P。单价待核实，请以百炼控制台为准
-- 万相 2.2 文生视频 Plus：旧版协议，按 size 传像素；480P 只有 16:9 / 9:16 / 1:1，所以画幅只开放这三种
('VIDEO', 'wan2.2-t2v-plus', '万相 2.2 文生视频 Plus', 'DASHSCOPE', 'T2V', 'WAN_LEGACY_T2V', '480P,1080P', '16:9,9:16,1:1', 5, 5, 5,
 'CNY', 0.1400, NULL, 0.7000, 0, '低价文生视频，固定 5 秒，无声', '单价待核实，请以百炼控制台为准', 1, 60),
-- 万相 2.2 图生视频 Plus：以首帧图片生成视频，画幅跟随图片
('VIDEO', 'wan2.2-i2v-plus', '万相 2.2 图生视频 Plus', 'DASHSCOPE', 'I2V', 'WAN_LEGACY_I2V', '480P,1080P', NULL, 5, 5, 5,
 'CNY', 0.1400, NULL, 0.7000, 0, '低价图生视频，固定 5 秒，无声', '单价待核实，请以百炼控制台为准', 1, 70),
-- 万相 2.2 图生视频 Flash：更快更便宜，支持 480P / 720P / 1080P
('VIDEO', 'wan2.2-i2v-flash', '万相 2.2 图生视频 Flash', 'DASHSCOPE', 'I2V', 'WAN_LEGACY_I2V', '480P,720P,1080P', NULL, 5, 5, 5,
 'CNY', 0.1000, 0.2000, 0.4800, 0, '最便宜的图生视频，固定 5 秒，无声', '单价待核实，请以百炼控制台为准', 1, 80);

INSERT IGNORE INTO t_model_config
(model_type, model_code, display_name, provider, sizes, ratios, max_reference_images, currency,
 price_1k, price_2k, price_4k, description, price_note, enabled, sort_order)
VALUES
-- 万相 2.7 图像：¥0.2/张，最高 2K，最多 9 张参考图
('IMAGE', 'wan2.7-image', '万相 2.7 图像', 'DASHSCOPE', '1K,2K', '1:1,16:9,9:16,4:3,3:4', 9, 'CNY',
 0.2000, 0.2000, NULL, '速度快、价格低，支持参考图编辑', '官方原价', 1, 10),
-- 万相 2.7 图像 Pro：¥0.5/张，纯文生图最高 4K（带参考图时最高 2K）
('IMAGE', 'wan2.7-image-pro', '万相 2.7 图像 Pro', 'DASHSCOPE', '1K,2K,4K', '1:1,16:9,9:16,4:3,3:4', 9, 'CNY',
 0.5000, 0.5000, 0.5000, '画质更高，纯文生图支持 4K', '官方原价', 1, 20),
-- Nano Banana 2（Gemini 3.1 Flash Image）：1K $0.067 / 2K $0.101 / 4K $0.151，最多 14 张参考图
('IMAGE', 'gemini-3.1-flash-image', 'Nano Banana 2', 'GEMINI', '1K,2K,4K', '1:1,16:9,9:16,4:3,3:4,3:2,2:3,21:9', 14, 'USD',
 0.0670, 0.1010, 0.1510, '速度快，角色一致性好，适合批量出图', '官方定价（美元），按汇率折算', 1, 30),
-- Nano Banana Pro（Gemini 3 Pro Image）：1K / 2K $0.134，4K $0.24，最多 14 张参考图
('IMAGE', 'gemini-3-pro-image', 'Nano Banana Pro', 'GEMINI', '1K,2K,4K', '1:1,16:9,9:16,4:3,3:4,3:2,2:3,21:9', 14, 'USD',
 0.1340, 0.1340, 0.2400, '质量最高，画面中的文字渲染好', '官方定价（美元），按汇率折算', 1, 40);

-- 4. 仅当每条旧图片配置均已保留且内容一致时，删除旧图片表。
-- 有冲突时返回 OLD_TABLE_RETAINED 和差异行数，保留旧表，便于人工核对。
SET @aivideo_image_differences = NULL;
SET @aivideo_model_sql = IF(@aivideo_has_old_image_table,
    'SELECT COUNT(*) INTO @aivideo_image_differences
FROM t_image_model_config AS old_image
LEFT JOIN t_model_config AS current_image
    ON current_image.model_type = ''IMAGE'' AND current_image.model_code = old_image.model_code
WHERE current_image.id IS NULL
   OR NOT (
       BINARY current_image.model_code <=> BINARY old_image.model_code
       AND BINARY current_image.display_name <=> BINARY old_image.display_name
       AND BINARY current_image.provider <=> BINARY old_image.provider
       AND BINARY current_image.sizes <=> BINARY old_image.sizes
       AND BINARY current_image.ratios <=> BINARY old_image.ratios
       AND current_image.max_reference_images <=> old_image.max_reference_images
       AND BINARY current_image.currency <=> BINARY old_image.currency
       AND current_image.price_1k <=> old_image.price_1k
       AND current_image.price_2k <=> old_image.price_2k
       AND current_image.price_4k <=> old_image.price_4k
       AND BINARY current_image.description <=> BINARY old_image.description
       AND BINARY current_image.price_note <=> BINARY old_image.price_note
       AND current_image.enabled <=> old_image.enabled
       AND current_image.sort_order <=> old_image.sort_order
       AND current_image.created_at <=> old_image.created_at
       AND current_image.updated_at <=> old_image.updated_at
   )',
    'SET @aivideo_image_differences = 0'
);
PREPARE aivideo_model_stmt FROM @aivideo_model_sql;
EXECUTE aivideo_model_stmt;
DEALLOCATE PREPARE aivideo_model_stmt;

SET @aivideo_model_sql = IF(
    @aivideo_has_old_image_table AND @aivideo_image_differences = 0,
    'DROP TABLE IF EXISTS t_image_model_config',
    'SELECT 1'
);
PREPARE aivideo_model_stmt FROM @aivideo_model_sql;
EXECUTE aivideo_model_stmt;
DEALLOCATE PREPARE aivideo_model_stmt;

SELECT CASE
           WHEN NOT @aivideo_has_old_image_table THEN 'NO_OLD_TABLE'
           WHEN @aivideo_image_differences = 0 THEN 'OLD_TABLE_DROPPED'
           ELSE 'OLD_TABLE_RETAINED'
       END AS migration_status,
       @aivideo_image_differences AS image_differences;

SET @aivideo_has_model_type = NULL;
SET @aivideo_model_config_ddl = NULL;
SET @aivideo_has_old_image_table = NULL;
SET @aivideo_model_sql = NULL;
SET @aivideo_image_differences = NULL;
