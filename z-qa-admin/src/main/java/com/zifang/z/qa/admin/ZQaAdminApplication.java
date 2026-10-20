package com.zifang.z.qa.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-qa 测试平台启动类。
 * <p>
 * 类必须放在 {@code com.zifang.z.qa.admin} 根包下 —— {@code @SpringBootApplication} 的隐式
 * 组件扫描以本类所在包为根，放进子包 {@code host} 就扫不到兄弟包 {@code config} 里的
 * {@link QaWebMvcConfig}，SPA 回落会静默失效（表现为前端路由刷新全 404）。
 * <p>
 * 本包只扫本模块自己的 config；业务组件由 z-qa-web / z-qa-core 的 META-INF/spring.factories
 * 以 FQCN 注册（EnableAutoConfiguration 键），不靠 @ComponentScan，避免双扫描导致
 * NoUniqueBeanDefinitionException。
 */
@SpringBootApplication
public class ZQaAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZQaAdminApplication.class, args);
    }
}
