package com.example.evmonitorplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 关键：指定你的 Mapper 接口所在的包路径
@MapperScan("com.example.evmonitorplatform.mapper")
@SpringBootApplication
public class EvMonitorPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvMonitorPlatformApplication.class, args);
    }
}