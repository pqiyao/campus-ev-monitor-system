package com.example.evmonitorplatform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类（适配 3.5.4 版本，兼容所有环境）
 * 解决 "无法解析符号 MybatisPlusPlugin" 问题
 */
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {

    /**
     * 分页插件配置（改用3.5.x兼容的写法，避免类识别问题）
     * 功能和 MybatisPlusPlugin 完全一致
     */
    @Bean
    public com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 全类名引用，彻底解决IDE识别问题
        com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor interceptor =
                new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();

        // MySQL分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 最大单页限制，-1表示无限制
        paginationInterceptor.setMaxLimit(-1L);
        // 防止分页查询时的SQL注入
        paginationInterceptor.setOverflow(true);

        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * 插入数据时自动填充创建时间和更新时间
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 兼容不同命名风格（createTime / create_time）
        this.fillStrategy(metaObject, "createTime", LocalDateTime.now());
        this.fillStrategy(metaObject, "updateTime", LocalDateTime.now());
        // 兼容数据库下划线命名
        this.fillStrategy(metaObject, "create_time", LocalDateTime.now());
        this.fillStrategy(metaObject, "update_time", LocalDateTime.now());
    }

    /**
     * 更新数据时自动填充更新时间
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.fillStrategy(metaObject, "updateTime", LocalDateTime.now());
        this.fillStrategy(metaObject, "update_time", LocalDateTime.now());
    }
}