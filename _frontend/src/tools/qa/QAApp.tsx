import React from 'react';
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

/**
 * z-qa 测试平台路由 (FEATURE052 §7).
 * 路径: /test/qa/*
 */
const QAApp: React.FC = () => {
    return (
        <Routes>
            {/* 相对路径: 父路由 main.jsx 的 /test/* 已消耗前缀, <Routes> 会把 pathname
             * 重基到 /qa/*, 写绝对路径 /test/qa/xxx 反而一条都匹配不到 */}
            <Route path="qa" element={<Dashboard/>}/>
            <Route path="qa/dashboard" element={<Dashboard/>}/>
            <Route path="qa/suites" element={<SuiteList/>}/>
            <Route path="qa/suites/:id/steps" element={<SuiteSteps/>}/>
            <Route path="qa/plans" element={<PlanList/>}/>
            <Route path="qa/runs" element={<RunList/>}/>
            <Route path="qa/runs/:id" element={<RunDetail/>}/>
            <Route path="qa/envs" element={<EnvList/>}/>
            <Route path="qa/cases" element={<CaseList/>}/>
            <Route path="qa/schedules" element={<ScheduleList/>}/>
            <Route path="qa/e2e" element={<E2ESessions/>}/>
            <Route path="*" element={<Navigate to="/test/qa/dashboard" replace/>}/>
        </Routes>
    );
};

export default QAApp;
