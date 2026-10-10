import type { ComponentType, ReactNode } from 'react'
import { ApiOutlined, AppstoreOutlined, ClockCircleOutlined, CloudOutlined, DatabaseOutlined, FileTextOutlined, HomeOutlined, OrderedListOutlined, PlayCircleOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import { routes } from '@yuku123/z-qa-component/pages'

export interface MenuItem { key: string; label: string; icon: ReactNode }
export interface RouteEntry { path: string; Component: ComponentType }

/** 菜单 + 路由清单（lead 008 §10/§14/§16）。QAApp 内部自持相对子路由（suites/:id/steps、runs/:id 为详情页）。 */
export const menuItems: MenuItem[] = [
    { key: '/z-qa/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-qa/dashboard', label: '仪表盘', icon: <AppstoreOutlined /> },
    { key: '/z-qa/suites', label: '测试套件', icon: <DatabaseOutlined /> },
    { key: '/z-qa/plans', label: '测试计划', icon: <FileTextOutlined /> },
    { key: '/z-qa/runs', label: '测试执行', icon: <PlayCircleOutlined /> },
    { key: '/z-qa/cases', label: '用例库', icon: <OrderedListOutlined /> },
    { key: '/z-qa/envs', label: '环境管理', icon: <CloudOutlined /> },
    { key: '/z-qa/schedules', label: '定时调度', icon: <ClockCircleOutlined /> },
    { key: '/z-qa/e2e', label: 'E2E 会话', icon: <ApiOutlined /> },
]

export const routeTable: RouteEntry[] = [
    { path: '/z-qa/home', Component: HomePage },
    { path: '/z-qa/*', Component: routes[0].Component },
]
