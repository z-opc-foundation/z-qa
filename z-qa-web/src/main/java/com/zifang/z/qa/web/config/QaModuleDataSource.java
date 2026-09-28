package com.zifang.z.qa.web.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * z-qa 独立数据源 + SqlSessionFactory.
 * <p>
 * 沿用 z-meta/z-task 等模块的多数据源模式:
 * - 14 个模块各用一个 SqlSessionFactory (dataSourceMeta, dataSourceLc, ...)
 * - 每个模块 @MapperScan 必须指定 sqlSessionFactoryRef
 * <p>
 * z-qa 的数据源复用 main 数据库 (oc), 通过 application.properties 的 z.base.db.qa.* 配置.
 */
@Configuration
@MapperScan(basePackages = "com.zifang.z.qa.admin.domain.mapper", sqlSessionFactoryRef = "sqlSessionFactoryQa")
public class QaModuleDataSource extends ModuleDataSourceTemplate {

    @Bean("dataSourceQa")
    public DataSource dataSource(org.springframework.core.env.Environment env) {
        return buildDataSource(env, "qa");
    }

    @Bean("sqlSessionFactoryQa")
    public MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSourceQa) throws Exception {
        return buildSqlSessionFactory(dataSourceQa);
    }
}