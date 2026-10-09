import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import 'antd/dist/reset.css'
import { routes } from '@yuku123/z-qa-component/pages'

// suit 只做壳：默认 apiBase='/api'（本 dev 经 vite proxy 转主启动器 8888）。
// 换宿主时在挂载前 configureQa({ apiBase: '/其他前缀' })。
createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ConfigProvider locale={zhCN}>
            <BrowserRouter>
                <Routes>
                    {routes.map((r) => (
                        <Route key={r.path} path={r.path} element={<r.Component />} />
                    ))}
                    <Route path="*" element={<Navigate to="/test/qa/dashboard" replace />} />
                </Routes>
            </BrowserRouter>
        </ConfigProvider>
    </StrictMode>,
)
