import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import dts from 'vite-plugin-dts';
import path from 'path';

// z-qa 组件层 — Vite library mode 双入口（lead 005 §1.2/§8.2/§9）
//   .      → dist/index.js  （QAApp + services 桶导出）
//   ./pages → dist/pages.js （routes manifest + configureQa，宿主重组契约面）
// d.ts 走 rollupTypes 打成单文件直接落在 dist/ 根，与 package.json 的 types 字段对齐
// （非 rollup 模式会落到 dist/src/**，manifest 会指向不存在的路径）。
export default defineConfig({
    plugins: [react(), dts({ tsconfigPath: path.resolve(__dirname, 'tsconfig.json'), rollupTypes: true })],
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
                '@yuku123/z-frontend-common',
            ],
        },
    },
});
