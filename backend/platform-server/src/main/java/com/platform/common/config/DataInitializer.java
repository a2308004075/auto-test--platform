/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 数据初始化器
 */
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
 * 负责创建默认 superAdmin 用户等基础数据
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
        User existing = userMapper.selectByUsernameIncludeInactive("superAdmin");
        if (existing != null) {
            log.debug("superAdmin 用户已存在，跳过初始化");
            return;
        }

        // 优先使用 SUPER_ADMIN 角色（V23 迁移），降级使用 ADMIN 角色
        UserRole superAdminRole = userRoleMapper.selectByCode("SUPER_ADMIN");
        UserRole adminRole = (superAdminRole != null) ? superAdminRole : userRoleMapper.selectByCode("ADMIN");
        if (adminRole == null) {
            log.error("SUPER_ADMIN/ADMIN 角色不存在，请检查 user_role 表初始化数据");
            return;
        }

        boolean useSuperAdmin = (superAdminRole != null);
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("superAdmin");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setDisplayName(useSuperAdmin ? "超级管理员" : "管理员");
        admin.setRoleId(adminRole.getId());
        admin.setIsActive(1);
        userMapper.insert(admin);
        log.info("已创建默认 superAdmin 用户（角色：{}，密码：Admin@123）", adminRole.getRoleCode());
    }
}
