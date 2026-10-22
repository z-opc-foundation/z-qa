import type { ComponentType } from 'react';
import QAApp from './QAApp';

export { configureQa } from './services/api';
export { default as ComponentDoc } from './pages/ComponentDoc';
// 主壳接线用的统一名（lead 008 §7 约定：各包主入口统一导出 ComponentDocPage）。
// 与上面的 ComponentDoc 指向同一个组件，不是两份实现 —— 原名保留给已在用它的页面。
export { default as ComponentDocPage } from './pages/ComponentDoc';
/** 本组件包版本（lead 005 §12：宿主版本号走这里，不硬编码）。 */
export { version as componentVersion } from '../package.json';

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
