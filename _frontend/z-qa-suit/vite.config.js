import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// z-qa 独立运行壳。dev 代理 /api → z-opc main-starter(:8888)，qa 后端目前宿主在内。
export default defineConfig({
    plugins: [react()],
  resolve: { dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'] },
    server: {
        port: 3001,
        fs: { allow: ['..'] },
        proxy: { '/api': { target: 'http://localhost:8888', changeOrigin: true } },
    },
})
