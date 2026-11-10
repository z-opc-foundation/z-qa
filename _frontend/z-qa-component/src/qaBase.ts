import React, {createContext, useContext} from 'react';
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

/**
 * 合成挂载（宿主在无 qa 段的路径上渲染本模块，如 SystemShell ?mode=test 时真实
 * pathname 仍是 /workspace）下 pathname 推断失效，QAApp 把显式 mountPath 放进
 * 这里的 context，页面内导航统一从 {@link useQaBase} 取值。
 */
export const QaBaseContext = createContext<string | null>(null);

/** 页面内使用：优先取 QAApp 声明的挂载前缀，推断不到再从 pathname 反推。 */
export function useQaBase(): string {
    const ctx = useContext(QaBaseContext);
    const {pathname} = useLocation();
    return ctx ?? qaBase(pathname);
}

export function QaBaseProvider({base, children}: { base: string, children: React.ReactNode }) {
    return React.createElement(QaBaseContext.Provider, {value: base}, children);
}
