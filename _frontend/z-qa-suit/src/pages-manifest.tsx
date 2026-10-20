import type { ComponentType, ReactNode } from 'react'
import { ApiOutlined, AppstoreOutlined, ClockCircleOutlined, CloudOutlined, DatabaseOutlined, FileTextOutlined, HomeOutlined, OrderedListOutlined, PlayCircleOutlined, ReadOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import { routes } from '@yuku123/z-qa-component/pages'

export interface MenuItem { key: string; label: string; icon: ReactNode }
export interface RouteEntry { path: string; Component: ComponentType }

/**
 * 菜单 + 路由清单（lead 008 §10/§14/§16）。QAApp 内部自持相对子路由（suites/:id/steps、runs/:id 为详情页）。
 *
 * §9.6：本壳默认挂在站点根。若宿主按子路径挂载（如 /z-qa/），构建时传 BASE_PATH，
 * 这里所有 key/path 会自动带上该前缀，不必逐条手改。
 */
const P = import.meta.env.BASE_URL.replace(/\/$/, '')
export const menuItems: MenuItem[] = [
    { key: `${P}/z-qa/home`, label: '首页', icon: <HomeOutlined /> },
    { key: `${P}/z-qa/dashboard`, label: '仪表盘', icon: <AppstoreOutlined /> },
    { key: `${P}/z-qa/suites`, label: '测试套件', icon: <DatabaseOutlined /> },
    { key: `${P}/z-qa/plans`, label: '测试计划', icon: <FileTextOutlined /> },
    { key: `${P}/z-qa/runs`, label: '测试执行', icon: <PlayCircleOutlined /> },
    { key: `${P}/z-qa/cases`, label: '用例库', icon: <OrderedListOutlined /> },
    { key: `${P}/z-qa/envs`, label: '环境管理', icon: <CloudOutlined /> },
    { key: `${P}/z-qa/schedules`, label: '定时调度', icon: <ClockCircleOutlined /> },
    { key: `${P}/z-qa/e2e`, label: 'E2E 会话', icon: <ApiOutlined /> },
    // §7 顺序铁律：组件文档恒为末项
    { key: `${P}/z-qa/docs`, label: '组件文档', icon: <ReadOutlined /> },
]

export const routeTable: RouteEntry[] = [
    { path: `${P}/z-qa/home`, Component: HomePage },
    { path: `${P}/z-qa/*`, Component: routes[0].Component },
]

export const LOGIN_PATH = `${P}/z-qa/login`
