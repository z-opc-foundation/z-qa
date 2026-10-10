import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import 'antd/dist/reset.css'
import App from './App'

// suit 只做壳：默认 apiBase='/api'（本 dev 经 vite proxy 转主启动器 8888）。
// 换宿主时在挂载前 configureQa({ apiBase: '/其他前缀' })。
createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ConfigProvider locale={zhCN}>
            <App />
        </ConfigProvider>
    </StrictMode>,
)
