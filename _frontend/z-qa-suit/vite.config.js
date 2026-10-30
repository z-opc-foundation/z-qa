import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import {safeEmptyOutDir} from '../../build-tools/safe-vite-plugin.mjs'

// z-qa 独立运行壳。dev 代理 /api → z-opc main-starter(:8888)，qa 后端目前宿主在内。
//
// 三道环境开关：
//   BASE_PATH       宿主挂载前缀（lead 005 §9.6 通道三）。默认 '/'，主壳挂 /z-qa/* 时传 /z-qa/。
//   LOCAL_SIBLINGS  =1 时 component 走 ../z-qa-component/src 源码，改完即生效；默认走 registry 包。
//   VITE_PROXY_TARGET  dev 代理后端地址。
const LOCAL_SIBLINGS = process.env.LOCAL_SIBLINGS === '1'
const BASE_PATH = process.env.BASE_PATH || '/'

export default defineConfig({
    base: BASE_PATH,
    plugins: [safeEmptyOutDir(), react()],
    resolve: {
        dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'],
        alias: LOCAL_SIBLINGS
            ? { '@yuku123/z-qa-component/pages': path.resolve(__dirname, '../z-qa-component/src/pages.tsx') }
            : {},
    },
    server: {
        port: 3001,
        fs: { allow: ['..'] },
        proxy: {
            '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8888', changeOrigin: true },
        },
    },
})
