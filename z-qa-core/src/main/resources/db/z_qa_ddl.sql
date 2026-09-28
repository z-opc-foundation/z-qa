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