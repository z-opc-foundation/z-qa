-- =============================================================================
-- z-qa 测试平台完整 DDL（单一入口，可重复执行）
--
-- 由原 z_qa_ddl.sql + z_qa_ddl_patch.sql + z_qa_ddl_patch2.sql 三份顺序合并而成。
-- 分段保留了原有的阶段注释，排查"某列是谁加的"时按小节标题定位。
--
-- ⚠️ 第 3 段（patch2）用了存储过程 + DELIMITER，必须走 mysql 客户端执行，
--    不能塞进 JDBC 单语句通道。
--
-- 12 张表：z_qa_suite / z_qa_suite_step / z_qa_case / z_qa_plan / z_qa_run /
--         z_qa_run_detail / z_qa_env / z_qa_assert_template / z_qa_schedule /
--         z_qa_e2e_session / z_qa_e2e_action / z_qa_notify_log
-- =============================================================================

-- #################### 第 1 段：基线 8 张表 ####################
-- ========================================
-- z-qa 测试平台 DDL (FEATURE052)
-- 数据库: oc
-- 特点: CREATE TABLE IF NOT EXISTS, 可重复执行
-- ========================================

-- 1. 测试套件 (Test Suite) — 一组有序步骤的集合
CREATE TABLE IF NOT EXISTS `z_qa_suite`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `code`
    VARCHAR
(
    64
) NOT NULL COMMENT '套件编码 (唯一)',
    `name` VARCHAR
(
    128
) NOT NULL COMMENT '套件名称',
    `description` VARCHAR
(
    500
) NULL COMMENT '套件描述',
    `category` VARCHAR
(
    64
) NULL COMMENT '分类 (smoke/regression/api/e2e)',
    `env_code` VARCHAR
(
    64
) NULL COMMENT '默认环境 (env.baseUrl)',
    `owner` VARCHAR
(
    64
) NULL COMMENT '负责人',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `tags` VARCHAR
(
    256
) NULL COMMENT '逗号分隔标签',
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
    UNIQUE KEY `uk_code`
(
    `code`
),
    KEY `idx_category`
(
    `category`
),
    KEY `idx_tenant`
(
    `tenant_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='测试套件';

-- 2. 套件步骤 (Suite Step) — 一个 HTTP 请求或一个 E2E 操作
CREATE TABLE IF NOT EXISTS `z_qa_suite_step`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `suite_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '所属套件 ID',
    `step_no`
    INT
    NOT
    NULL
    COMMENT
    '步骤序号 (从1开始)',
    `step_name`
    VARCHAR
(
    128
) NOT NULL COMMENT '步骤名称',
    `execution_mode` VARCHAR
(
    8
) NOT NULL DEFAULT 'api' COMMENT 'api=HTTP请求 e2e=浏览器操作',
    `method` VARCHAR
(
    8
) NULL COMMENT 'HTTP method: GET/POST/PUT/DELETE',
    `url` VARCHAR
(
    512
) NULL COMMENT 'HTTP URL',
    `headers_json` TEXT NULL COMMENT '请求头 JSON',
    `body` LONGTEXT NULL COMMENT '请求体 (可含 ${VAR} 变量)',
    `target_mode` VARCHAR
(
    16
) NOT NULL DEFAULT 'real' COMMENT '目标: real=真实服务 mock=Mock端点',
    `mock_endpoint` VARCHAR
(
    256
) NULL COMMENT 'Mock 端点 code',
    `extract_rules` TEXT NULL COMMENT 'JSONPath 变量提取规则 JSON',
    `assert_rules` TEXT NULL COMMENT '断言规则 JSON (eq/gt/contains/regex)',
    `timeout_ms` INT NOT NULL DEFAULT 10000 COMMENT '单步超时',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '失败重试次数',
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
    KEY `idx_suite_id`
(
    `suite_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='套件步骤';

-- 3. 测试用例 (Test Case) — 可被多个套件复用
CREATE TABLE IF NOT EXISTS `z_qa_case`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `code`
    VARCHAR
(
    64
) NOT NULL COMMENT '用例编码 (唯一)',
    `name` VARCHAR
(
    128
) NOT NULL,
    `description` VARCHAR
(
    500
) NULL,
    `priority` VARCHAR
(
    16
) NOT NULL DEFAULT 'P1' COMMENT 'P0/P1/P2/P3',
    `category` VARCHAR
(
    64
) NULL,
    `owner` VARCHAR
(
    64
) NULL,
    `tags` VARCHAR
(
    256
) NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
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
    UNIQUE KEY `uk_code`
(
    `code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='测试用例';

-- 4. 测试计划 (Test Plan) — 一组用例/套件的批量执行安排
CREATE TABLE IF NOT EXISTS `z_qa_plan`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `code`
    VARCHAR
(
    64
) NOT NULL COMMENT '计划编码 (唯一)',
    `name` VARCHAR
(
    128
) NOT NULL,
    `description` VARCHAR
(
    500
) NULL,
    `suite_ids` TEXT NULL COMMENT 'JSON 数组: 关联套件 ID 列表',
    `case_ids` TEXT NULL COMMENT 'JSON 数组: 关联用例 ID 列表',
    `cron_expr` VARCHAR
(
    64
) NULL COMMENT 'cron 表达式 (空=手动)',
    `notify_type` VARCHAR
(
    32
) NULL COMMENT 'none/inapp/email/all',
    `status` TINYINT NOT NULL DEFAULT 1,
    `owner` VARCHAR
(
    64
) NULL,
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
    UNIQUE KEY `uk_code`
(
    `code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='测试计划';

-- 5. 执行记录 (Run) — 一次完整执行
CREATE TABLE IF NOT EXISTS `z_qa_run`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `run_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '执行编码 (唯一, RUN-{yyyyMMddHHmmss}-{seq})',
    `plan_id` BIGINT NULL COMMENT '触发的计划 ID (手动=NULL)',
    `suite_id` BIGINT NULL COMMENT '触发的套件 ID (单套件时填)',
    `name` VARCHAR
(
    128
) NULL,
    `trigger_type` VARCHAR
(
    16
) NOT NULL DEFAULT 'manual' COMMENT 'manual/cron/ci',
    `status` VARCHAR
(
    16
) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed/cancelled',
    `total_steps` INT NOT NULL DEFAULT 0,
    `passed_steps` INT NOT NULL DEFAULT 0,
    `failed_steps` INT NOT NULL DEFAULT 0,
    `duration_ms` BIGINT NULL,
    `started_at` DATETIME NULL,
    `finished_at` DATETIME NULL,
    `error_message` VARCHAR
(
    1024
) NULL,
    `trigger_user` VARCHAR
(
    64
) NULL,
    `tenant_code` VARCHAR
(
    64
) NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_run_code`
(
    `run_code`
),
    KEY `idx_status`
(
    `status`
),
    KEY `idx_plan_id`
(
    `plan_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='执行记录';

-- 6. 执行详情 (Run Detail) — 每步的完整溯源
CREATE TABLE IF NOT EXISTS `z_qa_run_detail`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `run_id`
    BIGINT
    NOT
    NULL,
    `step_no`
    INT
    NOT
    NULL,
    `step_name`
    VARCHAR
(
    128
) NOT NULL,
    `method` VARCHAR
(
    8
) NULL,
    `request_url` VARCHAR
(
    512
) NULL,
    `request_headers` TEXT NULL,
    `request_body` LONGTEXT NULL,
    `response_status` INT NULL,
    `response_headers` TEXT NULL,
    `response_body` LONGTEXT NULL,
    `variables_before` LONGTEXT NULL COMMENT '执行前变量快照 JSON',
    `variables_after` LONGTEXT NULL COMMENT '执行后变量快照 JSON',
    `extracted_vars` LONGTEXT NULL COMMENT '本次提取的变量 JSON',
    `assert_results` LONGTEXT NULL COMMENT '断言结果 JSON',
    `result` VARCHAR
(
    16
) NOT NULL DEFAULT 'pending' COMMENT 'pending/pass/fail/error/skip',
    `error_message` VARCHAR
(
    2048
) NULL,
    `duration_ms` BIGINT NULL,
    `retry_count` INT NOT NULL DEFAULT 0,
    `tenant_code` VARCHAR
(
    64
) NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_run_id`
(
    `run_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='执行详情';

-- 7. 测试环境 (Environment)
CREATE TABLE IF NOT EXISTS `z_qa_env`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `code`
    VARCHAR
(
    64
) NOT NULL,
    `name` VARCHAR
(
    128
) NOT NULL,
    `base_url` VARCHAR
(
    512
) NOT NULL,
    `headers_json` TEXT NULL COMMENT '默认请求头 JSON',
    `description` VARCHAR
(
    500
) NULL,
    `priority` INT NOT NULL DEFAULT 0 COMMENT '排序, 越大越靠前',
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
    UNIQUE KEY `uk_code`
(
    `code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='测试环境';

-- 8. 断言规则模板 (Assert Template)
CREATE TABLE IF NOT EXISTS `z_qa_assert_template`
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
) NOT NULL,
    `category` VARCHAR
(
    32
) NOT NULL DEFAULT 'jsonpath' COMMENT 'jsonpath/status/body',
    `expression` VARCHAR
(
    256
) NOT NULL COMMENT 'JSONPath 或 status code 或 body regex',
    `operator` VARCHAR
(
    16
) NOT NULL DEFAULT 'eq' COMMENT 'eq/neq/gt/lt/contains/matches',
    `expected` VARCHAR
(
    256
) NULL COMMENT '期望值',
    `description` VARCHAR
(
    500
) NULL,
    `tenant_code` VARCHAR
(
    64
) NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='断言规则模板';

-- 预设数据
INSERT
IGNORE INTO `z_qa_env` (`code`, `name`, `base_url`, `description`, `priority`) VALUES
('local', '本地环境', 'http://127.0.0.1:8888', 'z-opc 本地开发', 10),
('prod', '生产环境', 'https://opc.zopc.top', 'z-opc 线上', 1);

-- #################### 第 2 段：Phase 3 补全 4 张表 ####################
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

-- #################### 第 3 段：P0 地基 + P2 十态状态机 ####################
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

