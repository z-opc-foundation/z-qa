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
| **当前版本** | `1.1.1`（根 POM `<revision>`，CI-friendly versions + `flatten-maven-plugin:1.7.3` 的 `oss` 模式；子 POM 一律 `${revision}`） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.4`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘；第三方地板与兄弟仓权威表均已并入 parent 自身） |
| **Maven Central** | 已实测发布：`z-qa` / `z-qa-core` / `z-qa-web` 的 `1.0.0` / `1.0.1` / `1.1.1` 三组 POM 从 repo1 取回均为 `200`。⚠ `1.0.1` 的 `z-qa-web` 是 Controller 迁移前的字节（零 Controller）；`1.1.1` 起含 Controller（`z-qa-web` 有 19 条 FQCN 登记在 `AutoConfiguration.imports`） |
| **默认端口** | `8083` —— `z-qa-admin/src/main/resources/application.yml`（可执行管理端）；被当库件引时端口由宿主应用决定 |
| **运行口径** | Java 17（`maven.compiler.source/target=17`，由父链 `z-boot-parent:1.2.4` 下发）· Spring Boot 3.3.13（版本由父链下发，本仓 POM 已删掉这些键）。⚠ 2026-10-30 前的旧口径（Java 8 / Boot 2.7.18）已随 SB3 火车作废 |
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
├── pom.xml                      # 根聚合 POM：parent=z-boot-parent:1.2.4，<revision> 统一版本，enforcer + flatten
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
| 语言 / 运行时 | Java 17（口径由父链 `z-boot-parent:1.2.4` 下发） |
| 框架 | Spring Boot 3.3.13 · `spring-boot-starter-web`（`z-qa-web`） |
| 持久层 | MyBatis-Plus 3.5.7（`mybatis-plus-spring-boot3-starter`，Boot 3 专用 starter） |
| 数据源 | Druid 1.2.23（`druid-spring-boot-3-starter`），经 `z-boot-datasource-starter` 的 `ModuleDataSourceTemplate` |
| 数据库 | MySQL，`mysql-connector-j` 压回 **8.0.33** 并沿用 Boot 受管项的 `protobuf-java` exclusion（8.4 换 driver 行为且整件拖进 protobuf） |
| HTTP 调用 | JDK `HttpURLConnection`（`ApiStepExecutor` / `QaE2eServiceImpl` 都是裸连，没有 RestTemplate/OkHttp） |
| JSON | `z-util-parser-json`（`JsonUtil` / `JsonObject`，替掉 fastjson）+ 自研 `evaluateJsonPath` 的 `$.a.b` 子集 |
| 通知 | `z-msg-core` + `z-boot-msg-starter`（`MessageGateway` / `EmailMessage` / 站内信 mapper） |
| 对象存储 | `z-oss-core`（`IOssObjectService`，E2E 截图上传；可选依赖，缺 Bean 时降级占位 key） |
| 日志 | Log4j2 2.25.4 + slf4j **2.0.17** 单绑定链（Boot 3 起 slf4j 2.x，`log4j-slf4j2-impl` 是**正规绑定**不再被禁）；`maven-enforcer-plugin:3.4.1` 在 `validate` 阶段禁掉 `log4j-to-slf4j`、`spring-boot-starter-logging`、`ch.qos.logback:*`、`slf4j-log4j12`、`slf4j-simple` |
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

第三方版本一律由 `z-boot-parent:1.2.4` 下发（第三方地板与兄弟仓权威表均已并进 parent 自身，
`z-boot-dependencies` / `z-boot-fleet` 两个文件夹随 2026-10-08 火车制拓扑退役），
模块 POM 里不应再出现字面版本钉（`mybatis-spring` / `mysql-connector-j` 两格是**刻意**压住的例外，见根 POM 注释）。
若报找不到版本，先确认本地或镜像能解析到 `io.github.yuku123:z-boot-parent:1.2.4`。

### 宿主应用接入

**方式 A：当库件引**（给别的宿主用），加依赖即可：

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-qa-web</artifactId>   <!-- 传递带上 z-qa-core -->
    <version>1.1.1</version>
</dependency>
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 用 **FQCN** 逐个注册全部业务 bean
（`QaModuleDataSource` / `QaAsyncConfig` / 3 个 engine / 4 个 ServiceImpl / 9 个 `*Api`），
**刻意不用 `@ComponentScan`** —— 宿主若也扫到本包会双注册，同类型 bean 出现两份，
直接 `NoUniqueBeanDefinitionException`。宿主因此**不需要**（也不应该）扫本包。

> ⚠ **Boot 3 迁移（2026-10-30）**：注册入口从 `spring.factories` 的 `EnableAutoConfiguration` 键
> 迁到 `AutoConfiguration.imports` —— **Boot 3 不再读 `spring.factories` 的该键**。
> 只带 `spring.factories` 的 jar 在 Boot 3 里等于**完全不装配**，且启动日志一片安静（静默失效，不是报错）。
> 消费方若见到下游 `NoSuchBeanDefinitionException`，先查这份 imports 是否登记齐全。

**方式 B：独立跑**（本仓自带管理台）：

```bash
mvn clean package
java -jar z-qa-admin/target/z-qa-admin-1.1.1.jar
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
- **日志绑定链唯一性钉在构建上**：`z-msg-core` 传递来的 `log4j-slf4j2-impl` 与 Boot 3 自带的
  `spring-boot-starter-log4j2` 会带来**两份** Log4j2 绑定；Boot 3 的栈是 slf4j **2.0.17**，
  两个 jar 共用 `org.apache.logging.slf4j` 包名，同时上 classpath 会在启动时 `NoSuchMethodError`；
  这条约束由根 POM 的 `maven-enforcer-plugin` 在 `validate` 阶段兜住。
- **根 POM 顶注仍写「2026-09-29 起消费入口走 z-boot-parent:1.2.4」**，而 `<parent>` 实际已是 `1.2.0`
  （2026-10-30 SB3 火车）—— 读 POM 注释时以 `<parent>` 为准。

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

当前实测状态：`io.github.yuku123` 下 `z-qa`（pom）、`z-qa-core`、`z-qa-web` 的 **1.0.0 / 1.0.1 / 1.1.1** 均可从 repo1 取到
（1.0.1 来自 `ed5a00f`「抬版本 1.0.0 -> 1.0.1（1.0.19 批量发布）」；它仍是 Controller 迁移前的字节）。
**1.1.1 已发布**（2026-10-30，commit `09fef303`）：Controller 迁到 `z-qa-web` 的重构（`5cd5dcf`）+
Boot 3 的 `AutoConfiguration.imports` 补登 19 条 FQCN 都在这一版里 ⇒ 消费方改用 `1.1.1` 即可拿到 `/api/qa/**`。
⚠ `1.1.0` 被 Central 占位跳号（发布流程检测到已占用），中央没有这份版本，消费方不要写它。

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

---

<!-- z-opc-foundation-sibling-registry:v1 -->
## 🔗 兄弟仓全景（z-opc-foundation 组织）

本仓隶属于 [z-opc-foundation](https://github.com/orgs/z-opc-foundation/repositories) 组织——一人公司基座的全部自研件（现存 61 仓，含私有仓；口径 2026-11-24 GitHub org API 实读）。

| 仓 | 一句话说明 |
|---|---|
| [z-agent](https://github.com/z-opc-foundation/z-agent) | 云端 Agent 平台的库与 starter——ReAct/Plan 实现 + 注册中心 + REST 网关 + 只读控制面 |
| [z-agent-kernel](https://github.com/z-opc-foundation/z-agent-kernel) | Agent 框架基座——消息/工具/记忆/provider/会话 SPI + 轻量默认实现（纯 POJO 无 Spring） |
| [z-asset](https://github.com/z-opc-foundation/z-asset) | 企业资产台账——实物设备全生命周期 + 域名/证书/许可到期提醒 |
| [z-audit](https://github.com/z-opc-foundation/z-audit) | 审计与合规留痕——事件模型 + 哈希链 + 事件出口 SPI + 留存期策略 |
| [z-biz](https://github.com/z-opc-foundation/z-biz) | 商务外壳域——客户/合同/票据/对账四聚合 |
| [z-biz-erp](https://github.com/z-opc-foundation/z-biz-erp) | 进销存 ERP——商品/客商/采购/销售/库存流水/应收应付核销（z-biz 业务线） |
| [z-biz-security](https://github.com/z-opc-foundation/z-biz-security) | 业务数据安全防护入口——场景化策略注册 + 安全线四段流水线对外编排 |
| [z-boot](https://github.com/z-opc-foundation/z-boot) | Spring Boot Starter 聚合仓 + 版本权威（火车制）——唯一消费入口 z-boot-parent |
| [z-bot](https://github.com/z-opc-foundation/z-bot) | 本地 agent 应用——终端 CLI + HTTP/SSE web 控制台 + 多通道 gateway（飞书/钉钉/webhook） |
| [z-cache](https://github.com/z-opc-foundation/z-cache) | RESP2 协议兼容的分布式内存缓存——Java 8 + Netty，Redis 客户端零修改直连 |
| [z-camuda](https://github.com/z-opc-foundation/z-camuda) | 【已退役·归档】基于 Camunda 的工作流封装层（原 z-wf 改名，与现行 z-wf 无共同历史） |
| [z-cognition](https://github.com/z-opc-foundation/z-cognition) | 业务认知域/认知舱——术语/概念/证据链 + memory/kb/ontology 渐进式模块 |
| [z-config](https://github.com/z-opc-foundation/z-config) | 分布式配置中心 + 服务注册中心（类 Nacos）——Netty 长连接推送 + React 控制台 |
| [z-ctc](https://github.com/z-opc-foundation/z-ctc) | 4A 中心——认证/授权/账户/审计，多租户 JWT + SSO 跨子域 |
| [z-dms](https://github.com/z-opc-foundation/z-dms) | 数据源管控底座——统一登记、统一建池、统一探活、变更统一推送 |
| [z-env](https://github.com/z-opc-foundation/z-env) | 本地中间件与基础设施环境——31 个 docker-compose 场景 + make 一键运维 |
| [z-etl](https://github.com/z-opc-foundation/z-etl) | ETL 引擎——DataX 式线性作业 + Flink 式 DAG 流管道（键控状态/水位/窗口） |
| [z-ext](https://github.com/z-opc-foundation/z-ext) | Java 扩展点框架——@ExtPoint/@ExtImpl 运行期选实现，五档侵入方式 |
| [z-frontend-common](https://github.com/z-opc-foundation/z-frontend-common) | 全仓共享前端基座——AppLayout/LoginPage 等前端规范的唯一承载 |
| [z-frontend-markdown](https://github.com/z-opc-foundation/z-frontend-markdown) | Markdown 通用共享组件包（npm）——MarkdownLiveEditor/NoteMarkdown/MermaidBlock 单一真源 |
| [z-frontend-render](https://github.com/z-opc-foundation/z-frontend-render) | 平台共享前端渲染引擎——后端 schema 是唯一事实源，报表与低代码共用 |
| [z-graph](https://github.com/z-opc-foundation/z-graph) | 独立图数据库——MVCC 版本链存储 + OpenCypher 子集 + Bolt 风格协议 + React 控制台 |
| [z-gw](https://github.com/z-opc-foundation/z-gw) | 自研高性能 API 网关——Netty 手写运行时，Route/Predicate/Filter 模型可嵌 Spring Boot |
| [z-indexer](https://github.com/z-opc-foundation/z-indexer) | 代码索引服务——仓库注册表 + 定时重建索引 + 符号/调用链查询 |
| [z-introduce](https://github.com/z-opc-foundation/z-introduce) | 组织仓库展示站——全仓实测元数据汇总成可搜索静态站点 |
| [z-lc](https://github.com/z-opc-foundation/z-lc) | 低代码平台——DB 驱动通用 CRUD 引擎 + 报表/模型设计框架 |
| [z-llm-agent](https://github.com/z-opc-foundation/z-llm-agent) | CLI Agent 服务化——把本机 CLI agent（CC/Hermes/Qoder 等）暴露成 OpenAI 兼容对话 API |
| [z-llm-gateway](https://github.com/z-opc-foundation/z-llm-gateway) | LLM 网关——OpenAI/Anthropic 双协议面 + 多 vendor 凭据池、限流与 token 记账 |
| [z-llm-msghub](https://github.com/z-opc-foundation/z-llm-msghub) | msgHub 中心消息路由——WebSocket 中心枢纽 + Agent 侧客户端 SDK |
| [z-mask](https://github.com/z-opc-foundation/z-mask) | 脱敏与遮蔽引擎——按敏感命中位置遮蔽，策略即数据 |
| [z-mcp](https://github.com/z-opc-foundation/z-mcp) | MCP 服务端 + 上游聚合网关——Java 8 手写协议层，Spring Boot 自动装配 |
| [z-meta](https://github.com/z-opc-foundation/z-meta) | 元数据管理平台——租户/应用/接口清单登记 + @BizModel 业务模型采集 |
| [z-mill](https://github.com/z-opc-foundation/z-mill) | 资料磨坊——全格式文档转 Markdown/JSON/纯文本/HTML（对标 MinerU） |
| [z-mist](https://github.com/z-opc-foundation/z-mist) | KMS 分布式密钥/秘密管理平台——集中加密存储 + Netty 协议 + starter 嵌入 |
| [z-mq](https://github.com/z-opc-foundation/z-mq) | 自研分布式消息队列——NameServer+Broker+Store，形状对标 RocketMQ、自有 Netty 协议 |
| [z-msg](https://github.com/z-opc-foundation/z-msg) | 多通道消息 + WebSocket 实时下发基础设施——短信/邮件/站内信/Webhook/IM 平台 |
| [z-observer](https://github.com/z-opc-foundation/z-observer) | 可观测性域——指标/日志/链路 + 告警引擎 |
| [z-opc](https://github.com/z-opc-foundation/z-opc) | 一人公司统一后端+统一前端——单 JVM all-in-one 承载协作/Agent/RPA/交易/资产等业务域与自研中间件 |
| [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) | 组织治理大脑——事实表、强制规范、跨仓工具集、新机器恢复钥匙（本清单权威持有处） |
| [z-opcs](https://github.com/z-opc-foundation/z-opcs) | 任务撮合/服务市场——用户付费发布诉求，持有者接单完成，金额自动瓜分 |
| [z-ops](https://github.com/z-opc-foundation/z-ops) | 个人公司运维平台——部署单元/镜像构建/WebShell/K8s 巡检 + Ops Agent 执行器 |
| [z-oss](https://github.com/z-opc-foundation/z-oss) | 对象存储服务——自持 local 后端 + aliyun/s3 对接层，starter 注入 OssTemplate |
| [z-product](https://github.com/z-opc-foundation/z-product) | 产品设计信息汇总仓（开发域 PM 工件） |
| [z-qa](https://github.com/z-opc-foundation/z-qa) | 步骤式 API 链路测试平台——用例/计划/执行/报告/质量看板（对标 TestRail·MeterSphere） **（本仓）** |
| [z-rpa](https://github.com/z-opc-foundation/z-rpa) | RPA 产品皮——应用市场/需求中心（执行归 z-script，触发归 z-schedule） |
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | Java 8 + Netty RPC 框架——微内核 + SPI 插件化，形状参照 Dubbo/gRPC |
| [z-schedule](https://github.com/z-opc-foundation/z-schedule) | 分布式任务调度中枢（对标 XXL-Job）——选主 + 时间环排期，starter 嵌入宿主 |
| [z-script](https://github.com/z-opc-foundation/z-script) | 多 DSL 脚本平台 + Mock 平台——沙箱执行/版本灰度/可注册 MCP Tool，starter 内嵌 |
| [z-security](https://github.com/z-opc-foundation/z-security) | 数据安全域编排层——场景→策略→识别→脱敏→水印→审计 四段流水线编排 |
| [z-sense](https://github.com/z-opc-foundation/z-sense) | 敏感识别与分类分级引擎——数据安全线底座（规则目录 + 可插拔识别器 + 分类分级） |
| [z-skill](https://github.com/z-opc-foundation/z-skill) | Skill 平台聚合器——归一化各家 Skill 形状，再按各家 API 形状发回 |
| [z-surl](https://github.com/z-opc-foundation/z-surl) | 短网址服务——短码生成/解析/命中统计，starter 一行集成 |
| [z-task](https://github.com/z-opc-foundation/z-task) | 任务协作平台——项目空间/看板/任务卡片（看板范式，对标 Trello·Teambition） |
| [z-team](https://github.com/z-opc-foundation/z-team) | 数字员工协同域——全栈独立仓，自 z-opc 毕业 |
| [z-trade](https://github.com/z-opc-foundation/z-trade) | 交易域独立仓——自 z-opc reactor 迁出 |
| [z-util](https://github.com/z-opc-foundation/z-util) | 底层 Java 工具库——89 个模块 POM，Java 8/17 口径，按需引用按需升级 |
| [z-vector](https://github.com/z-opc-foundation/z-vector) | 自研 Java 向量数据库——嵌入式+可独立部署，ANN+Payload 过滤，兼容 Qdrant REST |
| [z-voice-demo](https://github.com/z-opc-foundation/z-voice-demo) | 语音链路 demo——流式识别→kernel LLM→流式 TTS→可打断 的契约验证实现 |
| [z-watermark](https://github.com/z-opc-foundation/z-watermark) | 水印引擎——显式文字/图片隐写/盲水印，三种技术成对「打+验」 |
| [z-webide](https://github.com/z-opc-foundation/z-webide) | 企业级 WebIDE 后端——Git 仓/共享目录秒变 code-server 实例，全生命周期管理 |
| [z-wf](https://github.com/z-opc-foundation/z-wf) | 自研流程引擎——不依赖 Camunda/Flowable，定义层与运行时全自研 |

**已退役仓**（迁至 [z-trash](https://github.com/z-trash) 组织，archived 只读）：

| 仓 | 说明 |
|---|---|
| [z-agent-proxy](https://github.com/z-trash/z-agent-proxy) | 三方 coding agent 适配代理（Aider/Cursor → kernel Agent SPI），2026-10-26 退役 |
| [z-kb](https://github.com/z-trash/z-kb) | 自研知识库系统（对标 LightRAG/RAGFlow），2026-11 并入 z-cognition 认知舱后退役 |
| [z-ontology](https://github.com/z-trash/z-ontology) | 本体层（TBox/ABox/RDFS 推理），并入 z-cognition 后退役 |

另有 [z-report](https://github.com/z-opc-foundation/z-report)（报表域，2026-10-11 撤销并入 z-lc（远端已删除））。

总清单权威持有处在 [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) `008_组织规范/005_兄弟仓清单.md`。
