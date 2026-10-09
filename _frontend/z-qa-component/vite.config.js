import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// z-qa 组件层 — Vite library mode 双入口（lead 005 §1.2/§8.2/§9）
//   .      → dist/index.js  （QAApp + services 桶导出）
//   ./pages → dist/pages.js （routes manifest + configureQa，宿主重组契约面）
export default defineConfig({
    plugins: [react()],
    build: {
        lib: {
            entry: {
                index: path.resolve(__dirname, 'src/index.ts'),
                pages: path.resolve(__dirname, 'src/pages.tsx'),
            },
            formats: ['es'],
        },
        outDir: 'dist',
        emptyOutDir: true,
        sourcemap: false,
        rollupOptions: {
            external: [
                'react', 'react-dom', 'react-dom/client',
                'react-router-dom', 'antd', '@ant-design/icons', 'axios',
            ],
        },
    },
});
