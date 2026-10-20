# z-qa-component

z-opc 测试平台（对标 TestRail / MeterSphere）的管理台页面集。跨仓共享的唯一前端面（lead 005 §9.1）。

- **npm 包名**：`@yuku123/z-qa-component`
- **宿主接入**：`z-qa-suit`（独立壳）或其他主壳（如 z-opc）挂 `@yuku123/z-qa-component/pages`
- **后端**：z-qa-core 的 `/api/qa/**`，库件自带本域接口调用，前缀由 `configureQa` 注入

---

## 一、能力概览

| 页面 | 路由（相对 `/z-qa/*`） | 能力 |
|------|----------------------|------|
| 仪表盘 | `dashboard` | 统计概览、趋势、套件健康度、失败 Top N |
| 测试套件 | `suites` | 套件 CRUD + 触发运行 |
| 套件步骤 | `suites/:id/steps` | 步骤编排与保存 |
| 测试计划 | `plans` | 计划 CRUD + 按计划触发 |
| 执行记录 | `runs` | 执行列表（5s 轮询）+ 进度/通过失败统计 |
| 执行详情 | `runs/:id` | 单次执行的步骤明细 |
| 用例库 | `cases` | 用例 CRUD |
| 环境管理 | `envs` | 环境 CRUD（baseUrl / 默认 headers / 优先级） |
| 定时调度 | `schedules` | cron 定时任务 CRUD + 启停 + 立即扫描 |
| E2E 会话 | `e2e` | 浏览器会话与动作记录 |
| 组件文档 | `docs` | 本文件渲染页 |

---

## 二、导出面

```ts
// 主入口
import QAApp from '@yuku123/z-qa-component';          // 默认导出，自持相对子路由
import { QAApp, QaNotImplemented, configureQa, suiteApi, runApi } from '@yuku123/z-qa-component';

// 宿主组装面（suit / 主壳用）
import { routes, configureQa } from '@yuku123/z-qa-component/pages';
```

`routes` 是单元素 manifest：

```ts
[{ path: '/z-qa/*', title: 'QA 测试', order: 900, Component: QAApp }]
```

宿主只需挂 `/z-qa/*` 一条路由，`QAApp` 内部再用相对路径分发到各子页。

---

## 三、配置

```ts
import { configureQa } from '@yuku123/z-qa-component/pages';

configureQa({ apiBase: '/api' });   // 默认即 '/api'
```

宿主只需把 `/api` 反代到 z-qa-core 所在服务。

**后端能力探测**：`QAApp` 挂载时先打 `GET /api/qa/dashboard/stats`，404 则整块渲染 `QaNotImplemented`
「未实现」页，不再让 40 个接口逐个刷屏。探测通过即自动恢复正常 UI，无需改代码。

---

## 四、后端接口

统一响应包：`{ code: 200, msg: "success", content: <payload>, success: true }`。

### 套件 `/api/qa/suite`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 列表，支持 `keyword` / `category` |
| GET | `/get?id=` | 单个套件 |
| POST | `/add` | 新增（body = 套件对象） |
| POST | `/update` | 更新 |
| POST | `/remove?id=` | 删除 |
| GET | `/steps?suiteId=` | 步骤列表 |
| POST | `/steps/save?suiteId=` | 保存步骤（body = 步骤数组） |

### 用例 `/api/qa/case`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/list` | 列表，body 支持 `keyword` / `priority` / `category`（POST 以承载长 keyword） |
| POST | `/add` | 新增 |
| POST | `/update` | 更新 |
| POST | `/remove?id=` | 删除 |

### 计划 `/api/qa/plan`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 列表 |
| GET | `/get?id=` | 单个计划 |
| POST | `/add` | 新增 |
| POST | `/update` | 更新 |
| POST | `/remove?id=` | 删除 |

### 执行 `/api/qa/run`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list?status=&limit=` | 执行列表 |
| GET | `/get?id=` | 单次执行 |
| GET | `/status?runId=` | 执行状态（轻量，供轮询） |
| GET | `/details?runId=` | 步骤明细 |
| POST | `/trigger?suiteId=&envCode=&triggerUser=&triggerType=` | 触发单套件 |
| POST | `/runPlan?planId=&envCode=&triggerUser=&triggerType=` | 按计划触发 |
| POST | `/abort?runId=` | 中止 |

### 定时 `/api/qa/schedules`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 列表 |
| GET | `/get?id=` | 单个任务 |
| POST | `/add` | 新建 cron 任务 |
| POST | `/update` | 更新 |
| POST | `/remove?id=` | 删除 |
| POST | `/toggle?id=&enabled=` | 启停 |
| POST | `/scan` | 立即扫描到期任务 |

### 环境 `/api/qa/env`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 列表 |
| POST | `/add` | 新增 |
| POST | `/update` | 更新 |
| POST | `/remove?id=` | 删除 |

### 看板 `/api/qa/dashboard`

`GET /stats`、`GET /summary`、`GET /trend`、`GET /suite-health`、`GET /top-failures`。

### E2E `/api/qa/e2e`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sessions/open` | 开会话 |
| POST | `/sessions/{sessionId}/finish` | 结束会话 |
| GET | `/sessions/{sessionId}` | 会话详情 |
| GET | `/sessions/{sessionId}/actions` | 动作列表 |
| POST | `/actions/execute` | 执行单个动作 |

### 报告 `/api/qa/report`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/html/{runCode}` | HTML 报告（text/html） |
| GET | `/chain/{runCode}` | 报告链路数据 |

---

## 五、状态机与执行语义

步骤十态：`PENDING` → `RUNNING` → 终态（`SUCCESS` / `FAILED` / `SKIPPED` / `ABORTED` / …），
由 `QaStepStatus` 定义。

- **failFast**：套件级开关。为开时，本套件某步失败即跳过该套件剩余步骤；计划里的后续套件照常执行。
- **中止**：`POST /run/abort` 立即终止执行中 run。
- **失败三分类**：断言失败 / 业务错误 / 基础设施错误，由 `QaFailureClassifier` 判定，报告与看板按类聚合。

---

## 六、限制与注意事项

- `E2E 会话` 页目前只渲染容器骨架，动作列表尚未接后端（见 `E2ESessions.tsx`）。
- 步骤执行只支持 API 步骤（`ApiStepExecutor`）；浏览器步骤需 E2E 链路，未实现。
- 所有请求走 `services/api.ts` 的薄封装，只做 baseURL 拼接，**不做 401 跳转**——那是宿主 request 层的职责。
- 组件依赖 antd 6 / React 19 / react-router-dom 7，均为 peerDependency，宿主版本由宿主决定。

---

## 七、变更记录

| 版本 | 变更 |
|------|------|
| 0.1.2 | 补 §2 列表页四件套（分页 / 搜索 / EmptyState / 操作列规范）；补 §7 文档页；工程侧出 `d.ts`、补 TypeScript 5.9 |
| 0.1.1 | `file:` 协议改钉 registry 版本；suit 侧加 `LOCAL_SIBLINGS` 源码联调门控 |
| 0.1.0 | 首版：Dashboard / Suite / Plan / Run / Case / Env / Schedule / E2E 八页 |
