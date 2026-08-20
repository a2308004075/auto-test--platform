package com.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 关键字驱动测试管理平台 - Spring Boot 启动类
 */
@SpringBootApplication
@MapperScan("com.platform.**.mapper")
public class PostmanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostmanPlatformApplication.class, args);
    }
}
