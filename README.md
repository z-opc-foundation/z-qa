# z-qa · 测试平台

步骤式 API 链路测试平台：用例 / 套件 / 计划 / 环境 / 定时 / 执行 / 报告 / 质量看板。
Spring Boot 2.7 + Java 8 字节码，MyBatis-Plus，AutoConfiguration 一行 import 即启用。

2026-09-28 从 z-opc monorepo 迁出为独立仓库并发布到 Maven Central。
迁出前的一期实现与二阶段蒸馏计划见 z-opc 的 `_doc/001_feature/052_测试平台（z-qa）/feature052.md`。

## 坐标

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-qa-web</artifactId>   <!-- 传递带上 z-qa-core -->
    <version>1.0.0</version>
</dependency>
```

| module | 内容 |
|---|---|
| `z-qa-core` | entity / mapper / service + 执行引擎（`QaExecutionDispatcher`、`QaRunRunner`、`ApiStepExecutor`、`QaFailureClassifier`、`QaStepStatus`）+ 9 个 Controller + DDL |
| `z-qa-web` | `QaAutoConfiguration`（`META-INF/spring.factories`）+ `QaModuleDataSource`（独立数据源 + `@MapperScan`） |

宿主应用**不需要** `@ComponentScan`：`spring.factories` 会注册
`QaAutoConfiguration`，它自己扫 `com.zifang.z.qa.web` 与 `com.zifang.z.qa.admin`。

## 配置

z-qa 用独立数据源（Bean 名 `dataSourceQa` / `sqlSessionFactoryQa`）。
每个 key 缺省时回落到 `z.base.db.default.*`，再回落到硬编码默认值：

```properties
z.base.db.qa.host=127.0.0.1
z.base.db.qa.port=3306
z.base.db.qa.database=oc
z.base.db.qa.username=
z.base.db.qa.password=
# 连接池（同样支持 z.base.db.qa.initial-size / min-idle / max-active / max-wait / remove-abandoned）
```

E2E 会话（浏览器侧）连的是 browserless 服务，**必须走环境变量或配置中心，不要写死**：

```properties
z-qa.e2e.token=${Z_QA_E2E_TOKEN:}
```

## 建表

DDL 随 jar 发布在 `z-qa-core` 的 `db/` 资源目录：

| 文件 | 用途 |
|---|---|
| `db/z_qa_ddl.sql` | 12 张表的基线（`z_qa_suite` / `z_qa_suite_step` / `z_qa_case` / `z_qa_plan` / `z_qa_run` / `z_qa_run_detail` / `z_qa_env` / `z_qa_assert_template` / `z_qa_schedule` / `z_qa_e2e_session` / `z_qa_e2e_action` / `z_qa_notify_log`），`CREATE TABLE IF NOT EXISTS` 可重复执行 |
| `db/z_qa_ddl_patch.sql` | 一期增量列 |
| `db/z_qa_ddl_patch2.sql` | 二阶段口径：`z_qa_run.abort_requested` + `idx_abort_requested`、`z_qa_suite.fail_fast`，并**删除** `z_qa_run.step_total/step_pass/step_fail/step_skip`（计数改为从 `z_qa_run_detail` 聚合） |
| `db/z_qa_self_test.sql` | 冒烟数据：一条「登录 → 取租户」的两步套件（用的是本地 demo 账号 `admin/admin`，别拿它当真凭据） |

## API

| 前缀 | Controller | 说明 |
|---|---|---|
| `/api/qa/case/**` | `CaseController` | 用例 CRUD + 分页 |
| `/api/qa/suite/**` | `SuiteController` | 套件 CRUD、步骤编排、`failFast` |
| `/api/qa/plan/**` | `PlanController` | 计划（一组套件）与执行 |
| `/api/qa/env/**` | `EnvController` | 环境（baseUrl + 变量） |
| `/api/qa/schedules/**` | `ScheduleController` | 定时触发 |
| `/api/qa/report/**` | `ReportController` | 报告与链路渲染 |
| `/api/qa/dashboard/**` | `DashboardController` | 质量看板聚合 |
| `/api/qa/e2e/**` | `E2eController` | 浏览器录制会话 |
| `/api/qa/**` | `RunController` | `run/page`、`run/detail`、`run/abort`、`run/status` |

约定：列表/分页用 **POST**，详情 `GET /api/qa/xxx/{id}`，响应 `{ code, msg, data, success }`，
鉴权 `Authorization: Bearer {jwt}`。

JSON 字段名全局是 **snake_case**，由宿主的 Jackson `PropertyNamingStrategy` 决定；
本模块不自己配 Jackson。三处手写 `Map` 的出口保持 camelCase：
`/api/qa/run/status`、`/api/qa/dashboard/*`、`E2eController`。

## 执行模型

```
POST /api/qa/plan/run  或  定时触发
        └─ QaExecutionDispatcher：建 z_qa_run 行（pending）→ 交异步
             └─ QaRunRunner：按套件顺序跑，套件内按 step_no 跑
                  └─ ApiStepExecutor.execute(QaSuiteStepDO, QaEnvDO, ctx)
                        变量替换 → HTTP 调用 → JSONPath 提取 → 断言
                  └─ QaFailureClassifier 定性 → 写 z_qa_run_detail
```

- **步骤十态**：`pending` `running` `success` `skip` `fail_assert` `fail_system_error`
  `fail_timeout` `fail_navigation` `fail_param_null` `fail_unknown`
- **缺陷三分类**：`system_defect`（被测系统）/ `script_defect`（脚本写法）/ `env_data_defect`（环境数据）
- **定性优先于断言**：`detectSystemError` 在断言之前问一次，否则 500 会被顺带失败的断言掩盖成
  `fail_assert`，缺陷单直接开错方向
- **retry**：只对 `isRetryable` 的状态重试（超时 / 连不上 / 5xx / unknown）；断言不符与入参未解析重试也不会变
- **failFast**：套件级，失败即跳过该套件剩余步骤（其余套件继续）
- **abort**：宿主置 `z_qa_run.abort_requested=1`，runner 在步骤边界轮询数据库后中止

## 已知边界

- `QaRunRunner` 的步骤模式目前只有 `ApiStepExecutor`。`mode=e2e` 的步骤没有对应执行器，
  会落成 `fail_unknown`（"步骤模式无对应执行器"）而不是假装成功。Playwright over WebSocket 的
  `E2eStepExecutor` 在计划里。
- 步骤证据链只有请求/响应/变量/断言四件套，没有截图、Trace、HAR。
- `z-qa-core` 传递依赖里排掉了 `log4j-slf4j2-impl`：Spring Boot 2.7 的栈是 slf4j 1.7.36，
  两个 jar 共用 `org.apache.logging.slf4j` 包名，同时上 classpath 会在启动时 `NoSuchMethodError`。
  这条约束由根 pom 的 `maven-enforcer-plugin` 钉在构建上。

## _frontend

`_frontend/src/tools/qa/` 是主 SPA（z-opc `bootstraps/z-opc-main-starter-frontend`）里 QA 页面的上游副本，
**不是可独立运行的前端工程**：它依赖宿主的 `@/` 别名、`request.ts` 与 SystemShell。
改这里不会生效，改完必须回流宿主 SPA。

## 发布到 Maven Central

```bash
cp ../z-schedule/.env .            # 或从 z-opc-foundation-lead/004_重要秘钥 取
cp -R ../z-schedule/.gnupg .
./deploy_maven_center.sh gpg-init  # 仅首次
./deploy_maven_center.sh publish
./deploy_maven_center.sh verify
```

版本号只有根 pom 的 `<revision>` 一处，子 pom 一律 `${revision}`，
`flatten-maven-plugin`（`oss` 模式）在发布时展开成 self-contained 的子 POM。

⚠️ 已发布的版本号永久占位，不可覆盖、不可删除。发布那一遍不要加 `-DskipTests`。
