export * from './services/api';
export * from './services/stepStatus';
export { default as QAApp } from './QAApp';
export { default as ComponentDoc } from './pages/ComponentDoc';
// 主壳接线用的统一名（lead 008 §7 约定：各包主入口统一导出 ComponentDocPage）。
// 本包原先只导出了 ComponentDoc —— 主壳按统一名取会取不到 ⇒ 补一个别名导出。
// 两个名字指向同一个组件，不是两份实现。
export { default as ComponentDocPage } from './pages/ComponentDoc';
export { default as QaNotImplemented } from './QaNotImplemented';
// default 导出：主壳 SystemShell.tsx `import QAApp from '@yuku123/z-qa-component'`
// 走的是默认导入语义（npm 包 0.1.0 同形）；dev LOCAL_SIBLINGS 走这里同一份源码。
import QAAppDefault from './QAApp';
export default QAAppDefault;
