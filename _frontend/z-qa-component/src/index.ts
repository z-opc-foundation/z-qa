export * from './services/api';
export * from './services/stepStatus';
export { default as QAApp } from './QAApp';
export { default as QaNotImplemented } from './QaNotImplemented';
// default 导出：主壳 SystemShell.tsx `import QAApp from '@yuku123/z-qa-component'`
// 走的是默认导入语义（npm 包 0.1.0 同形）；dev LOCAL_SIBLINGS 走这里同一份源码。
import QAAppDefault from './QAApp';
export default QAAppDefault;
