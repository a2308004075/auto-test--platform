/**
 * @author HXN
 * @date 2026-08-22
 * @description Flyway 迁移前自动修复 checksum 的后置处理器
 */
package com.platform.common.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Flyway 迁移前自动修复 checksum。
 * <p>
 * 当迁移脚本文件内容发生变化（如去掉 MySQL 8.0 废弃的整数显示宽度）时，
 * Flyway 会检测到 checksum 不匹配并阻止启动。此处理器在
 * FlywayMigrationInitializer 执行 migrate() 之前调用 repair()，
 * 自动更新 flyway_schema_history 表中的 checksum。
 * </p>
 * <p>
 * 仅在 dev 环境生效：生产环境应通过新增迁移脚本而非修改已应用脚本来变更 schema。
 * </p>
 */
@Slf4j
@Component
@Profile("dev")
public class FlywayRepairPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // Spring Boot 2.7 中 FlywayMigrationInitializer 是包级可见类，无法直接 instanceof
        if (bean.getClass().getName().contains("FlywayMigrationInitializer")) {
            log.info("[FlywayRepair] 检测到 FlywayMigrationInitializer，在迁移前执行 repair() 以同步 checksum");
            try {
                Field flywayField = bean.getClass().getDeclaredField("flyway");
                flywayField.setAccessible(true);
                Flyway flyway = (Flyway) flywayField.get(bean);
                flyway.repair();
                log.info("[FlywayRepair] repair() 执行完成，checksum 已同步");
            } catch (Exception e) {
                log.warn("[FlywayRepair] 执行 repair() 失败: {}", e.getMessage());
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
