package com.platform.common.config;

import com.platform.auth.entity.User;
import com.platform.auth.entity.UserRole;
import com.platform.auth.mapper.UserMapper;
import com.platform.auth.mapper.UserRoleMapper;
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
    private final UserRoleMapper userRoleMapper;
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

        // 确认 ADMIN 角色存在
        UserRole adminRole = userRoleMapper.selectByCode("ADMIN");
        if (adminRole == null) {
            log.error("ADMIN 角色不存在，请检查 user_role 表初始化数据");
            return;
        }

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setDisplayName("管理员");
        admin.setRoleId(adminRole.getId());
        admin.setIsActive(1);
        userMapper.insert(admin);
        log.info("已创建默认 admin 用户（密码：admin123）");
    }
}
