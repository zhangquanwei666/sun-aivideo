-- 视频模型与单价（元/秒，华北2 北京地域官方原价，不含限时优惠）
-- 已存在的记录不会被覆盖，方便在库里直接修改单价
INSERT IGNORE INTO t_model_config
(model_code, display_name, provider, mode, protocol, resolutions, ratios, min_duration, max_duration, default_duration,
 price_480p, price_720p, price_1080p, supports_audio, description, price_note, enabled, sort_order)
VALUES
('wan2.7-t2v', '万相 2.7 文生视频', 'DASHSCOPE', 'T2V', 'WAN27_T2V', '720P,1080P', '16:9,9:16,1:1,4:3,3:4', 2, 15, 5,
 NULL, 0.6000, 1.0000, 0, '文字直接生成视频，支持多种画幅', '官方原价', 1, 10),
('wan2.7-i2v', '万相 2.7 图生视频', 'DASHSCOPE', 'I2V', 'WAN27_I2V', '720P,1080P', NULL, 2, 15, 5,
 NULL, 0.6000, 1.0000, 0, '以首帧图片生成视频，画幅跟随图片', '单价待核实，请以百炼控制台为准', 1, 20),
('wan2.6-i2v', '万相 2.6 图生视频', 'DASHSCOPE', 'I2V', 'WAN_LEGACY_I2V', '720P,1080P', NULL, 2, 15, 5,
 NULL, 0.6000, 1.0000, 0, '以首帧图片生成视频，支持多镜头', '官方原价', 1, 30),
('wan2.6-i2v-flash', '万相 2.6 图生视频 Flash', 'DASHSCOPE', 'I2V', 'WAN_LEGACY_I2V', '720P,1080P', NULL, 2, 15, 5,
 NULL, 0.6000, 1.0000, 1, '更快的图生视频，可选是否生成音频', '单价待核实，请以百炼控制台为准', 1, 40);
