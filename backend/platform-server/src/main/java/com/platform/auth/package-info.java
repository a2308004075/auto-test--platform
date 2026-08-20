/**
 * M1 - 认证与用户管理模块
 *
 * <p>职责：用户登录认证、JWT Token 管理、RBAC 权限控制、用户 CRUD、全局配置
 *
 * <p>子包结构：
 * <ul>
 *   <li>controller - AuthController, UserController, SettingsController</li>
 *   <li>service - AuthService, UserService, SettingsService</li>
 *   <li>mapper - UserMapper</li>
 *   <li>entity - User</li>
 *   <li>dto - 请求/响应 DTO</li>
 *   <li>security - JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig</li>
 *   <li>config - WebMvcConfig (CORS)</li>
 * </ul>
 */
package com.platform.auth;
