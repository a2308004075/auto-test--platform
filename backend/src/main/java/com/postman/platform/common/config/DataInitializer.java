package com.postman.platform.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.postman.platform.auth.entity.User;
import com.postman.platform.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动数据初始化器
 * 负责创建默认 admin 用户等基础数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initAdminUser();
    }

    private void initAdminUser() {
        User existing = userMapper.selectByUsername("admin");
        if (existing != null) {
            log.debug("admin 用户已存在，跳过初始化");
            return;
        }

        User admin = new User();
        admin.setId("00000000-0000-0000-0000-000000000001");
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setDisplayName("管理员");
        admin.setRole("ADMIN");
        admin.setIsActive(true);
        userMapper.insert(admin);
        log.info("已创建默认 admin 用户（密码：admin123）");
    }
}
