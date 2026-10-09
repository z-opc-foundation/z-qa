import React from "react";
import { Alert, Typography } from "antd";

const {Text, Paragraph} = Typography;

/**
 * QA 测试平台「后端未实现」占位（2026-10-04 实测后新增）。
 *
 * 为什么要占位而不是让页面照常渲染：实测这个模块的前端是完整的（13 个源文件、
 * 11 条路由、40 个 `/qa/*` 接口调用），但**后端一处都没有**：
 *   · 运行中的 app（z-opc-local-starter）spec 里与 qa 相关的路径只有 6 条，
 *     且命名空间是 `/api/admin/qa-eval/*`，与前端调的 `/api/qa/*` 完全不同；
 *   · vite 代理把 `/api` 直转 8888 且不 rewrite，说明不是代理写错；
 *   · z-qa 迁出独立仓后，Central 上确实有 `io.github.yuku123:z-qa-web`
 *     （1.0.0 / 1.0.1，latest 1.0.1），但把两个 jar 都解开看过：
 *     整包只有 `QaModuleDataSource` + `QaAutoConfiguration` 两个 class，
 *     **零 Controller** —— 是只留了独立数据源的空壳，业务代码从未发布。
 *
 * 照常渲染的后果是：每次进模块 40 个接口全 404，控制台刷满
 * 「[404] 接口不存在: /qa/...」，Dashboard 还会抛一个**未被捕获**的
 * AxiosError（src/tools/qa/pages/Dashboard.tsx:38 的 Promise.all 没有 catch）。
 * 用户看到的是一个"能点但什么都不工作"的模块，比直接说没实现更糟。
 *
 * 恢复方式：后端把 `/api/qa/**` 真正实现并挂进应用后，本组件的探测会自动通过，
 * 整块 UI 无需任何改动即恢复 —— 所以这是**能力探测**，不是写死的禁用开关。
 */
export const QaNotImplemented: React.FC = () => (
    <div style={{padding: 24}}>
        <Alert
            type="warning"
            showIcon
            title="测试平台后端未实现"
            description={
                <Paragraph style={{marginBottom: 8}}>
                    本页前端（用例 / 套件 / 计划 / 运行 / 环境 / 定时 / E2E 共 11 个页面、
                    40 个接口）已就绪，但服务端**没有对应实现**，所有操作都会 404。
                </Paragraph>
            }
        />
        <Paragraph style={{marginTop: 16}}>
            <Text type="secondary">
                2026-10-04 实测依据：
            </Text>
        </Paragraph>
        <ul style={{color: "rgba(0,0,0,0.45)", paddingLeft: 20, lineHeight: 1.9}}>
            <li>
                <Text type="secondary">
                    运行中的 app 暴露的 qa 相关接口只有 6 条，且都在
                    <Text code>/api/admin/qa-eval/*</Text> 下，与前端调的
                    <Text code>/api/qa/*</Text> 不是同一套
                </Text>
            </li>
            <li>
                <Text type="secondary">
                    vite 代理对 <Text code>/api</Text> 是直转 8888 且不 rewrite，
                    排除「代理路径写错」这一可能
                </Text>
            </li>
            <li>
                <Text type="secondary">
                    Central 上 <Text code>io.github.yuku123:z-qa-web:1.0.1</Text>{" "}
                    解包后只有数据源与自动配置两个类，零 Controller —— 空壳件
                </Text>
            </li>
        </ul>
    </div>
);

export default QaNotImplemented;
