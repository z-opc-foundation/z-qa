# API 与 DTO 契约

## 统一响应信封

所有 JSON 端点返回 `web/response/QaResult`：

```json
{ "code": 200, "msg": "success", "content": <载荷>, "success": true }
```

- 载荷键是 **`content`**（不是 `data`）。
- 工厂方法：`QaResult.ok(content)` / `ok(msg, content)` / `ok()` /
  `notFound(msg)`（`code=404`）/ `badRequest(msg)`。
- 前端 `services/api.ts` 的 `QaResponse<T>` 与之逐字段对齐，取值一律 `res?.content`。

## DTO 边界（lead 007 §6）

| 层 | 包 | 约束 |
|----|----|------|
| 入参 | `web/request/*Req` | 普通 POJO，无注解 |
| 出参 | `web/response/*Resp` | 普通 POJO，无注解 |
| 转换 | `web/api/QaConverter` | `BeanUtils.copyProperties`，**字段名必须与 DO 严格同名** |

**DO 不出 Web 层。** `*Resp` 的字段是从 DO 源文件逐个 `private <Type> <name>;` 抄出来的，
不是手写猜的 —— 猜错一个名字 `copyProperties` 会静默丢字段，编译也拦不住。

字段一律 **camelCase**。2026-10-19 之前前端曾发 snake_case（`env_code` / `fail_fast` /
`target_type` / `suite_ids` / `cron_expr`），现已前后端一起转驼峰。

## 端点清单

### 套件 `/api/qa/suite` — `SuiteApi`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` · `/get` · `/steps` | 列表 / 详情 / 步骤（`?suiteId=`） |
| POST | `/add` · `/update` · `/remove` · `/steps/save` | 增改删；`steps/save` 是**整表覆盖** |

`failFast` 是**套件级**策略：触发后只跳过该套件剩余步骤，计划里的后续套件照常执行。

### 用例 `/api/qa/case` — `CaseApi`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/list` | 按 `keyword`（code/name 模糊）/ `priority` / `category` 过滤。用 POST 是为了兼容长 keyword |
| POST | `/add` · `/update` · `/remove` | 增改删；新增缺省 `priority=P1`、`status=1` |

### 计划 `/api/qa/plan` — `PlanApi`

`GET /list` · `GET /get` · `POST /add|update|remove`。
`suiteIds` 在库里存 JSON 数组字符串，**引用失效的套件会被跳过并 warn**；
全部失效时 run 直接落 `failed`，不会永远停在 `running`。

### 环境 `/api/qa/env` — `EnvApi`

`GET /list` · `POST /add|update|remove`。步骤的相对路径靠 `baseUrl` 拼绝对 URL；
`env_code` 在 `z_qa_env` 里查不到时回落 `local`。

### 定时 `/api/qa/schedules` — `ScheduleApi`

`GET /list|get` · `POST /add|update|remove|toggle|scan`。
`cronExpr` 自实现 5 位（unix）/ 6 位（quartz）解析，不引 cron-utils。
`@Scheduled(cron = "0 * * * * ?")` 每分钟扫描，**但本仓没有 `@EnableScheduling`** ——
宿主不开就只有 `/scan` 手动兜底。

### 执行 `/api/qa` — `RunApi`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/run/list` · `/run/get` · `/run/details` · `/run/status` | 执行记录 / 详情 / 进度 |
| POST | `/run/trigger` | 触发套件，**query 参数**（`suiteId` / `envCode` / `triggerUser` / `triggerType`） |
| POST | `/run/runPlan` | 触发计划，query 参数 `planId` |
| POST | `/run/abort` · `/runs/{runCode}/abort` | 中止；两种寻址方式，`runCode` 供跨实例用 |

`trigger` / `runPlan` 走 query 参数而非 JSON body，前端 `axios` 的 `params` 口径 —— 刻意不为统一而改。

### 看板 `/api/qa/dashboard` — `DashboardApi`

`/stats` · `/summary` · `/trend`（近 14 天）· `/suite-health`（各套件 pass rate 排序）·
`/top-failures`（Top 失败步骤）。

### E2E `/api/qa/e2e` — `E2eApi`

`POST /sessions/open` · `POST /sessions/{sessionId}/finish` · `GET /sessions/{sessionId}` ·
`GET /sessions/{sessionId}/actions` · `POST /actions/execute`。

`actions/execute` 是**同步执行一步**（调试用），走 browserless `/function`；
`finish` 收 `videoOssKey` / `traceOssKey` / `harOssKey`。

### 报告 `/api/qa/report` — `ReportApi`

`GET /html/{runCode}`（`text/html`，整页报告）· `GET /chain/{runCode}`（链路 JSON）。

## 步骤状态机

`QaStepStatus` 十态，落在 `z_qa_run_detail.result`：

| 态 | 含义 | 缺陷分类 | 可重试 |
|----|------|---------|--------|
| `pending` | 未开始 | — | — |
| `running` | 执行中 | — | — |
| `success` | 通过 | — | — |
| `skip` | 跳过（abort / failFast 命中） | — | — |
| `fail_assert` | 断言不符 | `system_defect` | 否（重试也不会变） |
| `fail_system_error` | 被测系统自己报错（5xx / 信封 code≥500 / 4xx 带错误标记） | `system_defect` | 是 |
| `fail_timeout` | 超时 | `script_defect` | 是 |
| `fail_navigation` | 连不上 / 域名解析失败 / URL 非法 | `script_defect` | 是 |
| `fail_param_null` | 上下文缺 `${VAR}`，**未发请求** | `env_data_defect` | 否 |
| `fail_unknown` | 兜底（含 e2e 步骤尚无执行器） | `script_defect` | 是 |

判定顺序：**系统错误优先于断言**。5xx 若被顺带失败的断言吃掉，缺陷会直接开错方向。

## 断言规则

```json
{ "status": 200, "$.code": 200, "$.msg": "success" }
```

`status` 走 HTTP 状态码，`$.` 开头走 JSONPath（支持 `$.a.b` 与 `$.arr[0].field`）。
响应体不是 JSON（HTML / 纯文本）时只认 `status`，`$.` 规则判为不通过。

不给断言规则时走默认断言：状态码 2xx = pass。

## 变量与提取

- URL / body / header 支持 `${VAR}` 占位符，取不到即 `fail_param_null` 且**不发请求**
  （原先静默替换成空串，会把 `/api/x?id=` 这种残破请求发出去，报出来的是一行断言失败）。
- 响应提取规则 `{"TOKEN": "$.data.token"}` 写进步骤上下文，**按套件重置** ——
  上一个套件提取的 `${TOKEN}` 不该漏进下一个套件。

## 前端调用口径

`_frontend/z-qa-component/src/services/api.ts` 是唯一接口出口：

```ts
const res = await suiteApi.list();   // QaResponse<T>
res?.content                        // 取载荷
```

`apiBase` 默认 `/api`，宿主可用 `configureQa({ apiBase })` 覆写。
本层只做 baseURL 拼接，不做 unwrap / 401 跳转 —— 那是宿主 request 层的职责。

前端路由全部在 `/z-qa/**` 下（`_frontend/z-qa-suit/src/pages-manifest.tsx`），
后端不加 context-path，由 Ingress / frps 按 Host 分流。
