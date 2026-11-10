import React, {useEffect, useState} from 'react';
import {Navigate, useLocation} from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import SuiteList from './pages/SuiteList';
import SuiteSteps from './pages/SuiteSteps';
import PlanList from './pages/PlanList';
import RunList from './pages/RunList';
import RunDetail from './pages/RunDetail';
import EnvList from './pages/EnvList';
import CaseList from './pages/CaseList';
import ScheduleList from './pages/ScheduleList';
import E2ESessions from './pages/E2ESessions';
import ComponentDoc from './pages/ComponentDoc';
import {dashboardApi} from './services/api';
import QaNotImplemented from './QaNotImplemented';
import {qaBase} from './qaBase';

/**
 * z-qa 测试平台路由 (FEATURE052 §7).
 * 路径: /z-qa/*（Z-Team 工作台挂在 /test/qa）
 *
 * 后端能力探测（2026-10-04）：本模块前端完整、后端零实现，详见
 * {@link QaNotImplemented} 的实测依据。进模块先探一个代表性接口，
 * 404 就整块显示「未实现」，避免 40 个接口刷屏 + 未捕获的 AxiosError。
 * 探测是**能力探测而非禁用开关**：后端真落地后自动通过，UI 无需改动即恢复。
 */

/**
 * 这里刻意**不用嵌套 &lt;Routes&gt;**：Z-Team 的 SystemShell 是按 pathname
 * 直接渲染业务组件的，其 RouteContext 来自 main.jsx 的 {@code /test/*}，
 * 剩余路径成了 {@code qa/cases}；子路由写 {@code cases} 或
 * {@code /test/qa/cases} 都匹配不到，只会在 * 兜底里反复重定向。
 * 按挂载前缀切出相对段自己做分发，两种宿主形态表现一致。
 */
const QAApp: React.FC<{ mountPath?: string }> = ({mountPath}) => {
    const [backend, setBackend] = useState<'checking' | 'ok' | 'missing'>('checking');
    const {pathname} = useLocation();
    // 挂载前缀：宿主显式声明优先；否则按「路径里找 /qa 或 /z-qa 段」推断。
    // 推断不到（宿主在无 qa 段的路径上合成挂载，如 SystemShell ?mode=test 把
    // effectivePath 定成 /test/qa/dashboard 但真实 pathname 仍是 /workspace）时，
    // mountPath 是唯一可靠来源 —— 没有它 base 为空，default 分支会把用户
    // <Navigate> 到无主的 /dashboard（2026-11-11 实测横跳/404 根因）。
    const base = mountPath ?? qaBase(pathname);
    // 文档页只读编译期嵌入的 DOC.md，不打任何后端接口；探不通时也必须可达。
    const isDocs = /\/docs\/?$/.test(pathname);

    useEffect(() => {
        if (isDocs) return;
        let cancelled = false;
        // 任取一个代表性接口即可；选 stats 是因为它无路径参数、最轻。
        dashboardApi
            .stats()
            .then(() => {
                if (!cancelled) setBackend('ok');
            })
            .catch(() => {
                if (!cancelled) setBackend('missing');
            });
        return () => {
            cancelled = true;
        };
    }, [isDocs]);

    if (isDocs) {
        return <ComponentDoc/>;
    }

    // 探测在飞时不渲染内容：否则子页会先发一轮注定 404 的请求。
    if (backend !== 'ok') {
        return backend === 'missing' ? <QaNotImplemented/> : null;
    }

    // 子路径解析：真实路径以挂载前缀开头时按真实路径切；合成挂载（真实路径
    // 不含前缀）时视作落在默认页 —— 页内跳转走 `${base}/xxx` 真实导航，
    // 自然过渡到 /test/qa/* 形态，由宿主真实路由接管。
    const rel = (base && pathname.startsWith(base)
        ? pathname.slice(base.length)
        : '/dashboard'
    ).replace(/^\/+|\/+$/g, '');
    const [head, param, tail] = rel.split('/');

    switch (head) {
        case '':
        case 'dashboard':
            return <Dashboard/>;
        case 'suites':
            return param && tail === 'steps' ? <SuiteSteps id={param}/> : <SuiteList/>;
        case 'plans':
            return <PlanList/>;
        case 'runs':
            return param ? <RunDetail id={param}/> : <RunList/>;
        case 'envs':
            return <EnvList/>;
        case 'cases':
            return <CaseList/>;
        case 'schedules':
            return <ScheduleList/>;
        case 'e2e':
            return <E2ESessions/>;
        default:
            return <Navigate to={`${base}/dashboard`} replace/>;
    }
};

export default QAApp;
