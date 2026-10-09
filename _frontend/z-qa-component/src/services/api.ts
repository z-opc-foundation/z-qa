import axios from 'axios';

// lead 005 §9.3 终版：component 自洽——自带本域接口调用，前缀参数注入。
// 宿主接一个 /api 反代到本域后端即可；默认 '/api'，可 configureQa({ apiBase }) 覆写。
let apiBase = '/api';

export function configureQa({ apiBase: base } = {}) {
    if (base !== undefined) apiBase = base;
}

// 轻封装：仅做 baseURL 拼接，不裹 unwrap/401 跳转（那是宿主 request 层的职责）。
const request = {
    get: (url, config) => axios.get(apiBase + url, config),
    post: (url, data, config) => axios.post(apiBase + url, data, config),
    put: (url, data, config) => axios.put(apiBase + url, data, config),
    delete: (url, config) => axios.delete(apiBase + url, config),
};

// ============ 套件 (Suite) ============
export const suiteApi = {
    list: (params) => request.get('/qa/suite/list', {params}),
    get: (id) => request.get('/qa/suite/get', {params: {id}}),
    add: (data) => request.post('/qa/suite/add', data),
    update: (data) => request.post('/qa/suite/update', data),
    remove: (id) => request.post('/qa/suite/remove', null, {params: {id}}),
    steps: (suiteId) => request.get('/qa/suite/steps', {params: {suiteId}}),
    saveSteps: (suiteId, steps) => request.post('/qa/suite/steps/save', steps, {params: {suiteId}}),
};

// ============ 用例 (Case) ============
export const caseApi = {
    list: (params) => request.post('/qa/case/list', params),
    add: (data) => request.post('/qa/case/add', data),
    update: (data) => request.post('/qa/case/update', data),
    remove: (id) => request.post('/qa/case/remove', null, {params: {id}}),
};

// ============ 计划 (Plan) ============
export const planApi = {
    list: () => request.get('/qa/plan/list'),
    get: (id) => request.get('/qa/plan/get', {params: {id}}),
    add: (data) => request.post('/qa/plan/add', data),
    update: (data) => request.post('/qa/plan/update', data),
    remove: (id) => request.post('/qa/plan/remove', null, {params: {id}}),
    run: (planId, envCode, triggerUser, triggerType) =>
        request.post('/qa/run/runPlan', null, {params: {planId, envCode, triggerUser, triggerType}}),

};

// ============ 执行 (Run) ============
export const runApi = {
    list: (params) => request.get('/qa/run/list', {params}),
    get: (id) => request.get('/qa/run/get', {params: {id}}),
    trigger: (suiteId, envCode, triggerUser, triggerType) =>
        request.post('/qa/run/trigger', null, {params: {suiteId, envCode, triggerUser, triggerType}}),
    status: (runId) => request.get('/qa/run/status', {params: {runId}}),
    details: (runId) => request.get('/qa/run/details', {params: {runId}}),
    abort: (runId) => request.post('/qa/run/abort', null, {params: {runId}}),
};

// ============ 定时执行 (Schedule) — FEATURE052 Phase 5 ============
export const scheduleApi = {
    list: () => request.get('/qa/schedules/list'),
    get: (id) => request.get('/qa/schedules/get', {params: {id}}),
    add: (data) => request.post('/qa/schedules/add', data),
    update: (data) => request.post('/qa/schedules/update', data),
    remove: (id) => request.post('/qa/schedules/remove', null, {params: {id}}),
    toggle: (id, enabled) => request.post('/qa/schedules/toggle', null, {params: {id, enabled}}),
    scanNow: () => request.post('/qa/schedules/scan'),
};

// ============ 环境 (Env) ============
export const envApi = {
    list: () => request.get('/qa/env/list'),
    add: (data) => request.post('/qa/env/add', data),
    update: (data) => request.post('/qa/env/update', data),
    remove: (id) => request.post('/qa/env/remove', null, {params: {id}}),
};

// ============ 看板 (Dashboard) ============
export const dashboardApi = {
    stats: () => request.get('/qa/dashboard/stats'),
    summary: () => request.get('/qa/dashboard/summary'),
    trend: () => request.get('/qa/dashboard/trend'),
    suiteHealth: () => request.get('/qa/dashboard/suite-health'),
    topFailures: () => request.get('/qa/dashboard/top-failures'),
};

// ============ E2E 浏览器 (FEATURE052 Phase 7) ============
export const e2eApi = {
    openSession: (data) => request.post('/qa/e2e/sessions/open', data),
    finishSession: (id, data) => request.post('/qa/e2e/sessions/' + id + '/finish', data),
    getSession: (id) => request.get('/qa/e2e/sessions/' + id),
    listActions: (id) => request.get('/qa/e2e/sessions/' + id + '/actions'),
    executeAction: (data) => request.post('/qa/e2e/actions/execute', data),
};
