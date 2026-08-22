package com.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 关键字驱动测试管理平台 - Spring Boot 启动类
 *
 * 排除 UserDetailsServiceAutoConfiguration：本项目基于 JWT 自定义认证
 * （JwtAuthenticationFilter + UserMapper），不依赖 Spring Security 默认的
 * InMemoryUserDetailsManager，排除后可消除启动时生成随机密码的告警。
 */
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@MapperScan("com.platform.**.mapper")
public class PostmanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostmanPlatformApplication.class, args);
    }
}
