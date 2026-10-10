# z-qa

> 步骤式 API 链路测试平台 —— 用例 / 套件 / 计划 / 环境 / 定时 / 执行 / 报告 / 质量看板（对标 TestRail · MeterSphere）

一人公司基座的**内建质量保障层**：把「一条业务链路该按什么顺序跑、每步期望什么、失败到底是谁的锅」
固化成数据（`z_qa_*` 12 张表）与一个可复用的执行引擎，让宿主应用 import 一个依赖就拿到全套 `/api/qa/**`
端点。它解决的核心问题是**失败定性**：改造前一个 HTTP 500 和一句业务断言不符都叫 `fail`，缺陷单直接开错方向；
现在十态状态机 + 缺陷三分类（系统 / 脚本 / 环境数据）在断言之前先问一次，报告与看板才有可行动的信号。

2026-09-28 从 z-opc monorepo 迁出为独立仓并发布到 Maven Central；迁出前的一期实现与二阶段蒸馏计划见
同级仓 `z-opc` 的 `_doc/001_feature/052_测试平台（z-qa）/feature052.md`（跨仓路径，不在本仓）。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-qa`（根 POM `packaging=pom`，聚合 2 个模块） |
| **Maven 坐标** | `io.github.yuku123:z-qa` / `:z-qa-core` / `:z-qa-web` |
| **当前版本** | `1.0.1`（根 POM `<revision>`，CI-friendly versions + `flatten-maven-plugin:1.7.3` 的 `oss` 模式；子 POM 一律 `${revision}`） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.0.21`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘；再往上是 `z-boot-dependencies:1.0.20` 地板 + `z-boot-fleet:1.0.1` 兄弟仓权威表） |
| **Maven Central** | 已实测发布：`z-qa` / `z-qa-core` / `z-qa-web` 的 `1.0.0` 与 `1.0.1` 三份 POM 从 repo1 取回均为 `200` |
| **默认端口** | 不适用 —— 本仓**没有** `application*.yml`／`.properties`，也没有 admin/bootstrap 启动模块，端口由宿主应用决定 |
| **运行口径** | Java 8（`java.version=1.8`、`maven.compiler.source/target=8`）· Spring Boot 2.7.18（版本由父链下发，本仓 POM 已删掉这些键） |
| **最近更新** | 2026-09-30 |

---

## 🎯 能力清单

每一条都能对应到 `z-qa-core/src/main/java/com/zifang/z/qa/admin/` 下的真实类：

| 能力 | 入口类 | 说明 |
|------|--------|------|
| 用例管理 | `controller/CaseController` | `list`（按 keyword/priority/category 过滤）、`add`、`update`、`remove`；缺省优先级 `P1` |
| 套件与步骤编排 | `controller/SuiteController` | 套件 CRUD + `steps` 读取 / `steps/save` 整表覆盖；套件级 `failFast` |
| 计划（一组套件） | `controller/PlanController` + `engine/QaRunRunner#parseSuiteIds` | `suite_ids` 存 JSON 数组；引用失效的套件会被跳过并 warn |
| 环境 | `controller/EnvController` + `domain/entity/QaEnvDO` | `baseUrl` + 变量；步骤相对路径靠它拼绝对 URL |
| 执行引擎 | `engine/QaExecutionDispatcher` · `QaRunRunner` · `ApiStepExecutor` · `QaFailureClassifier` · `QaStepStatus` | 建 run 行 → 异步跑步骤 → 十态定性 → 逐步落 `z_qa_run_detail` |
| 定时触发 | `controller/ScheduleController` + `service/impl/QaScheduleServiceImpl` | 自实现 5/6 位（unix / quartz 风格）cron `computeNextFire`，避免引 cron-utils；`@Scheduled(cron = "0 * * * * ?")` 每分钟扫描，另有 `toggle` / `scan` 手动接口 |
| 报告 | `controller/ReportController` + `service/impl/QaReportServiceImpl` | `html/{runCode}` 输出整页 HTML 报告，`chain/{runCode}` 输出链路 JSON |
| 质量看板 | `controller/DashboardController` | `stats` / `summary` / `trend` / `suite-health` / `top-failures`，聚合走 `QaRunMapper.countByStatus·sumPassed·sumTotal` 与 `QaRunDetailMapper.topFailures` |
| E2E 录制会话 | `controller/E2eController` + `service/impl/QaE2eServiceImpl` | 会话开关、动作逐条入库、同步执行单步（browserless `/function`），截图落 z-oss |
| 结果通知 | `service/impl/QaNotifyServiceImpl` | 站内信 + 邮件（`z-msg` 的 `MessageGateway`），渠道取自计划的 `notify_channels`，投递留痕在 `z_qa_notify_log` |
| 断言模板 | `domain/entity/QaAssertTemplateDO` + `QaAssertTemplateMapper` + `z_qa_assert_template` 表 | 目前只有表与 mapper，**尚无** 对应 Controller |
| 模块装配 | `z-qa-web`：`QaAutoConfiguration` · `QaModuleDataSource` · `META-INF/spring.factories` | 宿主无需 `@ComponentScan`；独立数据源 `dataSourceQa` + `sqlSessionFactoryQa` + `@MapperScan` |

---

## 🏗️ 项目结构

```
z-qa/
├── pom.xml                      # 根聚合 POM：parent=z-boot-parent:1.0.21，<revision> 统一版本，enforcer + flatten
├── LICENSE                      # Apache License 2.0 全文（见文末 License 一节的口径冲突）
├── _doc/003_script/deploy_maven_center.sh       # Central 发布脚本（gpg-init / publish / verify / readme / help）
├── z-qa-core/                   # 领域层 + 执行引擎 + 9 个 Controller + DDL（进 Maven Central）
│   └── src/main/
│       ├── java/com/zifang/z/qa/admin/
│       │   ├── config/          # QaAsyncConfig（@EnableAsync）
│       │   ├── controller/      # Case / Suite / Plan / Env / Schedule / Run / Report / Dashboard / E2e
│       │   ├── domain/entity/   # 12 个 Qa*DO（MyBatis-Plus）
│       │   ├── domain/mapper/   # 12 个 Qa*Mapper
│       │   ├── engine/          # Dispatcher / RunRunner / ApiStepExecutor / FailureClassifier / StepStatus
│       │   └── service/         # E2e · Notify · Report · Schedule（+ impl/）
│       └── resources/
│           ├── db/              # z_qa_ddl.sql · _patch.sql · _patch2.sql · z_qa_self_test.sql
│           └── mapper/          # QaRun / QaRunDetail / QaSuiteStep / QaE2eAction 四个 XML
├── z-qa-web/                    # 装配层：AutoConfiguration + 模块数据源 + spring.factories
│   └── src/main/
│       ├── java/com/zifang/z/qa/web/config/
│       └── resources/META-INF/spring.factories
└── _frontend/src/tools/qa/      # 主 SPA 里 QA 页面的上游副本（见文末 _frontend 一节）
```

两个模块**都参与发布**：POM 里没有 `maven.deploy.skip`，`reactor` 只有 `z-qa-core` + `z-qa-web` 两条
（根 POM `<modules>` 实测；`_frontend/` 不是 Maven 模块）。9 个 Controller 全在 `z-qa-core`，
`z-qa-web` 只有 2 个配置类 —— 历史上它们分层的意义是「领域+HTTP 实现」与「装配」，不是「库 vs 应用」。

---

## 🔧 技术栈

| 层级 | 技术（均为 POM / 源码实测） |
|------|------|
| 语言 / 运行时 | Java 8（口径由父链 `z-boot-dependencies:1.0.20` 下发） |
| 框架 | Spring Boot 2.7.18 · `spring-boot-starter-web`（`z-qa-web`） |
| 持久层 | MyBatis-Plus 3.5.7（地板）+ `mybatis-spring` 压回 **2.1.2**（本仓 DM 直写，防父链回压） |
| 数据源 | Druid，经 `z-boot-datasource-starter`（`${z-boot.version}`=1.0.21）的 `ModuleDataSourceTemplate` |
| 数据库 | MySQL，`mysql-connector-j` 压回 **8.0.33** 并沿用 Boot 受管项的 `protobuf-java` exclusion（8.4 换 driver 行为且整件拖进 protobuf） |
| HTTP 调用 | JDK `HttpURLConnection`（`ApiStepExecutor` / `QaE2eServiceImpl` 都是裸连，没有 RestTemplate/OkHttp） |
| JSON | `z-util-parser-json`（`JsonUtil` / `JsonObject`，替掉 fastjson）+ 自研 `evaluateJsonPath` 的 `$.a.b` 子集 |
| 通知 | `z-msg-core` + `z-boot-msg-starter`（`MessageGateway` / `EmailMessage` / 站内信 mapper） |
| 对象存储 | `z-oss-core`（`IOssObjectService`，E2E 截图上传；可选依赖，缺 Bean 时降级占位 key） |
| 日志 | Log4j2 2.25.4 + slf4j 1.7.36 单绑定链；`maven-enforcer-plugin:3.4.1` 在 `validate` 阶段禁掉 `log4j-to-slf4j`、`log4j-slf4j2-impl`、`spring-boot-starter-logging`、`ch.qos.logback:*`、`slf4j-log4j12`、`slf4j-simple` |
| 定时 | 自实现 cron 解析 + Spring `@Scheduled`（无 Quartz / cron-utils） |
| 测试 | JUnit Jupiter（`junit-jupiter-api` + `-engine` 都在，避免 surefire「0 个测试」静默通过） |
| 构建 / 发布 | Maven + `flatten-maven-plugin:1.7.3`(oss) · `central-publishing-maven-plugin:0.7.0` · source 3.3.1 / javadoc 3.11.2 / gpg 3.2.7（全部挂在 `central` profile） |
| 前端副本 | React + antd + react-router-dom（无 `package.json`，不是独立 npm 工程） |

---

## 🚀 快速开始

### 编译 / 装到本地仓

```bash
mvn clean install -DskipTests
```

第三方版本一律由 `z-boot-parent:1.0.21` → `z-boot-dependencies:1.0.20`（地板）+ `z-boot-fleet:1.0.1`（兄弟仓权威表）
供给，模块 POM 里不应再出现字面版本钉（`mybatis-spring` / `mysql-connector-j` 两格是**刻意**压住的例外，见根 POM 注释）。
若报找不到版本，先确认本地或镜像能解析到 `io.github.yuku123:z-boot-parent:1.0.21`。

### 宿主应用接入

本仓不是可 `java -jar` 启动的服务，接入方式是加依赖：

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-qa-web</artifactId>   <!-- 传递带上 z-qa-core -->
    <version>1.0.1</version>
</dependency>
```

`spring.factories` 注册 `QaAutoConfiguration`，它自己 `@ComponentScan` `com.zifang.z.qa.web` 与
`com.zifang.z.qa.admin`，宿主**不需要** `@ComponentScan`。两点实测前置条件：

- 定时扫描用的是 `@Scheduled`，而本仓只带 `@EnableAsync`（`QaAsyncConfig`）、**没有** `@EnableScheduling`
  → 宿主必须自己开启，否则只有 `POST /api/qa/schedules/scan` 手动兜底。
- 异步执行依赖 `@Async` 走代理：`QaExecutionDispatcher` 特意把循环交给独立 bean `QaRunRunner`，
  宿主若把这两者重新合回一个类，整条 run 会同步跑在 HTTP 请求线程上。

### 建表（MySQL 8）

DDL 随 jar 发布在 `z-qa-core` 的 `db/` 资源目录，**按顺序**执行：

| 文件 | 用途 |
|------|------|
| [`z_qa_ddl.sql`](z-qa-core/src/main/resources/db/z_qa_ddl.sql) | 8 张基线表：`z_qa_suite` / `z_qa_suite_step` / `z_qa_case` / `z_qa_plan` / `z_qa_run` / `z_qa_run_detail` / `z_qa_env` / `z_qa_assert_template`，全 `CREATE TABLE IF NOT EXISTS` 可重复执行 |
| [`z_qa_ddl_patch.sql`](z-qa-core/src/main/resources/db/z_qa_ddl_patch.sql) | Phase 3 补全的 4 张表：`z_qa_schedule` / `z_qa_e2e_session` / `z_qa_e2e_action` / `z_qa_notify_log`（合计 12 张） |
| [`z_qa_ddl_patch2.sql`](z-qa-core/src/main/resources/db/z_qa_ddl_patch2.sql) | 二阶段口径：补 `z_qa_run.env_code`、`abort_requested`、`z_qa_suite.fail_fast`、`z_qa_run_detail.{suite_id,defect_type,target_mode}`、`e2e_screenshot`，`result` 放宽到 `VARCHAR(32)` 装十态，加 3 条索引，把历史 `pass/fail/error` 迁移到新词表并回填 `defect_type`，最后 **DROP** `z_qa_run.step_total/step_pass/step_fail/step_skip`（计数改由 `z_qa_run_detail` 聚合）。⚠️ 用存储过程 + `DELIMITER`，必须走 `mysql` 客户端而非 JDBC 单语句执行 |
| [`z_qa_self_test.sql`](z-qa-core/src/main/resources/db/z_qa_self_test.sql) | 冒烟数据：`zopc-self-test` 套件共 **10 步** —— 健康检查 → 登录 → 我的租户 → 调度中心 jobinfo/jobgroup/joblog/dashboard → 应用管理 → 环境列表 → 套件列表。自带本地演示账号，**别拿它当真凭据** |

目标库名 `oc`（`patch2` 头注写明 MySQL 8.0.36、`sql_mode` 无 `ANSI_QUOTES`）。

### 配置键

z-qa 用独立数据源（Bean 名 `dataSourceQa` / `sqlSessionFactoryQa`），解析口径由
`ModuleDataSourceTemplate` 实现：`z.base.db.qa.*` → 缺省回落 `z.base.db.default.*` → 再回落硬编码默认值。
本仓没有任何 yml/properties，这些键由宿主提供（Spring 宽松绑定下可用同名大写下划线环境变量注入）。

| 配置键 | 实测缺省值 | 说明 |
|--------|-----------|------|
| `z.base.db.qa.host` / `.port` | `localhost` / `3306` | |
| `z.base.db.qa.database` | 空（须显式给出；DDL 面向 `oc`） | |
| `z.base.db.qa.username` / `.password` | `root` / 空 | **口令必须经环境变量或配置中心注入，禁止写进 yml/jar/镜像层** |
| `z.base.db.qa.initial-size` / `.min-idle` / `.max-active` / `.max-wait` | `5` / `5` / `20` / `60000` | Druid 连接池 |
| `z.base.db.qa.remove-abandoned` / `.remove-abandoned-timeout` | `false` / `300` | 显式开启才生效 |
| `z-qa.e2e.browserless-url` | `http://browserless:3000` | browserless 服务地址（`/function?--allow-unsigned`） |
| `z-qa.e2e.token` | 空 | 非空时以 `Authorization: Bearer` 发出；对应环境变量约定 `Z_QA_E2E_TOKEN` |
| `z-qa.e2e.bucket` / `z-qa.e2e.tenant-id` | `qa-e2e` / `1` | 截图落 z-oss 的桶与租户 |
| `z-qa.notify.email-receiver-suffix` / `z-qa.notify.biz-type` / `z-qa.notify.default-user-id` | `@example.com` / `z-qa-run-finished` / `1` | 通知渠道与收件人推导 |

---

## 🔌 API 一览

9 个 Controller、前缀全部实测自 `@RequestMapping`。**约定与直觉不同处请留意右侧说明**：

| 路径 | 方法 | 归属 |
|------|------|------|
| `/api/qa/case/list` | POST（请求体取 `keyword`/`priority`/`category`） | 用例 |
| `/api/qa/case/add`、`/update`（请求体 = `QaCaseDO`）、`/remove`（`?id=`） | POST | 用例 |
| `/api/qa/suite/list` | GET（可选 `keyword`、`category`） | 套件 |
| `/api/qa/suite/get`（`?id=`）、`/steps`（`?suiteId=`） | GET | 套件 |
| `/api/qa/suite/add`、`/update`、`/remove` | POST | 套件 |
| `/api/qa/suite/steps/save` | POST（`?suiteId=` + 请求体 `List<QaSuiteStepDO>`；先 `delete` 再逐条 `insert`，即**整表覆盖**） | 套件 |
| `/api/qa/plan/list`（无参，按 `gmt_create` 倒序）、`/get`（`?id=`） | GET | 计划 |
| `/api/qa/plan/add`、`/update`、`/remove` | POST | 计划 |
| `/api/qa/env/list`（无参，按 `priority` 倒序） | GET | 环境 |
| `/api/qa/env/add`、`/update`、`/remove` | POST | 环境 |
| `/api/qa/schedules/list`（无参）、`/get`（`?id=`） | GET | 定时 |
| `/api/qa/schedules/add`、`/update`、`/remove` | POST | 定时 |
| `/api/qa/schedules/toggle`（`?id=`+`?enabled=`）、`/scan`（无参，返回本次触发条数） | POST | 定时 |
| `/api/qa/run/list`（`status`、`limit`，缺省 50）、`/status`（`?runId=`）、`/details`（`?runId=`）、`/get`（`?id=`） | GET | 执行 |
| `/api/qa/run/trigger`（`?suiteId=`）、`/run/runPlan`（`?planId=`）、`/run/abort`（`?runId=`） | POST | 执行 |
| `/api/qa/runs/{runCode}/abort` | POST（路径变量，与 `/run/abort` 等价，按 `run_code` 定位） | 执行 |
| `/api/qa/report/html/{runCode}` | GET，`produces=text/html` 整页报告 | 报告 |
| `/api/qa/report/chain/{runCode}` | GET，链路 JSON | 报告 |
| `/api/qa/dashboard/stats`、`/summary`、`/trend`、`/suite-health`、`/top-failures` | GET | 看板 |
| `/api/qa/e2e/sessions/open`、`/sessions/{id}/finish`、`/actions/execute` | POST（请求体为 `Map`：`runId`/`browserType`/`targetUrl`、`status`/`videoOssKey`/`traceOssKey`/`harOssKey`、`sessionId`/`stepNo`/`actionType`/`selector`/`value`/`screenshot`） | E2E |
| `/api/qa/e2e/sessions/{id}`、`/sessions/{id}/actions` | GET | E2E |

响应信封是 `{ code, msg, content, success }` —— 载荷键是 **`content`**（不是 `data`），9 个 Controller 一致。
详情走 `?id=` 查询参数而非路径变量（`/suite/get`、`/plan/get`、`/schedules/get`、`/run/get`），`/run/trigger`
与 `/run/runPlan` 也取查询参数而非请求体。E2E 的动作类型词表（`z_qa_e2e_action` 注释实测）为
`navigate/click/fill/select/hover/wait/assert_text/assert_visible/screenshot`。

JSON 字段名全局是 **snake_case**，由宿主 Jackson 的 `PropertyNamingStrategy` 决定；**本模块不配 Jackson**，
仓内没有 `ObjectMapper`／`Jackson2ObjectMapperBuilderCustomizer` 配置类。三处手写 `Map` 的出口保持 camelCase：
`/api/qa/run/status`（`runId`/`totalSteps`/`passedSteps`/`failedSteps`/`abortRequested`）、`/api/qa/dashboard/*`、
`/api/qa/e2e/*`。鉴权也不在本仓：没有任何 Filter/Interceptor/Security 配置类，`/api/qa/**` 的
`Authorization: Bearer {jwt}` 由宿主（z-ctc SSO 拦截器）负责。

---

## ⚙️ 执行模型

```
POST /api/qa/run/trigger?suiteId=   或   POST /api/qa/run/runPlan?planId=   或   定时扫描命中
        └─ QaExecutionDispatcher.triggerSuite / triggerPlan
             建 z_qa_run 行：status=pending，run_code=RUN-<yyyyMMddHHmmssSSS>-<进程内序号>（避开 uk_run_code 撞车）
             └─ QaRunRunner.runSuiteAsync / runPlanAsync（@Async，独立 bean 才走代理）
                  └─ 步骤按「套件顺序 + step_no」展平成一条有序 PlannedStep 序列，整个 run 只有一个循环
                       ├─ 变量上下文按套件重置（上套提取的 ${TOKEN} 不会漏进下一套）
                       ├─ executeWithRetry → ApiStepExecutor.execute(step, env, ctx)
                       │     ${VAR} 替换（缺失即 fail_param_null）→ 相对路径拼 env.baseUrl
                       │     → HttpURLConnection 真实调用 → evaluateJsonPath 提取 → 断言（{"status":200} / {"$.code":200} / {"$.msg":"success"}）
                       └─ QaFailureClassifier 定性 → 写 z_qa_run_detail（含 suite_id / defect_type / 重试次数）
```

- **步骤十态**：`pending` `running` `success` `skip` `fail_assert` `fail_system_error`
  `fail_timeout` `fail_navigation` `fail_param_null` `fail_unknown`（全小写，与源平台的大写下划线口径不同）
- **缺陷三分类**：`system_defect`（被测系统）/ `script_defect`（脚本写法）/ `env_data_defect`（环境数据）
- **定性优先于断言**：`detectSystemError` 在断言之前问一次（HTTP ≥500、信封 `code` ≥500、4xx 且响应含
  `异常码`/`错误码`/`TID:`/`Traceback`/`StackOverflow` 标记），否则 500 会被顺带失败的断言掩盖成 `fail_assert`
- **retry**：只对 `isRetryable` 的定性重试（超时 / 连不上 / 5xx / unknown），重试前固定等 1s；断言不符与入参未解析
  再来一次也不会变，不重试
- **failFast**：套件级 —— 只跳过该套件剩余步骤（落 `skip`），计划里的后续套件照常执行
- **abort**：`POST /run/abort` 只把 `z_qa_run.abort_requested` 置 1（不抢写终态），runner 在**步骤边界轮询数据库**
  后才把剩余步骤落 `skip` 并把 run 翻成 `cancelled`；走 DB 而非内存 flag 是因为发起中止的实例未必是跑这条 run 的实例
- **run 终态**：`success` / `failed` / `cancelled`；已是终态时 abort 返回 `code=400`
- **进度**：`progress` = 已落库的明细行数，`totalSteps/passedSteps/failedSteps` 每步刷一次且**只写计数列**
  （整行回写会把内存快照里的 `abort_requested=0` 盖回库中）
- **环境解析**：`env_code` 在 `z_qa_env` 里找不到时回落 `code='local'` 那行；连 `local` 都没有时
  `ApiStepExecutor` 用硬编码兜底 baseUrl `http://127.0.0.1:8888`
- **响应体入库截断** 8000 字符，明细存请求头/体、响应状态/体、变量前后快照、提取值与断言结果

---

## 🧪 测试

```bash
mvn test
```

实测只有 1 个测试类：[`QaScheduleServiceImplTest`](z-qa-core/src/test/java/com/zifang/z/qa/admin/service/impl/QaScheduleServiceImplTest.java)
的 5 个 JUnit Jupiter 用例（`next_fire_5field` / `next_fire_6field_with_seconds` / `cron_step_every_5_minutes` /
`invalid_cron_returns_null` / `next_fire_always_after_now`），覆盖自实现 cron 的 `computeNextFire`。
用例直接调静态方法，**不建 Spring 上下文、不连 MySQL**，因此 `mvn test` 无外部依赖；但它只测 cron 计算这一格，
且 `mvn test` 仍需能解析到 `z-boot-parent:1.0.21`（离线且本地仓无 parent 时会先挂在依赖解析上）。
本次 README 更新按作业单只读 POM 与源码，未执行 `mvn`，故不宣称实测运行结果。

如实说明跑不了更多东西：`z-qa-web` 没有测试目录，执行引擎（`QaRunRunner` / `ApiStepExecutor`）、看板聚合、
通知投递都**没有**单元测试；这些链路要验证只能挂到宿主应用上，用
`z_qa_self_test.sql` 建出 `zopc-self-test` 套件再 `POST /api/qa/run/trigger`，看 `z_qa_run_detail` 与
`/api/qa/report/html/{runCode}`。发布那遍不要加 `-DskipTests`（见下一节）。

---

## 🔍 已知边界

- **步骤模式只有 `api`**。`mode=e2e` 的步骤没有对应执行器：`QaRunRunner.executeWithRetry` 直接返回 null，
  落成 `fail_unknown`（"步骤模式无对应执行器"）而不是拿空 URL 去发一次 HTTP 请求假装成功。
  Playwright over WebSocket 的 `E2eStepExecutor` 在计划里。
- **E2E 证据链**：单步截图已有真实落地（`QaE2eServiceImpl.uploadScreenshotToOss` 经 `IOssObjectService` 上传，
  宿主没接入 z-oss 时降级写占位 key），会话表的 `video_oss_key` / `trace_oss_key` / `har_oss_key` 由
  `POST /e2e/sessions/{id}/finish` 回写 —— 但**产物生成端仍缺**，仓内不会自己产生视频/Trace/HAR。
  步骤明细（`z_qa_run_detail`）的证据只有请求/响应/变量/断言四件套。
- **无内嵌启动器**：没有 admin/server 模块、没有 `application*.yml`，本仓产物只有两个 library jar。
- **定时扫描要靠宿主开启 `@EnableScheduling`**（本仓只带 `@EnableAsync`）。
- **日志绑定链唯一性钉在构建上**：`z-msg-core` 传递来的 `log4j-slf4j2-impl` 已被 `z-qa-core` POM 排掉，
  Spring Boot 2.7 的栈是 slf4j 1.7.36，两个 jar 共用 `org.apache.logging.slf4j` 包名，同时上 classpath
  会在启动时 `NoSuchMethodError`；这条约束由根 POM 的 `maven-enforcer-plugin` 在 `validate` 阶段兜住。
- **根 POM 顶注仍写「2026-09-29 起消费入口走 z-boot-parent:1.0.19」**，而 `<parent>` 实际已是 `1.0.21`
  （HEAD 提交 `ac4bc9d`「消费入口抬到 z-boot-parent:1.0.21（消费者轮）」）—— 读 POM 注释时以 `<parent>` 为准。

---

## 📦 发布到 Maven Central

本仓没有 Dockerfile / compose / k8s 资产，部署即发布 —— 产物是给宿主用的 jar。发布脚本按
`002_项目文档收口规范` 收口在 [`_doc/003_script/deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh)，
脚本自己从 `_doc/003_script/` 上跳两级定位仓库根并 `cd` 过去，所以在任意子目录调用都对：

```bash
bash _doc/003_script/deploy_maven_center.sh gpg-init   # 首次发布前生成 GPG 密钥并写 .env
bash _doc/003_script/deploy_maven_center.sh publish    # mvn -B deploy -Pcentral -U -Dmaven.legacyLocalRepo=true
bash _doc/003_script/deploy_maven_center.sh verify     # 约 30 分钟后，以 repo1 的 HEAD 状态码为准
bash _doc/003_script/deploy_maven_center.sh readme     # 完整指引摘要
bash _doc/003_script/deploy_maven_center.sh help
```

凭证**不在本仓**：脚本按 `./.env` → `../z-boot/.env` → `../z-schedule/.env` 顺序解析，`GNUPGHOME` 同样指向上游密钥环
（`.env` / `.gnupg/` 已被 `.gitignore` 排除；台账在 `z-opc-foundation-lead/004_重要秘钥`）。

两条实测坑：必须联网（`mvn -o deploy -Pcentral` 会把 central-publishing 的 publish 目标整场静默跳过却照样
BUILD SUCCESS，判「发出去了」只认 repo1 的状态码）；已发布版本号永久占位、不可覆盖不可删除。
`flatten-maven-plugin`（`oss` 模式）在 process-resources 阶段把 `${revision}` 展开成字面量并剥掉 `<parent>`，
所以入库件/发布件是 self-contained 的子 POM。

当前实测状态：`io.github.yuku123` 下 `z-qa`（pom）、`z-qa-core`、`z-qa-web` 的 **1.0.0 与 1.0.1** 均可从 repo1 取到
（1.0.1 来自 `ed5a00f`「抬版本 1.0.0 -> 1.0.1（1.0.19 批量发布）」）。

---

## 🖥️ _frontend

[`_frontend/src/tools/qa/`](./_frontend/src/tools/qa) 是主 SPA（z-opc `bootstraps/z-opc-main-starter-frontend`）里
QA 页面的**上游副本**，不是可独立运行的前端工程：实测该目录下**没有** `package.json`／构建配置，
`services/api.ts` 直接 `import request from '@/common/utils/request'`（依赖宿主的 `@/` 别名与 baseURL=`/api`），
页面用 React + antd + react-router-dom（`QAApp.tsx` + 10 个 page）。改这里不会生效，改完必须回流宿主 SPA。

---

## 📄 License

根 [`LICENSE`](LICENSE) 文件是 **Apache License 2.0** 全文，但根 POM 与两个子 POM 的 `<licenses>` 都声明
`MIT License`（`url` 指向 opensource.org/licenses/MIT）—— 两处口径**不一致**且尚未收口。
按仓库实际附带的许可证文本，应以 Apache-2.0 为准；如需统一，请先由 owner 定方向再改 POM 或换 LICENSE。

_Maintained by the z-opc-foundation organization._


## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) — Maven Central 发布（gpg-init / publish / verify / readme / help）

`001_arch` / `002_deploy` / `004_skill` 本仓**尚未建立**（实测 `_doc/` 下只有 `003_script/`）：
建表 DDL 目前随代码放在 `z-qa-core`（见「建表（MySQL 8）」一节），未单独收口为 `_doc/002_deploy/init.sql`。

<!-- icon: minimax image-01, gradient=#059669, glyph=qa -->
