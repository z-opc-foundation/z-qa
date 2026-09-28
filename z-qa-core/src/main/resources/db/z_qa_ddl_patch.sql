-- ========================================
-- z-qa 测试平台 DDL 补丁 (FEATURE052 Phase 3 补全)
-- 适用: 在 z_qa_ddl.sql 执行完后追加执行
-- 数据库: oc
-- 兼容: MySQL 5.7 / 8.0
-- 特点: CREATE TABLE IF NOT EXISTS, 可重复执行 (ALTER 用存储过程包装)
-- ========================================

-- 9. 定时执行 (Schedule) — 套件或计划的定时执行
CREATE TABLE IF NOT EXISTS `z_qa_schedule`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `name`
    VARCHAR
(
    128
) NOT NULL COMMENT '定时任务名称',
    `target_type` VARCHAR
(
    16
) NOT NULL DEFAULT 'suite' COMMENT 'suite=单套件 plan=批量计划',
    `suite_id` BIGINT NULL COMMENT 'target_type=suite 时必填',
    `plan_id` BIGINT NULL COMMENT 'target_type=plan 时必填 (关联 z_qa_plan.id)',
    `cron_expr` VARCHAR
(
    64
) NOT NULL COMMENT 'Cron 表达式 (例如 0 0 23 * * ?)',
    `env_code` VARCHAR
(
    64
) NULL COMMENT '执行环境 (env.baseUrl)',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `notify_channels` VARCHAR
(
    128
) NULL COMMENT '通知渠道 (逗号分隔: in_app,email)',
    `notify_on_pass` TINYINT NOT NULL DEFAULT 0 COMMENT '通过也通知',
    `notify_on_fail` TINYINT NOT NULL DEFAULT 1 COMMENT '失败才通知',
    `last_run_id` BIGINT NULL COMMENT '上次执行 ID',
    `last_run_time` DATETIME NULL COMMENT '上次执行时间',
    `last_run_result` VARCHAR
(
    16
) NULL COMMENT '上次执行结果',
    `next_run_time` DATETIME NULL COMMENT '下次执行时间',
    `tenant_code` VARCHAR
(
    64
) NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_target`
(
    `target_type`,
    `suite_id`,
    `plan_id`
),
    KEY `idx_enabled`
(
    `enabled`
),
    KEY `idx_next_run_time`
(
    `next_run_time`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='测试定时执行';

-- 10. E2E 浏览器会话
CREATE TABLE IF NOT EXISTS `z_qa_e2e_session`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `run_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '关联执行记录 ID (z_qa_run.id)',
    `run_detail_id`
    BIGINT
    NULL
    COMMENT
    '关联步骤明细 ID (z_qa_run_detail.id)',
    `browser_type`
    VARCHAR
(
    16
) NOT NULL DEFAULT 'chromium' COMMENT 'chromium/firefox/webkit',
    `target_url` VARCHAR
(
    512
) NOT NULL COMMENT '目标 URL',
    `viewport_width` INT NULL DEFAULT 1920 COMMENT '视口宽',
    `viewport_height` INT NULL DEFAULT 1080 COMMENT '视口高',
    `video_oss_key` VARCHAR
(
    256
) NULL COMMENT '视频 OSS 路径 (z-oss)',
    `trace_oss_key` VARCHAR
(
    256
) NULL COMMENT 'Trace OSS 路径',
    `har_oss_key` VARCHAR
(
    256
) NULL COMMENT 'HAR OSS 路径',
    `status` VARCHAR
(
    16
) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/done/failed',
    `started_at` DATETIME NULL,
    `finished_at` DATETIME NULL,
    `tenant_code` VARCHAR
(
    64
) NULL,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_run_id`
(
    `run_id`
),
    KEY `idx_run_detail_id`
(
    `run_detail_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='E2E 浏览器会话';

-- 11. E2E 操作步骤
CREATE TABLE IF NOT EXISTS `z_qa_e2e_action`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `session_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '关联会话 ID (z_qa_e2e_session.id)',
    `step_no`
    INT
    NOT
    NULL
    COMMENT
    '操作序号',
    `action_type`
    VARCHAR
(
    16
) NOT NULL COMMENT 'navigate/click/fill/select/hover/wait/assert_text/assert_visible/screenshot',
    `selector` VARCHAR
(
    512
) NULL COMMENT '元素定位器 (CSS/XPath)',
    `action_value` TEXT NULL COMMENT '输入值/预期值',
    `description` VARCHAR
(
    256
) NULL COMMENT '操作描述',
    `screenshot_oss_key` VARCHAR
(
    256
) NULL COMMENT '截图 OSS 路径',
    `result` VARCHAR
(
    16
) NOT NULL DEFAULT 'pending' COMMENT 'pending/pass/fail/error',
    `error_message` VARCHAR
(
    1024
) NULL,
    `duration_ms` BIGINT NULL,
    `started_at` DATETIME NULL,
    `finished_at` DATETIME NULL,
    `tenant_code` VARCHAR
(
    64
) NULL,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_session_id`
(
    `session_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='E2E 操作步骤';

-- 12. 通知投递日志
CREATE TABLE IF NOT EXISTS `z_qa_notify_log`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `run_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '关联执行 ID',
    `channel`
    VARCHAR
(
    16
) NOT NULL COMMENT 'in_app/email',
    `target` VARCHAR
(
    256
) NULL COMMENT '收件方',
    `payload` TEXT NULL COMMENT '消息负载 JSON',
    `status` VARCHAR
(
    16
) NOT NULL DEFAULT 'pending' COMMENT 'pending/sent/failed',
    `error_message` VARCHAR
(
    512
) NULL,
    `sent_at` DATETIME NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_run_id`
(
    `run_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='通知投递日志';

-- ============== ALTER 补全字段 (使用存储过程实现 IF NOT EXISTS) ==============

DROP PROCEDURE IF EXISTS `z_qa_add_column_if_not_exists`;

DELIMITER $$

CREATE PROCEDURE `z_qa_add_column_if_not_exists`(
    IN p_table VARCHAR (128),
    IN p_column VARCHAR (128),
    IN p_definition TEXT
)
BEGIN
    DECLARE
v_count INT;
SELECT COUNT(*)
INTO v_count
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = p_table
  AND COLUMN_NAME = p_column;
IF
v_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;
END$$

DELIMITER ;

-- z_qa_suite_step 补 E2E 字段
CALL `z_qa_add_column_if_not_exists`('z_qa_suite_step', 'e2e_action_type', 'VARCHAR(16) NULL COMMENT "E2E动作: navigate/click/fill/assert"');
CALL `z_qa_add_column_if_not_exists`('z_qa_suite_step', 'e2e_selector', 'VARCHAR(512) NULL COMMENT "元素定位器"');
CALL `z_qa_add_column_if_not_exists`('z_qa_suite_step', 'e2e_value', 'TEXT NULL COMMENT "输入值/预期值"');
CALL `z_qa_add_column_if_not_exists`('z_qa_suite_step', 'e2e_screenshot', 'TINYINT NOT NULL DEFAULT 1 COMMENT "是否截图"');

-- z_qa_run 补字段
CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'env_base_url', 'VARCHAR(512) NULL COMMENT "执行环境快照 baseUrl"');
CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'callback_url', 'VARCHAR(512) NULL COMMENT "CI 回调 URL"');
-- 以下 4 列与 total_steps/passed_steps/failed_steps 完全重复且从未被写入，
-- 已从 QaRunDO 与 QaRunMapper.xml 摘除映射；存量库里的列保留 (删列是不可逆动作，见 patch2 的可选 DROP)
-- CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'step_total', 'INT NOT NULL DEFAULT 0 COMMENT "总步骤数"');
-- CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'step_pass', 'INT NOT NULL DEFAULT 0 COMMENT "通过步骤数"');
-- CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'step_fail', 'INT NOT NULL DEFAULT 0 COMMENT "失败步骤数"');
-- CALL `z_qa_add_column_if_not_exists`('z_qa_run', 'step_skip', 'INT NOT NULL DEFAULT 0 COMMENT "跳过步骤数"');

-- z_qa_run_detail 补字段 (FEATURE052 §3.3 溯源存储)
CALL `z_qa_add_column_if_not_exists`('z_qa_run_detail', 'mock_endpoint_code', 'VARCHAR(64) NULL COMMENT "Mock 端点编码 (mock 模式时)"');
CALL `z_qa_add_column_if_not_exists`('z_qa_run_detail', 'template_request', 'TEXT NULL COMMENT "模板请求 JSON (变量替换前)"');
CALL `z_qa_add_column_if_not_exists`('z_qa_run_detail', 'extractions', 'LONGTEXT NULL COMMENT "变量提取过程 JSON"');

-- z_qa_plan 强化通知字段 (FEATURE052 §9.5)
CALL `z_qa_add_column_if_not_exists`('z_qa_plan', 'notify_channels', 'VARCHAR(128) NULL COMMENT "通知渠道 (in_app,email)"');
CALL `z_qa_add_column_if_not_exists`('z_qa_plan', 'notify_on_pass', 'TINYINT NOT NULL DEFAULT 0');
CALL `z_qa_add_column_if_not_exists`('z_qa_plan', 'notify_on_fail', 'TINYINT NOT NULL DEFAULT 1');

DROP PROCEDURE `z_qa_add_column_if_not_exists`;
