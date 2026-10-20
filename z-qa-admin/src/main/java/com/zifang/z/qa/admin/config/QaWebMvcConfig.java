package com.zifang.z.qa.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前端 history 路由的 SPA 回落。
 * <p>
 * 只覆盖 /z-qa/** 前缀（前端路由表 pages-manifest.tsx 的全部路径都在此下）。
 * 刻意不做 "/** 转发 index.html"：那会把未匹配的 /api/** 变成 200 text/html，
 * 让接口探测的"全 200"判据失效（前端探针会误判后端可用）。
 * <p>
 * Spring Boot 2.7 默认 PathPatternParser 不允许 `/**` 后再跟模式段，
 * 故按有限深度枚举非资源路径段；含 `.` 的路径（静态资源）不转发。
 */
@Configuration
public class QaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/z-qa").setViewName("forward:/index.html");
        registry.addViewController("/z-qa/{p1:[^\\.]+}").setViewName("forward:/index.html");
        registry.addViewController("/z-qa/{p1:[^\\.]+}/{p2:[^\\.]+}").setViewName("forward:/index.html");
        registry.addViewController("/z-qa/{p1:[^\\.]+}/{p2:[^\\.]+}/{p3:[^\\.]+}")
                .setViewName("forward:/index.html");
    }
}
