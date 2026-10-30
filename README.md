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
| **仓库** | `z-qa`（根 POM `packaging=pom`，聚合 3 个模块：core / web / admin） |
| **Maven 坐标** | `io.github.yuku123:z-qa` / `:z-qa-core` / `:z-qa-web`（`:z-qa-admin` 只在仓内，不入 Central） |
| **当前版本** | `1.1.0`（根 POM `<revision>`，CI-friendly versions + `flatten-maven-plugin:1.7.3` 的 `oss` 模式；子 POM 一律 `${revision}`） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.0`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘；第三方地板与兄弟仓权威表均已并入 parent 自身） |
| **Maven Central** | 已实测发布：`z-qa` / `z-qa-core` / `z-qa-web` 的 `1.0.0` 与 `1.0.1` 三份 POM 从 repo1 取回均为 `200`。⚠ `1.0.1` 的 `z-qa-web` 是 Controller 迁移前的字节（零 Controller），`1.1.0` 待发布 |
| **默认端口** | `8083` —— `z-qa-admin/src/main/resources/application.yml`（可执行管理端）；被当库件引时端口由宿主应用决定 |
| **运行口径** | Java 8（`java.version=1.8`、`maven.compiler.source/target=8`）· Spring Boot 2.7.18（版本由父链下发，本仓 POM 已删掉这些键） |
| **最近更新** | 2026-10-19 |

---

## 🎯 能力清单

每一条都能对应到 `z-qa-core/src/main/java/com/zifang/z/qa/admin/` 下的真实类：

| 能力 | 入口类 | 说明 |
|------|--------|------|
| 用例管理 | `web/api/CaseApi` | `list`（按 keyword/priority/category 过滤）、`add`、`update`、`remove`；缺省优先级 `P1` |
| 套件与步骤编排 | `web/api/SuiteApi` | 套件 CRUD + `steps` 读取 / `steps/save` 整表覆盖；套件级 `failFast` |
| 计划（一组套件） | `web/api/PlanApi` + `engine/QaRunRunner#parseSuiteIds` | `suite_ids` 存 JSON 数组；引用失效的套件会被跳过并 warn |
| 环境 | `web/api/EnvApi` + `domain/entity/QaEnvDO` | `baseUrl` + 变量；步骤相对路径靠它拼绝对 URL |
| 执行引擎 | `engine/QaExecutionDispatcher` · `QaRunRunner` · `ApiStepExecutor` · `QaFailureClassifier` · `QaStepStatus` | 建 run 行 → 异步跑步骤 → 十态定性 → 逐步落 `z_qa_run_detail` |
| 定时触发 | `web/api/ScheduleApi` + `service/impl/QaScheduleServiceImpl` | 自实现 5/6 位（unix / quartz 风格）cron `computeNextFire`，避免引 cron-utils；`@Scheduled(cron = "0 * * * * ?")` 每分钟扫描，另有 `toggle` / `scan` 手动接口 |
| 报告 | `web/api/ReportApi` + `service/impl/QaReportServiceImpl` | `html/{runCode}` 输出整页 HTML 报告，`chain/{runCode}` 输出链路 JSON |
| 质量看板 | `web/api/DashboardApi` | `stats` / `summary` / `trend` / `suite-health` / `top-failures`，聚合走 `QaRunMapper.countByStatus·sumPassed·sumTotal` 与 `QaRunDetailMapper.topFailures` |
| E2E 录制会话 | `web/api/E2eApi` + `service/impl/QaE2eServiceImpl` | 会话开关、动作逐条入库、同步执行单步（browserless `/function`），截图落 z-oss |
| 结果通知 | `service/impl/QaNotifyServiceImpl` | 站内信 + 邮件（`z-msg` 的 `MessageGateway`），渠道取自计划的 `notify_channels`，投递留痕在 `z_qa_notify_log` |
| 断言模板 | `domain/entity/QaAssertTemplateDO` + `QaAssertTemplateMapper` + `z_qa_assert_template` 表 | 目前只有表与 mapper，**尚无** 对应 API |
| 模块装配 | `z-qa-web`：`QaAutoConfiguration` · `QaModuleDataSource` · `META-INF/spring.factories` | 宿主无需 `@ComponentScan`；独立数据源 `dataSourceQa` + `sqlSessionFactoryQa` + `@MapperScan` |

---

## 🏗️ 项目结构

```
z-qa/
├── pom.xml                      # 根聚合 POM：parent=z-boot-parent:1.2.0，<revision> 统一版本，enforcer + flatten
├── LICENSE                      # Apache License 2.0 全文（见文末 License 一节的口径冲突）
├── Dockerfile                   # 基于 eclipse-temurin:8-jdk，拷 z-qa-admin 的 fat jar，容器内 8080
├── _doc/
│   ├── 001_arch/                # 00-overview · 01-module-structure · 02-api · 03-db-schema
│   ├── 002_deploy/init/schema.sql   # 12 张表的完整 DDL（单一入口，可重复执行）
│   ├── 003_script/deploy_maven_center.sh  # Central 发布脚本（gpg-init / publish / verify / readme / help）
│   └── 005_testing/self_test.sql      # zopc-self-test 冒烟套件种子数据
├── z-qa-core/                   # 领域层 + 执行引擎 + entity/mapper/service（进 Maven Central）
│   └── src/main/
│       ├── java/com/zifang/z/qa/admin/
│       │   ├── config/          # QaAsyncConfig（@EnableAsync）
│       │   ├── domain/entity/   # 12 个 Qa*DO（MyBatis-Plus）
│       │   ├── domain/mapper/   # 12 个 Qa*Mapper
│       │   ├── engine/          # Dispatcher / RunRunner / ApiStepExecutor / FailureClassifier / StepStatus
│       │   └── service/         # E2e · Notify · Report · Schedule（+ impl/）
│       └── resources/mapper/    # QaRun / QaRunDetail / QaSuiteStep / QaE2eAction 四个 XML
├── z-qa-web/                    # API 层：request/response DTO + api/*Api + 数据源装配 + spring.factories（进 Maven Central）
│   └── src/main/
│       ├── java/com/zifang/z/qa/web/{api,request,response,config}/
│       └── resources/META-INF/spring.factories
├── z-qa-admin/                  # 可执行管理端：SpringApplication 主类 + SPA 回落 + 前端打进 fat jar（不入 Central）
└── _frontend/
    ├── z-qa-component/          # 可复用页面集 + DOC.md 组件文档
    └── z-qa-suit/               # 本域 SPA 壳（路由表 /z-qa/**）
```

三个模块里 **`z-qa-core` 与 `z-qa-web` 参与发布**，`z-qa-admin` 被根 POM `central` profile 的
`excludeArtifacts` 点名排除（它是 `repackage` 无 classifier 的执行 fat jar，不是别仓可引的库件；
`_frontend/` 同样不是 Maven 模块）。

分层口径：`z-qa-core` 是领域与引擎（DO / Mapper / Service / Executor），`z-qa-web` 是 API 层
（`request/*Req` + `response/*Resp` + `api/*Api`，**DO 不出 Web 层**，lead 007 §6），
`z-qa-admin` 是带主类的可执行应用。

---

## 🔧 技术栈

| 层级 | 技术（均为 POM / 源码实测） |
|------|------|
| 语言 / 运行时 | Java 8（口径由父链 `z-boot-parent` 下发） |
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
mvn clean install -DskipTests      # 只装库件（core / web）
mvn clean package                   # 完整构建：含前端，产出 z-qa-admin fat jar
```

`mvn package` 会在 `_frontend/z-qa-suit` 下装 Node v22.14.0 并跑 `npm install
--registry=https://registry.npmmirror.com` + `npm run build`（不走 npmjs.org，理由见该 pom 注释），
再把 `dist/` 复制进 `target/classes/static/`。

第三方版本一律由 `z-boot-parent:1.2.0` 下发（第三方地板与兄弟仓权威表均已并进 parent 自身，
`z-boot-dependencies` / `z-boot-fleet` 两个文件夹随 2026-10-08 火车制拓扑退役），
模块 POM 里不应再出现字面版本钉（`mybatis-spring` / `mysql-connector-j` 两格是**刻意**压住的例外，见根 POM 注释）。
若报找不到版本，先确认本地或镜像能解析到 `io.github.yuku123:z-boot-parent:1.2.0`。

### 宿主应用接入

**方式 A：当库件引**（给别的宿主用），加依赖即可：

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-qa-web</artifactId>   <!-- 传递带上 z-qa-core -->
    <version>1.1.0</version>
</dependency>
```

`spring.factories` 在 `EnableAutoConfiguration` 键下用 **FQCN** 逐个注册全部业务 bean
（`QaModuleDataSource` / `QaAsyncConfig` / 3 个 engine / 4 个 ServiceImpl / 9 个 `*Api`），
**刻意不用 `@ComponentScan`** —— 宿主若也扫到本包会双注册，同类型 bean 出现两份，
直接 `NoUniqueBeanDefinitionException`。宿主因此**不需要**（也不应该）扫本包。

**方式 B：独立跑**（本仓自带管理台）：

```bash
mvn clean package
java -jar z-qa-admin/target/z-qa-admin-1.1.0.jar
# 监听 8083；前端在 /z-qa/**，接口在 /api/qa/**
```

数据库连接走 `ZQA_DB_HOST` / `ZQA_DB_PORT` / `ZQA_DB_NAME` / `ZQA_DB_USER` / `ZQA_DB_PASSWORD`
（对应 `z.base.db.qa.*`，仓库内不落明文口令）。

无论哪种方式，两点前置条件不变：

- 定时扫描用的是 `@Scheduled`，而本仓只带 `@EnableAsync`（`QaAsyncConfig`）、**没有** `@EnableScheduling`
  → 宿主必须自己开启，否则只有 `POST /api/qa/schedules/scan` 手动兜底。
- 异步执行依赖 `@Async` 走代理：`QaExecutionDispatcher` 特意把循环交给独立 bean `QaRunRunner`，
  宿主若把这两者重新合回一个类，整条 run 会同步跑在 HTTP 请求线程上。

### 建表（MySQL 8）

DDL 已收口为 `_doc/002_deploy/init/schema.sql` **单一入口**，按第 1/2/3 段顺序执行即可（每段保留了
原阶段注释，便于定位某一列是谁加的）：

| 段 | 内容 |
|----|------|
| 第 1 段 | 8 张基线表：`z_qa_suite` / `z_qa_suite_step` / `z_qa_case` / `z_qa_plan` / `z_qa_run` / `z_qa_run_detail` / `z_qa_env` / `z_qa_assert_template`，全 `CREATE TABLE IF NOT EXISTS` 可重复执行 |
| 第 2 段 | Phase 3 补全的 4 张表：`z_qa_schedule` / `z_qa_e2e_session` / `z_qa_e2e_action` / `z_qa_notify_log`（合计 12 张） |
| 第 3 段 | 二阶段口径：补 `z_qa_run.env_code`、`abort_requested`、`z_qa_suite.fail_fast`、`z_qa_run_detail.{suite_id,defect_type,target_mode}`、`e2e_screenshot`，`result` 放宽到 `VARCHAR(32)` 装十态，加 3 条索引，把历史 `pass/fail/error` 迁移到新词表并回填 `defect_type`，最后 **DROP** `z_qa_run.step_total/step_pass/step_fail/step_skip`（计数改由 `z_qa_run_detail` 聚合）。⚠️ 用存储过程 + `DELIMITER`，必须走 `mysql` 客户端而非 JDBC 单语句执行 |

目标库名 `oc`（第 3 段头注写明 MySQL 8.0.36、`sql_mode` 无 `ANSI_QUOTES`）。

冒烟数据另置 [`_doc/005_testing/self_test.sql`](_doc/005_testing/self_test.sql)：`zopc-self-test` 套件共
**10 步** —— 健康检查 → 登录 → 我的租户 → 调度中心 jobinfo/jobgroup/joblog/dashboard → 应用管理 →
环境列表 → 套件列表。第 2 步登录的凭据在文件里是占位串，执行前替换成你自己的账号（仓库内不落明文凭据）。

### 配置键

z-qa 用独立数据源（Bean 名 `dataSourceQa` / `sqlSessionFactoryQa`），解析口径由
`ModuleDataSourceTemplate` 实现：`z.base.db.qa.*` → 缺省回落 `z.base.db.default.*` → 再回落硬编码默认值。
`z-qa-admin` 自带一份 `application.yml`（含上述键，全部走 `ZQA_DB_*` 环境变量）；
把 z-qa 当库件引时这些键由宿主提供（Spring 宽松绑定下可用同名大写下划线环境变量注入）。

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

9 个 API 类、前缀全部实测自 `@RequestMapping`。**约定与直觉不同处请留意右侧说明**：

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

响应信封是 `{ code, msg, content, success }` —— 载荷键是 **`content`**（不是 `data`），由 `web/response/QaResult` 统一产出，9 个 API 一致。入参一律 `request/*Req`、出参一律 `response/*Resp`，**DO 不出 Web 层**。
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

`z-qa-core` 下 4 个测试类共 **36 个用例**，`mvn -pl z-qa-core test` 实测全绿：

| 测试类 | 用例 | 手法 |
|--------|------|------|
| [`QaScheduleServiceImplTest`](z-qa-core/src/test/java/com/zifang/z/qa/admin/service/impl/QaScheduleServiceImplTest.java) | 5 | 纯函数：自实现 cron 的 `computeNextFire`（5 位 / 6 位 / 步长 / 非法 / 恒在未来） |
| [`QaFailureClassifierTest`](z-qa-core/src/test/java/com/zifang/z/qa/admin/engine/QaFailureClassifierTest.java) | 15 | 纯函数：异常分类、系统错误优先于断言、缺陷三分类映射、可重试判定 |
| [`ApiStepExecutorTest`](z-qa-core/src/test/java/com/zifang/z/qa/admin/engine/ApiStepExecutorTest.java) | 7 | 对 JDK 内置 `HttpServer` 打**真实 socket**（不打桩连接，否则等于把被测逻辑一起 stub 掉）：相对路径拼接、断言通过/失败、5xx 抢在断言之前定性、变量缺失不发请求、提取变量喂给下一步、HTML 响应、连不上判 `fail_navigation` |
| [`QaRunRunnerTest`](z-qa-core/src/test/java/com/zifang/z/qa/admin/engine/QaRunRunnerTest.java) | 9 | Mapper 走 **JDK 动态代理** + 反射注入（零 Mockito、零 `@SpringBootTest`）：failFast 粒度是套件不是整轮、abort 走 DB 标志、计数 update 不带其它列、失效套件被跳过、无可执行套件不挂在 `running`、e2e 步骤不降级、明细带 `defect_type`、套件之间变量上下文重置 |

测试不建 Spring 上下文、不连 MySQL，因此无外部依赖；但仍需能解析到 `z-boot-parent`
（离线且本地仓无 parent 时会先挂在依赖解析上）。
发布那遍不要加 `-DskipTests`（见下一节）。

---

## 🔍 已知边界

- **步骤模式只有 `api`**。`mode=e2e` 的步骤没有对应执行器：`QaRunRunner.executeWithRetry` 直接返回 null，
  落成 `fail_unknown`（"步骤模式无对应执行器"）而不是拿空 URL 去发一次 HTTP 请求假装成功。
  Playwright over WebSocket 的 `E2eStepExecutor` 在计划里。
- **E2E 证据链**：单步截图已有真实落地（`QaE2eServiceImpl.uploadScreenshotToOss` 经 `IOssObjectService` 上传，
  宿主没接入 z-oss 时降级写占位 key），会话表的 `video_oss_key` / `trace_oss_key` / `har_oss_key` 由
  `POST /e2e/sessions/{id}/finish` 回写 —— 但**产物生成端仍缺**，仓内不会自己产生视频/Trace/HAR。
  步骤明细（`z_qa_run_detail`）的证据只有请求/响应/变量/断言四件套。
- **库件与应用分离**：`z-qa-core` / `z-qa-web` 是给别仓引的 library jar；可执行应用是 `z-qa-admin`（带 `ZQaAdminApplication` 主类与 `application.yml`，repackage 成 fat jar，容器内 8080 / 宿主 8083），按政策不入 Central bundle。
- **定时扫描要靠宿主开启 `@EnableScheduling`**（本仓只带 `@EnableAsync`）。
- **日志绑定链唯一性钉在构建上**：`z-msg-core` 传递来的 `log4j-slf4j2-impl` 已被 `z-qa-core` POM 排掉，
  Spring Boot 2.7 的栈是 slf4j 1.7.36，两个 jar 共用 `org.apache.logging.slf4j` 包名，同时上 classpath
  会在启动时 `NoSuchMethodError`；这条约束由根 POM 的 `maven-enforcer-plugin` 在 `validate` 阶段兜住。
- **根 POM 顶注仍写「2026-09-29 起消费入口走 z-boot-parent:1.0.19」**，而 `<parent>` 实际已是 `1.1.0`
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
（1.0.1 来自 `ed5a00f`「抬版本 1.0.0 -> 1.0.1（1.0.19 批量发布）」；它仍是 Controller 迁移前的字节）。
**1.1.0 尚未发布**：Controller 迁到 `z-qa-web` 的重构（`5cd5dcf`）发生在 1.0.1 发布之后，中央那份装进宿主后
`/api/qa/**` 依然全 404 ⇒ 消费方必须等 1.1.0 发上 Central 后才改用新坐标。

---

## 🖥️ _frontend

本仓前端已按 lead 005 §9 拆成**组件 + 独立壳（suit）**两件：

- [`_frontend/z-qa-component/`](_frontend/z-qa-component/) — 可复用页面集（npm `@yuku123/z-qa-component`）：
  `src/pages/` 下 10 个 page + `QAApp.tsx` + `services/api.ts` + **`src/DOC.md`**（组件文档，
  由 `ComponentDoc.tsx` 以 `?raw` 编译进产物，页面上 `/z-qa/docs` 直达，不打任何后端接口）。
  `services/api.ts` **自洽**（自带 `configureQa({ apiBase })`，默认 baseURL=`/api`），
  响应类型 `QaResponse<T>` 与后端 `QaResult` 逐字段对齐。发布走双入口
  （`.` → `dist/index.js`、`./pages` → `dist/pages.js`），d.ts 由 `vite-plugin-dts` 的
  `rollupTypes` 打成单文件落在 `dist/` 根 —— 否则 `package.json` 的 `types` 会指向不存在的路径。
- [`_frontend/z-qa-suit/`](_frontend/z-qa-suit/) — 独立运行壳（`vite` dev/build + `tsc --noEmit` typecheck），
  装配 component 并挂路由。路由表用绝对路径 `/z-qa/**`，`BASE_PATH` 由 `import.meta.env.BASE_URL`
  自动带前缀（挂在子路径不必逐条改 path）。依赖走 `^0.1.2` 版本号，**不用** `file:` 协议
  （§8.5 禁止把本地路径写进 git）。

列表页按 §2 四件套对齐：分页、`Input.Search`（客户端过滤）、`EmptyState`（来自
`@yuku123/z-frontend-common/ui`，**不是** antd）、`type="link"` 动作按钮；每处删除都带
`Popconfirm`（title + description + danger 主按钮），不裸删。

页面用 React + antd + react-router-dom。宿主接入方式：起一个 `/api` 反代到本域后端即可。

---

## 📄 License

根 [`LICENSE`](LICENSE) 文件是 **Apache License 2.0** 全文，但根 POM 与两个子 POM 的 `<licenses>` 都声明
`MIT License`（`url` 指向 opensource.org/licenses/MIT）—— 两处口径**不一致**且尚未收口。
按仓库实际附带的许可证文本，应以 Apache-2.0 为准；如需统一，请先由 owner 定方向再改 POM 或换 LICENSE。

_Maintained by the z-opc-foundation organization._


## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档:
  - [`00-overview.md`](_doc/001_arch/00-overview.md) — 总览、执行模型四条语义、已知风险登记表
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md) — 坐标、目录树、分层规则、测试矩阵
  - [`02-api.md`](_doc/001_arch/02-api.md) — 响应信封、DTO 边界、端点清单、十态状态机、断言与变量
  - [`03-db-schema.md`](_doc/001_arch/03-db-schema.md) — 12 张表、关键列、历史踩坑、被 DROP 的列
- [`_doc/002_deploy/init/schema.sql`](_doc/002_deploy/init/schema.sql) — 建表 DDL 单一入口（12 张表）
- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) — Maven Central 发布（gpg-init / publish / verify / readme / help）
- [`_doc/005_testing/self_test.sql`](_doc/005_testing/self_test.sql) — `zopc-self-test` 冒烟套件种子（凭据为占位串）

`004_skill` 本仓尚未建立。前端组件文档另见
[`_frontend/z-qa-component/src/DOC.md`](_frontend/z-qa-component/src/DOC.md)（页面上 `/z-qa/docs` 可直达）。

<!-- icon: minimax image-01, gradient=#059669, glyph=qa -->
