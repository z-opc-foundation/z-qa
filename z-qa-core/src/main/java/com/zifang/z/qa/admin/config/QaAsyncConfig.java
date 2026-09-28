package com.zifang.z.qa.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用异步执行 —— run 的步骤循环跑在 QaRunRunner 的 @Async 线程上，
 * HTTP 请求线程只负责建 run 行并返回。
 */
@Configuration
@EnableAsync
public class QaAsyncConfig {
}