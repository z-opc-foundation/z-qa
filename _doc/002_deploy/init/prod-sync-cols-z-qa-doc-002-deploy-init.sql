-- =====================================================================
-- 结构对齐回写 (缺列) — 生成日 2026-10-24
-- 来源: 生产 oc 只读 mysqldump --no-data
-- 口径: 只加不改; 只收「两种独立解析实现都判缺」的列, 歧义区不动。
-- 用法: mysql <库名> < 本文件 (脚本用 DELIMITER, 走 mysql 客户端执行)。
-- 位置: 与本表 CREATE 语句同目录 (z-qa/_doc/002_deploy/init)
-- =====================================================================

-- 逐列补齐: 只在列不存在时才 ALTER ⇒ 可重复执行。
-- MySQL **不支持** ALTER TABLE ... ADD COLUMN IF NOT EXISTS (那是 MariaDB 语法),
-- 所以幂等靠 information_schema 判定 + 动态 SQL, 不靠语法糖。
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-qa-doc-002-deploy-init`;
DELIMITER $$
CREATE PROCEDURE `zopc_sync_prod_cols_z-qa-doc-002-deploy-init`()
BEGIN
  DECLARE v_t VARCHAR(64);
  DECLARE v_c VARCHAR(64);
  DECLARE v_d TEXT;
  DECLARE cur CURSOR FOR SELECT tbl, col, defn FROM `_zopc_prod_sync_cols_z-qa-doc-002-deploy-init`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_t = NULL;
  OPEN cur;
  lp: LOOP
    FETCH cur INTO v_t, v_c, v_d;
    IF v_t IS NULL THEN LEAVE lp; END IF;
    IF EXISTS (
         SELECT 1 FROM information_schema.TABLES
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t)
       AND NOT EXISTS (
         SELECT 1 FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t AND COLUMN_NAME = v_c) THEN
      SET @ddl = CONCAT('ALTER TABLE `', v_t, '` ADD COLUMN `', v_c, '` ', v_d);
      PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-qa-doc-002-deploy-init`;
CREATE TEMPORARY TABLE `_zopc_prod_sync_cols_z-qa-doc-002-deploy-init` (tbl VARCHAR(64), col VARCHAR(64), defn TEXT);
INSERT INTO `_zopc_prod_sync_cols_z-qa-doc-002-deploy-init` (tbl, col, defn) VALUES
  ('z_qa_e2e_action', 'e2e_screenshot', 'tinyint NOT NULL DEFAULT ''1'' COMMENT ''该操作是否截图'''),
  ('z_qa_plan', 'notify_channels', 'varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''通知渠道 (in_app,email)'''),
  ('z_qa_plan', 'notify_on_fail', 'tinyint NOT NULL DEFAULT ''1'''),
  ('z_qa_plan', 'notify_on_pass', 'tinyint NOT NULL DEFAULT ''0'''),
  ('z_qa_run', 'abort_requested', 'tinyint NOT NULL DEFAULT ''0'' COMMENT ''中止请求标志;跑这条 run 的实例在步骤间轮询它'''),
  ('z_qa_run', 'callback_url', 'varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''CI 回调 URL'''),
  ('z_qa_run', 'env_base_url', 'varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''执行环境快照 baseUrl'''),
  ('z_qa_run', 'env_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''执行环境编码 (z_qa_env.code)'''),
  ('z_qa_run_detail', 'defect_type', 'varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''缺陷分类: system_defect/script_defect/env_data_defect'''),
  ('z_qa_run_detail', 'extractions', 'longtext COLLATE utf8mb4_unicode_ci COMMENT ''变量提取过程 JSON'''),
  ('z_qa_run_detail', 'mock_endpoint_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''Mock 端点编码 (mock 模式时)'''),
  ('z_qa_run_detail', 'suite_id', 'bigint DEFAULT NULL COMMENT ''归属套件 (计划 run 一次跨多套件)'''),
  ('z_qa_run_detail', 'target_mode', 'varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''目标: real=真实服务 mock=Mock端点 (执行时从步骤快照)'''),
  ('z_qa_run_detail', 'template_request', 'text COLLATE utf8mb4_unicode_ci COMMENT ''模板请求 JSON (变量替换前)'''),
  ('z_qa_suite', 'fail_fast', 'tinyint NOT NULL DEFAULT ''0'' COMMENT ''遇错即停:某步失败后剩余步骤落 skip 并结束 run'''),
  ('z_qa_suite_step', 'e2e_action_type', 'varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''E2E动作: navigate/click/fill/assert'''),
  ('z_qa_suite_step', 'e2e_screenshot', 'tinyint NOT NULL DEFAULT ''1'' COMMENT ''是否截图'''),
  ('z_qa_suite_step', 'e2e_selector', 'varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''元素定位器'''),
  ('z_qa_suite_step', 'e2e_value', 'text COLLATE utf8mb4_unicode_ci COMMENT ''输入值/预期值''');

CALL `zopc_sync_prod_cols_z-qa-doc-002-deploy-init`();
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-qa-doc-002-deploy-init`;
DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-qa-doc-002-deploy-init`;
