# 数据库结构

## 执行入口

DDL 已收口为**单一文件**：[`_doc/002_deploy/init/schema.sql`](../../_doc/002_deploy/init/schema.sql)。
按第 1 / 2 / 3 段顺序执行即可，每段保留了原阶段注释，排查"某列是谁加的"时按小节标题定位。

```bash
mysql -h <host> -u <user> -p <database> < _doc/002_deploy/init/schema.sql
```

⚠️ **第 3 段用了存储过程 + `DELIMITER`，必须走 `mysql` 客户端执行，不能塞进 JDBC 单语句通道。**

目标库名 `oc`（第 3 段头注：MySQL 8.0.36，`sql_mode` 无 `ANSI_QUOTES`）。
除 `DROP COLUMN` 外全部 `IF NOT EXISTS` / `MODIFY`，可重复执行。

## 12 张表

| 表 | 用途 |
|----|------|
| `z_qa_suite` | 测试套件：一组有序步骤的集合 |
| `z_qa_suite_step` | 套件步骤：一个 HTTP 请求或一个 E2E 操作 |
| `z_qa_case` | 测试用例：可被多个套件复用的断言资产 |
| `z_qa_plan` | 测试计划：一组套件/用例的批量执行安排 |
| `z_qa_run` | 执行记录：一次完整执行 |
| `z_qa_run_detail` | 执行详情：每步的完整溯源（请求/响应/变量/断言/定性） |
| `z_qa_env` | 测试环境：`base_url` + 变量 |
| `z_qa_assert_template` | 断言规则模板（**目前只有表与 Mapper，无 Service 与 API**） |
| `z_qa_schedule` | 定时执行 |
| `z_qa_e2e_session` | E2E 浏览器会话 |
| `z_qa_e2e_action` | E2E 操作步骤 |
| `z_qa_notify_log` | 通知投递日志 |

全部表带 `tenant_code`（多租户隔离）与 `gmt_create` / `gmt_modified`。

## 关键列

### `z_qa_suite`
`code` · `name` · `env_code`（默认环境）· `status` · **`fail_fast`**（套件级遇错即停开关）

### `z_qa_suite_step`
`suite_id` · **`step_no`（套件内序号，不是全局唯一）** · `execution_mode`（`api` / `e2e`）·
`method` · `url` · `headers_json` · `body` · `target_mode` · `mock_endpoint` ·
`extract_rules`（JSONPath → `${VAR}`）· `assert_rules` · `timeout_ms` · `retry_count`

### `z_qa_run`
`run_code` · `plan_id` · `suite_id` · `status`（`running` / `success` / `failed` / `cancelled`）·
`total_steps` / `passed_steps` / `failed_steps` · `started_at` / `finished_at` / `duration_ms` ·
**`env_code`** · `env_base_url` · `callback_url` · **`abort_requested`**

### `z_qa_run_detail`
`run_id` · **`suite_id`** · `step_no` · `request_*` / `response_*` ·
`variables_before` / `variables_after` / `extracted_vars` · `assert_results` ·
**`result`**（十态，见 `02-api.md`）· **`defect_type`**（三分类）· **`target_mode`** ·
`mock_endpoint_code` · `template_request` · `extractions` · `retry_count`

索引：`idx_run_result` · `idx_suite_id`。

### `z_qa_run.abort_requested`
中止标志走 DB 而非内存 flag：发起中止的实例未必是跑这条 run 的实例。
配套约束 —— `QaRunRunner` 写计数与终态时**只写自己那几列**，整行回写会把
`abort_requested` 覆盖回内存快照的旧值，中止请求就丢了。

索引：`idx_abort_requested`。

### `z_qa_schedule`
`target_type`（`suite` / `plan`）· `suite_id` / `plan_id` · `cron_expr` · `env_code` · `enabled` ·
`notify_channels` · `notify_on_pass` / `notify_on_fail` ·
`last_run_id` / `last_run_time` / `last_run_result` · **`next_run_time`**（自实现 cron 解析算出）

## 历史踩坑（DDL 与实体曾经对不上）

第 3 段（`patch2`）修的正是这一类问题 —— 实体有映射、DDL 从未定义，落库必抛 `Unknown column`：

| 问题 | 修法 |
|------|------|
| `z_qa_run.env_code` 实体有映射、DDL 从未定义 | ADD COLUMN |
| `z_qa_e2e_action.e2e_screenshot` 同上 | ADD COLUMN |
| `z_qa_run_detail.result` 是 `VARCHAR(16)`，装不下 `fail_system_error`（18 字符） | 放宽到 `VARCHAR(32)` |
| 套件级 `failFast` / 跨实例可用的 abort 标志 / 计划运行的套件归属都缺列 | ADD COLUMN |
| `z_qa_run_detail.target_mode` 实体有映射、DDL 从未定义 | ADD COLUMN |

同时把历史 `pass` / `fail` / `error` 迁移到新词表并回填 `defect_type`：
旧 `error` 无法反推具体原因 → `fail_unknown`；旧 `fail` 绝大多数是断言不符 → `fail_assert`。

## 被 DROP 的列

`z_qa_run.step_total` / `step_pass` / `step_fail` / `step_skip` 与
`total_steps` / `passed_steps` / `failed_steps` 是同一份数据。
改造前两条执行链各写一套、互相覆盖；现在只有一套循环写 `*_steps`，这四列不再被任何代码映射，
DDL 里以注释保留 `CALL` 行（**默认不执行**）。存量库里的列保留 —— 删列是不可逆动作。

## Mapper XML

只有四个 Mapper 有 XML（其余走 MyBatis-Plus 通用方法）：

`QaRunMapper.xml`（`countByStatus` / `sumPassed` / `sumTotal`）·
`QaRunDetailMapper.xml`（`topFailures`）· `QaSuiteStepMapper.xml`（`selectBySuiteId`）·
`QaE2eActionMapper.xml`

## 冒烟数据

[`_doc/005_testing/self_test.sql`](../../_doc/005_testing/self_test.sql) 建出 `zopc-self-test` 套件
共 10 步：健康检查 → 登录 → 我的租户 → 调度中心 jobinfo/jobgroup/joblog/dashboard →
应用管理 → 环境列表 → 套件列表。

第 2 步登录的凭据在文件里是**占位串**，执行前替换成你自己的账号 —— 仓库内不落明文凭据。
建好后 `POST /api/qa/run/trigger`，看 `z_qa_run_detail` 与 `z_qa_run` 的计数与定性。
