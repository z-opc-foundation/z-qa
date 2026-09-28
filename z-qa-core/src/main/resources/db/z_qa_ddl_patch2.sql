-- ========================================
-- z-qa DDL 补丁 2 — P0 地基 + P2 状态机
-- 日期: 2026-09-28
-- 适用: z_qa_ddl.sql + z_qa_ddl_patch.sql 之后执行
-- 数据库: oc (MySQL 8.0.36, sql_mode 无 ANSI_QUOTES)
-- 特点: 全部 ADD COLUMN IF-NOT-EXISTS / MODIFY / UPDATE，可重复执行
--
-- 修的问题:
--   1. z_qa_run.env_code            实体有映射、DDL 从未定义 → 计划/套件运行落库必抛 Unknown column
--   2. z_qa_e2e_action.e2e_screenshot 同上 → POST /e2e/actions/execute 插入失败
--   3. z_qa_run_detail.result 是 VARCHAR(16)，装不下 fail_system_error (18 字符)
--   4. 缺陷分类、套件级 failFast、跨实例可用的 abort 标志、计划运行的套件归属 都缺列
--   5. z_qa_run_detail.target_mode  实体有映射、DDL 从未定义 → 每一步落库必抛 Unknown column
-- ========================================

DROP PROCEDURE IF EXISTS `z_qa_add_column_if_not_exists`;

DELIMITER $$
CREATE PROCEDURE `z_qa_add_column_if_not_exists`(
    IN p_table VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*)
    INTO v_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;
    IF v_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---- 1. z_qa_run ----
CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'env_code', 'VARCHAR(64) NULL COMMENT "执行环境编码 (z_qa_env.code)"');
CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'abort_requested', 'TINYINT NOT NULL DEFAULT 0 COMMENT "中止请求标志;跑这条 run 的实例在步骤间轮询它"');

-- ---- 2. z_qa_suite: 遇错即停是套件级策略 ----
CALL `z_qa_add_column_if_not_exists`('z_qa_suite', 'fail_fast', 'TINYINT NOT NULL DEFAULT 0 COMMENT "遇错即停:某步失败后剩余步骤落 skip 并结束 run"');

-- ---- 3. z_qa_run_detail ----
CALL `z_qa_add_column_if_not_exists`('z_qa_run_detail', 'suite_id', 'BIGINT NULL COMMENT "归属套件 (计划 run 一次跨多套件)"');
CALL `z_qa_add_column_if_not_exists`('z_qa_run_detail', 'defect_type', 'VARCHAR(32) NULL COMMENT "缺陷分类: system_defect/script_defect/env_data_defect"');
CALL `z_qa_add_column_if_not_exists`('z_qa_run_detail', 'target_mode', 'VARCHAR(16) NULL COMMENT "目标: real=真实服务 mock=Mock端点 (执行时从步骤快照)"');

-- result 词表从 pending/pass/fail/error/skip 扩到十态，最长值 fail_system_error 需 18 字符。
-- MODIFY 天然幂等，重复执行无害。
ALTER TABLE `z_qa_run_detail`
    MODIFY COLUMN `result` VARCHAR(32) NOT NULL DEFAULT 'pending'
        COMMENT 'pending/running/success/skip/fail_assert/fail_system_error/fail_timeout/fail_navigation/fail_param_null/fail_unknown';

-- ---- 4. z_qa_e2e_action ----
CALL `z_qa_add_column_if_not_exists`('z_qa_e2e_action', 'e2e_screenshot', 'TINYINT NOT NULL DEFAULT 1 COMMENT "该操作是否截图"');

-- ---- 5. 索引 ----
DROP PROCEDURE IF EXISTS `z_qa_add_index_if_not_exists`;

DELIMITER $$
CREATE PROCEDURE `z_qa_add_index_if_not_exists`(
    IN p_table VARCHAR(128),
    IN p_index VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*)
    INTO v_count
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index;
    IF v_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL `z_qa_add_index_if_not_exists`('z_qa_run_detail', 'idx_run_result', '(`run_id`, `result`)');
CALL `z_qa_add_index_if_not_exists`('z_qa_run_detail', 'idx_suite_id', '(`suite_id`)');
CALL `z_qa_add_index_if_not_exists`('z_qa_run', 'idx_abort_requested', '(`abort_requested`, `status`)');

-- ---- 6. 存量数据迁移到新状态词表 ----
-- 旧 error 无法反推具体原因，落到 fail_unknown;
-- 旧 fail 绝大多数是断言不符，落到 fail_assert (含少量非 2xx，宁可低估为断言问题也不臆造分类)。
UPDATE `z_qa_run_detail` SET `result` = 'success' WHERE `result` = 'pass';
UPDATE `z_qa_run_detail` SET `result` = 'fail_assert' WHERE `result` = 'fail';
UPDATE `z_qa_run_detail` SET `result` = 'fail_unknown' WHERE `result` = 'error';

-- 按新词表回填 defect_type，让历史报告立刻可按缺陷类型聚合
UPDATE `z_qa_run_detail`
SET `defect_type` = 'system_defect'
WHERE `result` IN ('fail_assert', 'fail_system_error');
UPDATE `z_qa_run_detail`
SET `defect_type` = 'env_data_defect'
WHERE `result` = 'fail_param_null';
UPDATE `z_qa_run_detail`
SET `defect_type` = 'script_defect'
WHERE `result` IN ('fail_timeout', 'fail_navigation', 'fail_unknown');

-- 回填历史明细的套件归属 (当时只有单套件在跑)
UPDATE `z_qa_run_detail` d
    JOIN `z_qa_run` r ON r.`id` = d.`run_id`
SET d.`suite_id` = r.`suite_id`
WHERE d.`suite_id` IS NULL
  AND r.`suite_id` IS NOT NULL;

-- ---- 7. 收掉重复计数器 ----
-- step_total/step_pass/step_fail/step_skip 与 total_steps/passed_steps/failed_steps 是同一份数据，
-- 改造前两条执行链各写一套、互相覆盖。现在只有一套循环写 *_steps，这四列不再被任何代码映射。
DROP PROCEDURE IF EXISTS `z_qa_drop_column_if_exists`;

DELIMITER $$
CREATE PROCEDURE `z_qa_drop_column_if_exists`(
    IN p_table VARCHAR(128),
    IN p_column VARCHAR(128)
)
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*)
    INTO v_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;
    IF v_count > 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL `z_qa_drop_column_if_exists`('z_qa_run', 'step_total');
CALL `z_qa_drop_column_if_exists`('z_qa_run', 'step_pass');
CALL `z_qa_drop_column_if_exists`('z_qa_run', 'step_fail');
CALL `z_qa_drop_column_if_exists`('z_qa_run', 'step_skip');

DROP PROCEDURE `z_qa_add_column_if_not_exists`;
DROP PROCEDURE `z_qa_add_index_if_not_exists`;
DROP PROCEDURE `z_qa_drop_column_if_exists`;
