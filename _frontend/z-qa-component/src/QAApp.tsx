import React, {useEffect, useState} from 'react';
import {Navigate, Route, Routes} from 'react-router-dom';
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
import {dashboardApi} from './services/api';
import QaNotImplemented from './QaNotImplemented';

/**
 * z-qa 测试平台路由 (FEATURE052 §7).
 * 路径: /z-qa/*
 *
 * 后端能力探测（2026-10-04）：本模块前端完整、后端零实现，详见
 * {@link QaNotImplemented} 的实测依据。进模块先探一个代表性接口，
 * 404 就整块显示「未实现」，避免 40 个接口刷屏 + 未捕获的 AxiosError。
 * 探测是**能力探测而非禁用开关**：后端真落地后自动通过，UI 无需改动即恢复。
 */
const QAApp: React.FC = () => {
    const [backend, setBackend] = useState<'checking' | 'ok' | 'missing'>('checking');

    useEffect(() => {
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
    }, []);

    // 探测在飞时不渲染内容：否则子页会先发一轮注定 404 的请求。
    if (backend !== 'ok') {
        return backend === 'missing' ? <QaNotImplemented/> : null;
    }

    return (
        <Routes>
            {/* 相对路径: 父路由 main.jsx 的 /test/* 已消耗前缀, <Routes> 会把 pathname
             * 重基到 /qa/*, 写绝对路径 /z-qa/xxx 反而一条都匹配不到 */}
            <Route path="dashboard" element={<Dashboard/>}/>
            <Route path="suites" element={<SuiteList/>}/>
            <Route path="suites/:id/steps" element={<SuiteSteps/>}/>
            <Route path="plans" element={<PlanList/>}/>
            <Route path="runs" element={<RunList/>}/>
            <Route path="runs/:id" element={<RunDetail/>}/>
            <Route path="envs" element={<EnvList/>}/>
            <Route path="cases" element={<CaseList/>}/>
            <Route path="schedules" element={<ScheduleList/>}/>
            <Route path="e2e" element={<E2ESessions/>}/>
            <Route path="*" element={<Navigate to="/z-qa/dashboard" replace/>}/>
        </Routes>
    );
};

export default QAApp;
