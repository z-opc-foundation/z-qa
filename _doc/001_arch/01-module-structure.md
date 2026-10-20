# 模块与包结构

## Maven 坐标

| 坐标 | packaging | 定位 |
|------|-----------|------|
| `io.github.yuku123:z-qa` | pom | 聚合 + 版本根（`${revision}`，CI-friendly）+ flatten(oss) |
| `io.github.yuku123:z-qa-core` | jar | 领域与引擎，入 Central |
| `io.github.yuku123:z-qa-web` | jar | API 层与装配，入 Central |
| `io.github.yuku123:z-qa-admin` | jar | 可执行 fat jar，**不入** Central |

父链：`io.github.yuku123:z-boot-parent`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘）。

## 目录树

```
z-qa/
├── pom.xml                       # 聚合 + DM + central profile（排除 z-qa-admin）
├── Dockerfile                    # eclipse-temurin:8-jdk，拷 z-qa-admin fat jar，容器内 8080
├── _doc/
│   ├── 001_arch/                 # 本目录
│   ├── 002_deploy/init/schema.sql   # 12 张表的完整 DDL 单一入口
│   ├── 003_script/deploy_maven_center.sh
│   └── 005_testing/self_test.sql      # zopc-self-test 冒烟种子（凭据为占位串）
├── z-qa-core/
│   ├── src/main/java/com/zifang/z/qa/admin/
│   │   ├── config/QaAsyncConfig.java         # @EnableAsync
│   │   ├── domain/entity/                    # 12 个 Qa*DO（MyBatis-Plus）
│   │   ├── domain/mapper/                    # 12 个 Qa*Mapper（4 个带 XML）
│   │   ├── engine/                           # Dispatcher / RunRunner / ApiStepExecutor
│   │   │                                     #   / FailureClassifier / StepStatus
│   │   └── service/ + service/impl/          # E2e · Notify · Report · Schedule
│   ├── src/main/resources/mapper/            # QaRun / QaRunDetail / QaSuiteStep / QaE2eAction XML
│   └── src/test/java/                         # 36 个测试（见下）
├── z-qa-web/
│   ├── src/main/java/com/zifang/z/qa/web/
│   │   ├── api/          # SuiteApi CaseApi PlanApi EnvApi ScheduleApi RunApi
│   │   │                 # DashboardApi E2eApi ReportApi —— 9 个 @RestController
│   │   ├── request/      # 10 个 *Req
│   │   ├── response/     # 10 个 *Resp + QaResult（统一信封）
│   │   └── config/       # QaAutoConfiguration + QaModuleDataSource
│   └── src/main/resources/META-INF/spring.factories   # FQCN 注册清单
├── z-qa-admin/
│   ├── src/main/java/com/zifang/z/qa/admin/
│   │   ├── host/ZQaAdminApplication.java     # @SpringBootApplication 主类
│   │   └── config/QaWebMvcConfig.java        # /z-qa/** 的 SPA 回落
│   └── src/main/resources/application.yml    # 8083 + z.base.db.qa.*（全走 ZQA_DB_* 环境变量）
└── _frontend/
    ├── z-qa-component/     # 可复用页面集 + src/DOC.md 组件文档（npm @yuku123/z-qa-component）
    └── z-qa-suit/          # 本域 SPA 壳，路由表 /z-qa/**
```

## 分层规则

- **DO 不出 Web 层**：`api/*Api` 只接 `request/*Req`、只返 `response/*Resp`，
  转换走 `web/api/QaConverter`（`BeanUtils.copyProperties`，字段名与 DO 严格同名）。
- **命名**：API 类叫 `XxxApi`（不是 `XxxController`），bean 名显式给
  `@RestController("qaXxxController")` —— 避免与宿主的同名 Controller 撞 bean 名。
- **字段一律驼峰**：JSON 入参出参统一 camelCase，与 DO 字段同名，`copyProperties` 才成立。
  历史上前端曾发 snake_case（`env_code` / `fail_fast` / `suite_ids`），
  2026-10-19 起前后端一起改成驼峰，**跨仓消费 `/api/qa/**` 的调用方需同步改**。

## 前端构建接线

`z-qa-admin/pom.xml` 里 `frontend-maven-plugin` 在 `_frontend/z-qa-suit` 下装 Node v22.14.0、
跑 `npm install --registry=https://registry.npmmirror.com` 与 `npm run build`，
`maven-resources-plugin` 在 `prepare-package` 把 `dist/` 复制进 `target/classes/static/`。
产物是单个 fat jar，前后端同源，不需要额外的静态服务器。

## 测试

| 类 | 数量 | 手法 |
|----|------|------|
| `QaScheduleServiceImplTest` | 5 | 纯函数（cron 解析） |
| `QaFailureClassifierTest` | 15 | 纯函数（十态分类 / 系统错误优先 / 缺陷三分类 / 可重试） |
| `ApiStepExecutorTest` | 7 | 对 JDK 内置 `HttpServer` 打**真实 socket**，不打桩连接 |
| `QaRunRunnerTest` | 9 | Mapper 走 **JDK 动态代理** + 反射注入；零 Mockito、零 `@SpringBootTest` |

```
mvn -pl z-qa-core test     # 36 tests
```
