package com.zifang.z.qa.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-qa 模块自动配置入口.
 * 扫描 web 包 (controllers) + admin 包 (services).
 * <p>
 * 注意: @MapperScan 在 QaModuleDataSource.java 里 (因为需要指定 sqlSessionFactoryRef).
 */
@Configuration
@ComponentScan(basePackages = {"com.zifang.z.qa.web", "com.zifang.z.qa.admin"})
public class QaAutoConfiguration {
}