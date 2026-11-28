/**
 * z-qa 子页菜单清单（2026-11-28 CEO「全部补充回来」）。
 *
 * 为什么单独成一个零依赖入口（dist/menu.js）：宿主侧栏（产研面 /product/qa/*、
 * 系统面 /test/qa/*）要引用这份菜单，但 z-qa 的页面代码体量不小 ——
 * 从包根或 ./pages 拿都会把 QAApp 连带拖进宿主主 chunk。
 * 这个入口只含纯数据，宿主 import 它不沾任何 React 代码。
 *
 * key 与 QAApp 内 <Routes> 的分发段一一对应（见 QAApp.tsx 的 switch(head)），
 * 宿主拼前缀即可：/product/qa/cases、/test/qa/cases 都命中同一个页面。
 */
export interface QaSubMenuItem {
    /** 相对挂载前缀的子路径段（与 QAApp 路由分发段一致） */
    key: string;
    label: string;
    title?: string;
}

export const QA_SUB_MENU: QaSubMenuItem[] = [
    {key: 'dashboard', label: 'QA 总览', title: '测试统计、趋势与套件健康度'},
    {key: 'cases', label: '测试用例', title: '接口用例与断言模板'},
    {key: 'suites', label: '测试套件', title: '步骤编排与变量传递'},
    {key: 'plans', label: '测试计划', title: '套件组合与环境绑定'},
    {key: 'runs', label: '执行记录', title: '执行触发、状态与报告'},
    {key: 'schedules', label: '定时调度', title: 'cron 定时执行'},
    {key: 'e2e', label: 'E2E 测试', title: '浏览器自动化测试'},
    {key: 'envs', label: '环境管理', title: '多环境变量管理'},
];
