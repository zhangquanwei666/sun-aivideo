-- 移除会员功能（MySQL 5.7 / 8.x）：先选择业务数据库，在同一连接中顺序执行完整脚本。
-- 发布顺序：停止旧版用户服务和管理服务，备份需要保留的会员历史，执行本脚本，再启动新版服务。
-- 本脚本会永久删除会员流水、会员等级及到期时间；保留用户、管理员、生成任务和厂商密钥数据。
-- 已删除的表/列会跳过，允许重复执行；新建库只需执行 ddl.sql，无需本脚本。

DROP TABLE IF EXISTS t_member_record;

-- MySQL 各版本对 DROP COLUMN IF EXISTS 支持不同，因此只拼接实际存在的旧字段。
SELECT GROUP_CONCAT(CONCAT('DROP COLUMN `', COLUMN_NAME, '`') ORDER BY ORDINAL_POSITION SEPARATOR ', ')
INTO @aivideo_membership_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 't_user'
  AND COLUMN_NAME IN ('member_level', 'member_expire_at');

SET @aivideo_membership_ddl = IF(
    @aivideo_membership_columns IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `t_user` ', @aivideo_membership_columns)
);
PREPARE aivideo_remove_membership FROM @aivideo_membership_ddl;
EXECUTE aivideo_remove_membership;
DEALLOCATE PREPARE aivideo_remove_membership;

ALTER TABLE t_user COMMENT = '前台用户';

SET @aivideo_membership_columns = NULL;
SET @aivideo_membership_ddl = NULL;
