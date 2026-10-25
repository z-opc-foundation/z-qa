import {useLocation} from 'react-router-dom';

/**
 * 从当前 pathname 反推本组件的挂载前缀（/test/qa 或 /z-qa）。
 *
 * <p>宿主（Z-Team SystemShell）按 pathname 直接渲染本组件，不提供嵌套 Route
 * 上下文；因此页面内的跳转不能用绝对路径 {@code /z-qa/...}，必须由当前
 * pathname 推导挂载前缀后再拼子路径，否则会跳出本模块落到宿主 404。
 */
export function qaBase(pathname: string): string {
    const m = pathname.match(/^(.*?)\/(?:z-)?qa(?=\/|$)/);
    return m ? m[0] : '';
}

/** 页面内使用：返回当前挂载前缀（如 /test/qa）。 */
export function useQaBase(): string {
    const {pathname} = useLocation();
    return qaBase(pathname);
}
