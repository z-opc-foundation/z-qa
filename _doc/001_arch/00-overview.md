# z-qa 架构总览

## 这是什么

z-qa 是测试平台（对标 TestRail / MeterSphere 的核心子集）：把「一组有序的 HTTP / E2E 步骤」组织成
**套件（Suite）**，把「一组套件」组织成**计划（Plan）**，然后执行出一条 **Run**，每一步都留下可溯源的
**Run Detail**，并给出**缺陷三分类**与**质量看板**。

## 一句话架构

```
套件 / 计划 / 环境 / 定时  →  执行引擎（步骤循环）  →  每步明细 + 缺陷定性  →  报告 / 通知 / 看板
```

引擎侧只有三个类承担全部策略：

| 类 | 职责 |
|----|------|
| `QaExecutionDispatcher` | 入口分流：`@Async` 触发 `QaRunRunner`，把循环交给独立 bean |
| `QaRunRunner` | 步骤循环：retry / failFast / abort / 计数 / 逐步落库 |
| `ApiStepExecutor` | 单步执行：变量替换 → 发请求 → JSONPath 提取 → 断言 |
| `QaFailureClassifier` | 失败定性的**单一真源**：异常分类、系统错误识别、缺陷三分类、可重试判定 |

## 十个步骤状态

`QaStepStatus` 定义十态，落在 `z_qa_run_detail.result`：

`pending` · `running` · `success` · `skip` ·
`fail_assert` · `fail_system_error` · `fail_timeout` · `fail_navigation` · `fail_param_null` · `fail_unknown`

## 三条必须记住的语义

1. **failFast 的粒度是套件，不是整轮。** 触发后只跳过该套件剩余步骤，计划里的后续套件照常执行
   （对齐「abort the scenario, next scenario continues」）。
2. **abort 标志走 DB，不走内存 flag。** 发起中止的实例未必是跑这条 run 的实例；因此计数器与终态的
   update 都只写自己那几列，整行回写会把 `abort_requested` 覆盖回旧快照。
3. **系统错误优先于断言。** 5xx 必须先判，否则会被顺带失败的断言掩盖成 `fail_assert`，缺陷单直接开错方向。

## 模块边界

| 模块 | 定位 | 入 Central |
|------|------|-----------|
| `z-qa-core` | 领域与引擎：DO / Mapper / Service / Executor | 是 |
| `z-qa-web` | API 层：`request/*Req` + `response/*Resp` + `api/*Api` + 数据源装配 | 是 |
| `z-qa-admin` | 可执行应用：`SpringApplication` 主类 + SPA 回落 + 前端打进 fat jar | 否（`excludeArtifacts` 点名排除） |

DO 不出 Web 层（lead 007 §6）。

## 自动装配

全部业务 bean 在 `z-qa-web/src/main/resources/META-INF/spring.factories` 的
`org.springframework.boot.autoconfigure.EnableAutoConfiguration` 键下以 **FQCN** 注册，
刻意不用 `@ComponentScan` —— 宿主若也扫到本包会双注册，同类型 bean 出现两份，直接
`NoUniqueBeanDefinitionException`。

宿主接入只需：引 `z-qa-web`（传递带 `z-qa-core`）+ 配 `z.base.db.qa.*`。

## 已知风险（如实登记）

| 风险 | 现状 | 影响面 |
|------|------|--------|
| **e2e 步骤无执行器** | `QaRunRunner.executeWithRetry` 对 `execution_mode=e2e` 的步骤返回 `null`，该步落 `fail_unknown`。E2E 只在 `E2eApi` 的会话/动作接口上可用，不进 run 循环 | 建了含 e2e 步骤的套件，跑出来必然是 `fail_unknown`（刻意不退化成发 HTTP，见 `QaRunRunner` 注释） |
| **定时扫描依赖宿主开 `@EnableScheduling`** | 本仓只带 `@EnableAsync`（`QaAsyncConfig`），没有 `@EnableScheduling` | 宿主不开就是只有 `POST /api/qa/schedules/scan` 手动兜底 |
| **`z_qa_assert_template` 只有表与 Mapper** | 无对应 Service 与 API | 断言模板功能未开放 |
| **`@Async` 依赖代理生效** | `QaExecutionDispatcher` 必须与 `QaRunRunner` 分成两个 bean | 宿主若把两者合回一个类，整条 run 会同步跑在 HTTP 请求线程上 |

> 详见 `03-db-schema.md`（DDL 单一入口）与 `02-api.md`（API 与 DTO 契约）。
