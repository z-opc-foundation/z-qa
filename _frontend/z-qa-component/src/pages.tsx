import type { ComponentType } from 'react';
import QAApp from './QAApp';

export { configureQa } from './services/api';

/**
 * routes manifest（lead 005 §8.2/§9.3）。
 * QAApp 内部自持 <Routes> 相对路由（qa/dashboard 等子路径），宿主挂 /z-qa/* 一条即可。
 * 数据装配已内聚在 services/api.ts（apiBase 可 configureQa 覆写，默认 '/api'）。
 */
export interface DomainRoute {
    path: string;
    title: string;
    order?: number;
    icon?: ComponentType;
    Component: ComponentType;
}

export const routes: DomainRoute[] = [
    { path: '/z-qa/*', title: 'QA 测试', order: 900, Component: QAApp },
];
