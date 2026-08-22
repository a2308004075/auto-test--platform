/**
 * @author HXN
 * @date 2026-08-20 10:05
 * @description common 模块包说明
 */
/**
 * 公共基础设施模块
 *
 * <p>职责：为所有业务模块（auth / project / api / keyword / execution）提供统一的
 * 响应格式、全局异常处理、错误码体系、数据库实体基类、分页响应、通用配置和工具类。
 *
 * <p>子包结构：
 * <ul>
 *   <li>config - MyBatisPlusConfig, RedisConfig, RabbitMQConfig</li>
 *   <li>constant - PasswordPolicy（密码策略常量）</li>
 *   <li>entity - BaseEntity（数据库实体基类）</li>
 *   <li>exception - BusinessException, DependencyException, NotFoundException, ErrorCode, GlobalExceptionHandler</li>
 *   <li>response - ApiResponse, PageResponse</li>
 *   <li>util - JsonUtils</li>
 * </ul>
 */
package com.platform.common;
