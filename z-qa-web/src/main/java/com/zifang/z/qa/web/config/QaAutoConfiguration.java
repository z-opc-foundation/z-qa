package com.zifang.z.qa.web.config;

import org.springframework.context.annotation.Configuration;

/**
 * z-qa 模块自动配置入口。
 *
 * <p>刻意不加 {@code @ComponentScan}（lead 007 §4.2）：宿主可能也扫到本包，双注册会让
 * 同类型 bean 出现两份。全部 controller / service impl 改由
 * {@code META-INF/spring.factories} 以 FQCN 显式注册。
 *
 * <p>{@code @MapperScan} 在 {@link QaModuleDataSource}（需指定 sqlSessionFactoryRef）。
 */
@Configuration
public class QaAutoConfiguration {
}
