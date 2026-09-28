-- ========================================
-- FEATURE052 Phase 7: z-opc self-test 套件预设
-- 数据库: oc
-- 作用: 预设一套集成测试, 让 z-opc 测试自己
-- ========================================

-- 清理已有 self-test
DELETE
FROM z_qa_suite_step
WHERE suite_id IN (SELECT id FROM z_qa_suite WHERE code = 'zopc-self-test');
DELETE
FROM z_qa_suite
WHERE code = 'zopc-self-test';

-- 1. 创建套件
INSERT INTO z_qa_suite (code, name, description, category, env_code, owner, status, version, tags, gmt_create,
                        gmt_modified)
VALUES ('zopc-self-test', 'z-opc 自身集成测试',
        '测试 z-opc 自己核心 API: 健康 → 登录 → 我的租户 → 调度中心 → 应用管理',
        'smoke', 'local', 'admin', 1, 1, 'z-opc,self-test,ci',
        NOW(), NOW());

SET
@suite_id = LAST_INSERT_ID();

-- 2. 套件步骤 (7 步核心链路)
INSERT INTO z_qa_suite_step
(suite_id, step_no, step_name, execution_mode, method, url, headers_json, body, target_mode, extract_rules,
 assert_rules, timeout_ms, retry_count, gmt_create, gmt_modified)
VALUES
-- Step 1: 健康检查 (text/html, 不解析为 JSON)
(@suite_id, 1, '健康检查 (健康页可访问)', 'api', 'GET', '/doc.html', NULL, NULL, 'real',
 NULL, '{"status": 200}', 5000, 0, NOW(), NOW()),

-- Step 2: 登录 (用已知凭据) — body 含 Content-Type 默认
(@suite_id, 2, '管理员登录', 'api', 'POST', '/api/ctc/authn/login',
 '{"Content-Type": "application/json"}',
 '{"identifier": "admin", "password": "admin", "identityType": 1}',
 'real',
 '{"TOKEN": "$.data.token"}',
 '{"status": 200, "$.code": 200}', 10000, 0, NOW(), NOW()),

-- Step 3: 我的租户 (需带 token)
(@suite_id, 3, '查询我的租户列表', 'api', 'GET', '/api/ctc/authn/my-tenants',
 '{"Authorization": "Bearer ${TOKEN}"}',
 NULL, 'real',
 NULL, '{"status": 200}', 10000, 0, NOW(), NOW()),

-- Step 4: 调度中心 - jobinfo
(@suite_id, 4, '查询调度任务列表', 'api', 'GET', '/jobinfo/list', NULL, NULL, 'real',
 NULL, '{"status": 200, "$.success": true}', 10000, 0, NOW(), NOW()),

-- Step 5: 调度中心 - jobgroup
(@suite_id, 5, '查询调度执行器', 'api', 'GET', '/jobgroup/list', NULL, NULL, 'real',
 NULL, '{"status": 200, "$.success": true}', 10000, 0, NOW(), NOW()),

-- Step 6: 调度中心 - joblog
(@suite_id, 6, '查询调度日志', 'api', 'GET', '/joblog/list', NULL, NULL, 'real',
 NULL, '{"status": 200, "$.success": true}', 10000, 0, NOW(), NOW()),

-- Step 7: 调度中心 - dashboard
(@suite_id, 7, '查询调度仪表盘', 'api', 'GET', '/api/schedule', NULL, NULL, 'real',
 NULL, '{"status": 200}', 10000, 0, NOW(), NOW()),

-- Step 8: 应用管理
(@suite_id, 8, '查询应用列表', 'api', 'GET', '/api/meta-app/list', NULL, NULL, 'real',
 NULL, '{"status": 200, "$.success": true}', 10000, 0, NOW(), NOW()),

-- Step 9: 环境列表 (qa 自己的)
(@suite_id, 9, '查询测试平台环境列表', 'api', 'GET', '/api/qa/env/list', NULL, NULL, 'real',
 NULL, '{"status": 200, "$.success": true}', 10000, 0, NOW(), NOW()),

-- Step 10: 套件列表 (qa 自己的)
(@suite_id, 10, '查询测试平台套件列表', 'api', 'GET', '/api/qa/suite/list', NULL, NULL, 'real',
 NULL, '{"status": 200, "$.success": true}', 10000, 0, NOW(), NOW());

-- 验证
SELECT '预设套件创建成功' AS status, COUNT(*) AS step_count
FROM z_qa_suite_step
WHERE suite_id = @suite_id;