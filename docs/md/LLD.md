# auto-test-platform 详细设计文档

> 版本：v1.0  
> 文档状态：初稿  
> 基线文档：[SRS v1.3](SRS.md) | [PRD V1.17](PRD.md) | [HLD v1.0](HLD.md) | [UI 原型](../ui/index.html)

---

## 1. 引言

### 1.1 文档目的

本文档是 auto-test-platform 的详细设计文档（Low-Level Design），在概要设计文档（HLD）的基础上，对每个模块进行内部架构设计、类与接口签名定义、数据库完整 DDL、关键算法伪代码、错误处理和测试边界描述，为编码实现提供直接指导。

### 1.2 基线文档

| 文档 | 版本 | 关系 |
|---|---|---|
| [需求规格说明书 SRS](SRS.md) | v1.3 | 数据模型、引擎复用方案、非功能需求的权威来源 |
| [产品需求文档 PRD](PRD.md) | V1.17 | 业务规则、操作步骤、字段说明、接口协议的权威来源 |
| [概要设计文档 HLD](HLD.md) | v1.0 | 模块划分、依赖关系、接口契约的直接上游 |
| [UI 原型](../ui/index.html) | v1.0 | 26 个核心页面的 HTML 高保真原型 |

### 1.3 术语约定

沿用 HLD §1.4 的术语定义。本文新增以下设计级术语：

| 术语 | 含义 |
|---|---|
| Mapper | MyBatis-Plus Mapper 接口，每个表对应一个 Mapper，通过 Spring 依赖注入获取 |
| DTO | 请求/响应的数据传输对象，使用 javax.validation 注解进行校验 |
| Service | 业务服务层类，封装领域逻辑，通过 @Service 注解和 Spring DI 使用 |
| Engine | execution 模块内置的执行引擎包，负责关键字执行、HTTP 客户端、断言引擎、Groovy 沙箱、Swagger 解析、Action 流程执行 |
| Execution Context | 运行时变量上下文，存储 save_as 变量和步骤执行结果 |

### 1.4 设计约定

| 约定项 | 规则 |
|---|---|
| 命名规范 | 类名 PascalCase，方法名 camelCase，常量 UPPER_SNAKE_CASE，路由路径 kebab-case |
| 分层架构 | Controller → Service → Mapper 三层，Controller 仅做参数校验和响应封装；模块间通过 Spring Bean 依赖注入直接调用 |
| 依赖注入 | 使用 Spring @Autowired / 构造器注入，注入 Mapper、Service、当前用户等 |
| 统一响应 | 所有 API 返回 `{ "code": 0, "message": "success", "data": {} }` 格式 |
| 分页格式 | `{ "items": [], "total": N, "page": 1, "page_size": 20 }` |
| 错误码体系 | 业务错误码以模块前缀编号，HTTP 409 表示依赖冲突（删除保护） |
| UUID 主键 | 所有业务表使用 UUID v4 作为主键，由数据库自动生成 |
| 软删除 | Project、User 等核心实体采用 `is_active` 软删除，不物理删除 |
| JSON 字段 | 复杂嵌套结构（steps、nodes、config 等）使用 MySQL 原生 JSON 类型，通过 MyBatis-Plus TypeHandler 映射 |

---

## 2. 公共基础设施设计

### 2.1 项目目录结构

```
auto-test-platform/
├── frontend/                              # 前端项目（Vue 3 + TypeScript + Vite）
│   ├── src/
│   │   ├── api/                           # API 接口封装（按模块分文件）
│   │   │   ├── request.ts                 #   Axios 实例 + 拦截器
│   │   │   ├── auth.ts
│   │   │   ├── project.ts
│   │   │   ├── user.ts
│   │   │   ├── apidoc.ts
│   │   │   ├── environment.ts
│   │   │   ├── keyword.ts
│   │   │   ├── tool.ts
│   │   │   ├── action.ts
│   │   │   ├── autoSuite.ts
│   │   │   ├── autoCase.ts
│   │   │   ├── plan.ts
│   │   │   ├── execution.ts
│   │   │   └── settings.ts
│   │   ├── assets/                        # 静态资源（图片、图标）
│   │   ├── components/                    # 通用组件
│   │   │   ├── Breadcrumb/index.vue       #   面包屑导航组件
│   │   │   └── Hamburger/index.vue        #   侧边栏折叠按钮组件
│   │   ├── composables/                   # Composition API hooks
│   │   │   └── useExecutionWebSocket.ts   #   执行 WebSocket 连接管理
│   │   ├── layouts/                       # 布局组件
│   │   │   ├── Layout.vue                 #   主布局编排容器（Sidebar + 主区）
│   │   │   └── components/                #   布局子组件
│   │   │       ├── Sidebar.vue            #     侧边栏
│   │   │       ├── Navbar.vue             #     顶部导航栏
│   │   │       ├── AppMain.vue            #     主内容区
│   │   │       └── TagsView/              #     标签页导航
│   │   ├── router/                        # 路由配置
│   │   │   └── index.ts
│   │   ├── stores/                        # Pinia 状态管理
│   │   │   ├── index.ts                   #   统一导出
│   │   │   └── modules/                   #   按功能分模块
│   │   │       ├── app.ts                 #     应用全局状态（侧边栏等）
│   │   │       ├── user.ts                #     用户认证状态
│   │   │       ├── project.ts             #     当前项目上下文
│   │   │       ├── permission.ts          #     权限管理（桩）
│   │   │       └── tagsView.ts            #     标签页状态
│   │   ├── styles/                        # 全局样式
│   │   │   ├── global.less                #   全局样式
│   │   │   ├── variables.less             #   CSS 变量
│   │   │   ├── sidebar.less               #   侧边栏样式
│   │   │   ├── scrollbar.less             #   滚动条样式
│   │   │   ├── transition.less            #   过渡动画
│   │   │   └── element-plus.less          #   Element Plus 主题覆盖
│   │   ├── views/                         # 页面视图
│   │   │   ├── auth/                      #   登录页
│   │   │   ├── project/                   #   项目管理页面
│   │   │   ├── api/                       #   接口文档页面
│   │   │   ├── environment/               #   环境配置页面
│   │   │   ├── keywords/                  #   接口关键字页面
│   │   │   ├── tool/                      #   工具方法页面
│   │   │   ├── action/                    #   Action 关键字页面
│   │   │   ├── cases/                     #   自动化套件/自动化用例页面
│   │   │   ├── execution/                 #   测试计划/执行记录页面
│   │   │   └── settings/                  #   系统设置页面
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── backend/                               # 后端服务（Maven 多模块聚合）
│   ├── pom.xml                            #   父 POM（packaging=pom, 聚合三模块）
│   ├── platform-api/                      #   契约层（DTO/响应/异常/基类）
│   │   ├── pom.xml
│   │   └── src/main/java/com/platform/
│   │       ├── common/
│   │       │   ├── response/              #   ApiResponse, PageResponse
│   │       │   ├── exception/             #   ErrorCode, BusinessException, DependencyException, NotFoundException
│   │       │   ├── entity/                #   BaseEntity（id/createdAt/updatedAt/isActive）
│   │       │   └── util/                  #   JsonUtils, SpringContextHolder
│   │       └── {feature}/dto/             #   action/apidoc/auth/environment/execution/keyword/project/tool 的 DTO
│   ├── platform-data/                     #   持久层（Entity + Mapper）
│   │   ├── pom.xml
│   │   └── src/main/java/com/platform/
│   │       └── {feature}/                 #   api/auth/environment/execution/keyword/project 的 entity + mapper
│   └── platform-server/                   #   应用层（Controller/Service/Config/引擎）
│       ├── pom.xml
│       ├── src/main/java/com/platform/
│       │   ├── PostmanPlatformApplication.java  # Spring Boot 启动类 + @MapperScan
│       │   ├── common/
│       │   │   ├── config/                #   MyBatisPlusConfig, RedisConfig, RabbitMQConfig, DataInitializer
│       │   │   └── exception/             #   GlobalExceptionHandler
│       │   ├── filter/                    #   CorsFilter
│       │   ├── auth/                      #   认证模块（M1）— controller/service/security/config
│       │   ├── project/                   #   项目管理模块（M2/M3）— controller/service
│       │   ├── environment/               #   环境配置模块 — controller/service
│       │   ├── apidoc/                    #   接口文档模块（M4）— controller/service/util
│       │   ├── keyword/                   #   关键字管理模块（M5/M6/M7）— controller/service
│       │   ├── tool/                      #   工具方法模块 — controller/service
│       │   ├── action/                    #   Action 模块 — controller/service
│       │   └── execution/                 #   执行与报告模块（M8/M9/M10）
│       │       ├── controller/
│       │       ├── service/
│       │       ├── engine/                #     执行引擎核心包
│       │       ├── context/               #     执行上下文
│       │       ├── mq/                    #     RabbitMQ 消息生产/消费
│       │       ├── websocket/             #     WebSocket 实时推送
│       │       └── config/                #     异步/WebSocket 配置
│       └── src/main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/              #     Flyway 迁移脚本（V1/V2/V3）
│
├── docs/                                  # 文档
│   ├── md/                                #   PRD/SRS/HLD/LLD/DDP 设计文档
│   ├── ui/                                #   UI 原型 HTML
│   ├── script/                            #   中间件启停脚本
│   └── vbPrompt/                          #   AI 提示词模板
│
└── .qoder/rules/                          # 开发规范规则文件
```

> **多模块依赖方向**：`platform-server` → `platform-data` → `platform-api`  
> **Maven groupId**：`com.postman`（POM 配置）；**Java 包路径**：`com.platform`（源码包声明）

### 2.2 数据库连接与连接池

> 后端单体应用使用同一个 MySQL 实例，通过 application.yml 统一管理数据库连接参数。以下为公共配置模板：

```yaml
# application.yml 配置

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:auto_test_platform}?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: ${DB_USER:root}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20              # 连接池最大连接数
      minimum-idle: 5                    # 最小空闲连接
      connection-timeout: 30000          # 获取连接超时（毫秒）
      idle-timeout: 600000               # 空闲连接超时（10分钟）
      max-lifetime: 1800000              # 连接最大生命周期（30分钟，避免 MySQL 8h 空闲断连）
      connection-test-query: SELECT 1    # 连接存活性检测

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: assign_uuid               # UUID 主键策略
      logic-delete-field: isActive        # 软删除字段
      logic-delete-value: 0
      logic-not-delete-value: 1
```

**设计要点：**
- 使用 HikariCP 连接池（Spring Boot 默认），高性能、低延迟
- 数据库连接参数通过 application.yml 配置，支持 `${DB_*}` 环境变量覆盖
- `max-lifetime` 设置为 30 分钟，远低于 MySQL 8 小时空闲超时，避免连接被服务端主动断开
- MyBatis-Plus 全局配置 UUID 主键、驼峰映射和软删除（common 包统一配置）

### 2.3 统一响应格式与错误码

```java
// src/main/java/com/platform/dto/ApiResponse.java

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
```

**全局错误码定义：**

| 错误码范围 | 模块 | 说明 |
|---|---|---|
| 1000-1099 | 公共 | 通用错误（参数校验、认证、权限） |
| 1100-1199 | M1 认证与用户 | 登录失败、Token 过期、用户不存在、账号保留 |
| 1200-1299 | M2 项目管理 | 项目名重复、项目已停用 |
| 1300-1399 | M3 环境配置 | 环境名重复、JSON 校验失败、连接测试失败 |
| 1400-1499 | M4 接口文档 | 接口路径重复、Swagger 解析失败、依赖冲突 |
| 1500-1599 | M5 接口关键字 | 关键字名重复、测试数据校验失败 |
| 1600-1699 | M6 工具方法 | 沙箱执行超时、代码安全检查失败 |
| 1700-1799 | M7 Action | 节点序列化失败、循环引用检测 |
| 1800-1899 | M8 自动化用例 | 步骤校验失败、参数化数据格式错误 |
| 1900-1999 | M9 测试执行 | 执行任务已满、计划未绑定环境 |
| 2000-2099 | M10 报告分析 | 报告生成失败、导出超时 |

**公共错误码明细：**

| 错误码 | HTTP 状态码 | 含义 |
|---|---|---|
| 1001 | 400 | 请求参数校验失败 |
| 1002 | 401 | 未认证（Token 缺失或无效） |
| 1003 | 401 | Access Token 已过期 |
| 1004 | 401 | Refresh Token 已过期 |
| 1005 | 403 | 权限不足（RBAC 校验失败） |
| 1006 | 404 | 资源不存在 |
| 1007 | 409 | 资源冲突（名称重复等） |
| 1008 | 500 | 服务器内部错误 |

### 2.4 认证中间件

> 认证通过全局过滤器 JwtAuthenticationFilter 负责 JWT 解析和验证，auth 模块负责 Token 签发/刷新和用户管理。

```java
// backend/platform-server/src/main/java/com/platform/auth/security/JwtTokenProvider.java

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    // Token 有效期：登录时读取全局配置 session.login_validity_days（天，所有用户统一，默认 5 天），
    // Access 与 Refresh Token 采用同一有效期；配置缺失或非法时回退默认 5 天

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String createAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .claim("type", "access")
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public String createRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("type", "refresh")
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

```java
// backend/platform-server/src/main/java/com/platform/auth/security/JwtAuthenticationFilter.java

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String token = resolveToken(request);
        if (token != null) {
            Claims claims = jwtTokenProvider.parseToken(token);
            if ("access".equals(claims.get("type"))) {
                String userId = claims.getSubject();
                User user = userService.findActiveById(userId);
                if (user != null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
```

### 2.5 全局异常处理器

```java
// backend/platform-server/src/main/java/com/platform/auth/security/SecurityConfig.java

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(DependencyException.class)
    public ResponseEntity<ApiResponse<Object>> handleDependency(DependencyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(1007, e.getMessage(), e.getDependencies()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return ResponseEntity.badRequest().body(ApiResponse.error(1001, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        log.error("Unexpected error", e);  // 生产环境记录日志，不暴露内部错误
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(1008, "服务器内部错误"));
    }
}
```

```java
// backend/platform-server/src/main/java/com/platform/common/exception/BusinessException.java

public class BusinessException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public BusinessException(int code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

public class DependencyException extends BusinessException {
    private final Object dependencies;

    public DependencyException(String message, Object dependencies) {
        super(1007, message, 409);
        this.dependencies = dependencies;
    }
}

public class NotFoundException extends BusinessException {
    public NotFoundException(String resource, String id) {
        super(1006, resource + " not found" + (id != null ? " (id=" + id + ")" : ""), 404);
    }
}
```

### 2.6 数据库实体基类

```java
// backend/platform-server/src/main/java/com/platform/common/entity/BaseEntity.java

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Boolean isActive;
}
```

### 2.7 Axios 请求拦截器（前端）

```typescript
// frontend/src/utils/request.ts

import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

// 请求拦截器：自动注入 Authorization Header
request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// 响应拦截器：Token 过期自动刷新、统一错误处理
request.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const auth = useAuthStore()
      try {
        await auth.refreshToken()                  // 用 Refresh Token 换取新 Access Token
        originalRequest.headers.Authorization = `Bearer ${auth.accessToken}`
        return request(originalRequest)            // 重试原始请求
      } catch {
        auth.logout()                              // Refresh Token 也过期，跳转登录
        return Promise.reject(error)
      }
    }
    const { code, message: msg } = error.response?.data || {}
    message.error(msg || '请求失败')
    return Promise.reject(error)
  }
)

export default request
```

### 2.8 WebSocket 连接管理（前端）

```typescript
// frontend/src/composables/useWebSocket.ts

import { ref, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useWebSocket(url: string) {
  const isConnected = ref(false)
  const messages = ref<any[]>([])
  let ws: WebSocket | null = null

  function connect() {
    const auth = useAuthStore()
    const fullUrl = `${import.meta.env.VITE_WS_URL}${url}?token=${auth.accessToken}`
    ws = new WebSocket(fullUrl)

    ws.onopen = () => { isConnected.value = true }
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      messages.value.push(data)
    }
    ws.onclose = () => { isConnected.value = false }
    ws.onerror = (err) => { console.error('WebSocket error:', err) }
  }

  function disconnect() {
    ws?.close()
    ws = null
  }

  onUnmounted(() => disconnect())

  return { isConnected, messages, connect, disconnect }
}
```

### 2.9 数据库迁移策略

使用 Flyway 管理数据库 Schema 变更：

```bash
# 迁移脚本命名规范（放在 backend/src/main/resources/db/migration/ 目录）
V1__create_user_table.sql
V2__create_global_settings_table.sql
V3__create_project_table.sql
...

# Spring Boot 启动时自动执行未应用的迁移脚本
# 也可手动执行：
mvn flyway:migrate

# 查看迁移状态
mvn flyway:info

# 修复失败的迁移
mvn flyway:repair
```

**迁移规范：**
- 脚本命名格式：`V{version}__{description}.sql`，版本号递增
- 每次 Schema 变更必须生成独立的迁移脚本
- 生产环境禁止自动迁移，必须通过 CI/CD 流程手动触发
- 大表变更（添加索引、修改字段类型）使用 `pt-online-schema-change` 或分批执行


---

## 3. M1 — 认证与用户管理模块

### 3.1 模块内部架构

```
┌────────────────────────────────────────────────────────────────┐
│                    M1 认证与用户管理                              │
│                                                                │
│  ┌─── API 路由层 ──────────────────────────────────────────┐   │
│  │ AuthController (login/refresh/logout/me)                 │   │
│  │ UserController (CRUD/status/reset-password)              │   │
│  │ SettingsController (GET/PUT 全局配置)                    │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           ▼                                    │
│  ┌─── 业务服务层 ──────────────────────────────────────────┐   │
│  │ AuthService: 登录验证、Token 生成与刷新、登出标记          │   │
│  │ UserService: 用户 CRUD、保留规则校验、admin 保护           │   │
│  │ SettingsService: 全局配置读写                             │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           ▼                                    │
│  ┌─── 数据层 ──────────────────────────────────────────────┐   │
│  │ UserMapper │ GlobalSettingsMapper │ TokenBlacklistMapper  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                │
│  ┌─── 横切关注点 ──────────────────────────────────────────┐   │
│  │ JWT 中间件 │ RBAC 权限守卫 │ 密码哈希 │ Token 黑名单     │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

### 3.2 数据模型 DDL

```sql
-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE `user` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `username`        VARCHAR(50)   NOT NULL                    COMMENT '账号（登录名）',
  `password_hash`   VARCHAR(255)  NOT NULL                    COMMENT 'bcrypt 哈希密码',
  `display_name`    VARCHAR(50)   NOT NULL                    COMMENT '用户姓名（显示名）',
  `role`            ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER' COMMENT '角色',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `last_login_at`   DATETIME      DEFAULT NULL                COMMENT '最近登录时间',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 全局配置表
-- ============================================================
CREATE TABLE `global_settings` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `config_key`      VARCHAR(100)  NOT NULL                    COMMENT '配置键',
  `config_value`    JSON          NOT NULL                    COMMENT '配置值',
  `description`     VARCHAR(255)  DEFAULT NULL                COMMENT '配置说明',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局配置表';

-- ============================================================
-- Token 黑名单表（用于登出后使 Token 失效）
-- ============================================================
CREATE TABLE `token_blacklist` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `token_jti`       VARCHAR(100)  NOT NULL                    COMMENT 'Token 唯一标识（JWT ID）',
  `user_id`         CHAR(36)      NOT NULL                    COMMENT '用户 ID',
  `expires_at`      DATETIME      NOT NULL                    COMMENT 'Token 原始过期时间（用于定期清理）',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_jti` (`token_jti`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token 黑名单';
```

**初始化数据：**

```sql
-- 创建默认 admin 账号（密码：admin123）
INSERT INTO `user` (`id`, `username`, `password_hash`, `display_name`, `role`, `is_active`)
VALUES (UUID(), 'admin', '$2b$12$LJ3m4ys3Gl.KjU1cPJlqNO...', '管理员', 'ADMIN', TRUE);

-- 初始化全局配置
INSERT INTO `global_settings` (`id`, `config_key`, `config_value`, `description`) VALUES
(UUID(), 'execution_timeout', '{"seconds": 30}', '单自动化用例执行超时默认值'),
(UUID(), 'concurrency_limit', '{"limit": 1}', '同一项目并发执行上限'),
(UUID(), 'log_retention_days', '{"days": 90}', '执行日志保留天数'),
(UUID(), 'report_retention_days', '{"days": 90}', '报告数据保留天数');
```

### 3.3 服务层设计

> 以下代码示例使用 Python 伪代码风格展示业务逻辑和流程设计，实际实现请使用 Java + Spring Boot + MyBatis-Plus 技术栈，遵循 Controller → Service → Mapper 三层架构。

```python
# backend/app/services/auth_service.py

class AuthService:
    """认证服务：登录、Token 刷新、登出"""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def login(self, username: str, password: str) -> LoginResult:
        """
        登录验证流程：
        1. 根据 username 查询用户
        2. 校验用户是否启用（is_active）
        3. bcrypt 验证密码
        4. 生成 Access Token + Refresh Token
        5. 更新 last_login_at
        """
        user = await self._find_user_by_username(username)
        if not user:
            raise BusinessError(1101, "用户名或密码错误", http_status=401)
        if not user.is_active:
            raise BusinessError(1102, "账号已被禁用", http_status=403)
        if not verify_password(password, user.password_hash):
            raise BusinessError(1101, "用户名或密码错误", http_status=401)

        access_token = create_access_token(user.id, user.role)
        refresh_token = create_refresh_token(user.id)
        user.last_login_at = datetime.utcnow()
        await self.db.flush()

        return LoginResult(
            access_token=access_token, refresh_token=refresh_token,
            expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            user=UserBrief(id=user.id, username=user.username,
                          display_name=user.display_name, role=user.role))

    async def refresh_token(self, refresh_token_str: str) -> TokenResult:
        """
        Token 刷新流程：
        1. 解码 Refresh Token，验证类型和有效期
        2. 验证 Token 未被加入黑名单
        3. 查询用户是否仍然有效
        4. 生成新的 Access Token
        """
        try:
            payload = decode_token(refresh_token_str)
        except JWTError:
            raise BusinessError(1004, "Refresh Token 已过期", http_status=401)
        if payload.get("type") != "refresh":
            raise BusinessError(1002, "Token 类型无效", http_status=401)
        if await self._is_token_blacklisted(payload.get("jti")):
            raise BusinessError(1004, "Token 已失效", http_status=401)

        user = await self._find_user_by_id(payload.get("sub"))
        if not user or not user.is_active:
            raise BusinessError(1102, "用户不存在或已禁用", http_status=401)
        new_token = create_access_token(user.id, user.role)
        return TokenResult(access_token=new_token, expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60)

    async def logout(self, token_jti: str, expires_at: datetime) -> None:
        """登出：将当前 Token 加入黑名单"""
        self.db.add(TokenBlacklist(token_jti=token_jti, user_id=self.current_user_id, expires_at=expires_at))
        await self.db.flush()
```

```python
# backend/app/services/user_service.py

class UserService:
    """用户管理服务：CRUD、保留规则校验、admin 保护"""

    RESERVED_USERNAME = "admin"          # 账号保留（不区分大小写）
    RESERVED_DISPLAY_NAME = "管理员"      # 用户名保留

    async def create_user(self, data: UserCreateSchema) -> User:
        """
        创建用户流程：
        1. 校验 username 保留规则（不能使用 admin，不区分大小写）
        2. 校验 display_name 保留规则（不能使用「管理员」）
        3. 校验 username 唯一性
        4. bcrypt 哈希密码
        5. 写入数据库
        """
        self._validate_reserved_username(data.username)
        self._validate_reserved_display_name(data.display_name)
        # ... 创建逻辑

    async def update_user(self, user_id: str, data: UserUpdateSchema) -> User:
        """
        更新用户流程：
        1. 查询目标用户
        2. admin 账号保护：不允许修改 username / role / is_active
        3. 若修改 username → 校验保留规则
        4. 若修改 display_name → 校验保留规则
        """
        target = await self._find_user(user_id)
        if target.username == self.RESERVED_USERNAME:
            if data.display_name and data.display_name != target.display_name:
                raise BusinessError(1103, "admin 账号不允许修改用户名")
            if data.role and data.role != target.role:
                raise BusinessError(1104, "admin 账号不允许修改角色")

    async def delete_user(self, user_id: str) -> None:
        """删除用户：admin 账号不可删除"""
        target = await self._find_user(user_id)
        if target.username == self.RESERVED_USERNAME:
            raise BusinessError(1105, "系统内置管理员账号不可删除")
        await self.db.delete(target)

    def _validate_reserved_username(self, username: str) -> None:
        if username.lower() == self.RESERVED_USERNAME.lower():
            raise BusinessError(1106, "账号 'admin' 为系统保留，不可使用")

    def _validate_reserved_display_name(self, display_name: str) -> None:
        if display_name == self.RESERVED_DISPLAY_NAME:
            raise BusinessError(1107, "用户名「管理员」为系统保留，不可使用")
```

### 3.4 API 处理函数签名

```python
# backend/app/api/v1/auth.py
router = APIRouter(prefix="/auth", tags=["认证"])

@router.post("/login")
async def login(body: LoginRequest, db: AsyncSession = Depends(get_db)) -> dict:
    """用户登录，返回双 Token"""

@router.post("/refresh")
async def refresh_token(body: RefreshRequest, db: AsyncSession = Depends(get_db)) -> dict:
    """刷新 Access Token"""

@router.post("/logout")
async def logout(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)) -> dict:
    """登出，标记当前 Token 失效"""

@router.get("/me")
async def get_current_user_info(current_user: User = Depends(get_current_user)) -> dict:
    """返回当前登录用户信息"""
```

```python
# backend/app/api/v1/users.py
router = APIRouter(prefix="/users", tags=["用户管理"])

@router.get("")
async def list_users(keyword: str = None, role: str = None, page: int = 1, page_size: int = 20,
                     current_user: User = Depends(require_admin), db: AsyncSession = Depends(get_db)) -> dict:
    """用户列表（仅 ADMIN）"""

@router.post("")
async def create_user(body: UserCreateSchema, current_user: User = Depends(require_admin),
                      db: AsyncSession = Depends(get_db)) -> dict:
    """创建用户"""

@router.put("/{user_id}")
async def update_user(user_id: str, body: UserUpdateSchema, current_user: User = Depends(require_admin),
                      db: AsyncSession = Depends(get_db)) -> dict:
    """更新用户"""

@router.delete("/{user_id}")
async def delete_user(user_id: str, current_user: User = Depends(require_admin),
                      db: AsyncSession = Depends(get_db)) -> dict:
    """删除用户（admin 不可删）"""

@router.patch("/{user_id}/status")
async def toggle_user_status(user_id: str, body: StatusToggleSchema,
                              current_user: User = Depends(require_admin), db: AsyncSession = Depends(get_db)) -> dict:
    """启用/禁用用户"""

@router.post("/{user_id}/reset-password")
async def reset_password(user_id: str, body: ResetPasswordSchema,
                          current_user: User = Depends(require_admin), db: AsyncSession = Depends(get_db)) -> dict:
    """管理员重置用户密码"""
```

### 3.5 Pydantic Schema

```python
# backend/app/schemas/auth.py
class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1, max_length=50)
    password: str = Field(..., min_length=8, max_length=128)

class LoginResponse(BaseModel):
    access_token: str
    refresh_token: str
    expires_in: int
    user: UserBrief

class UserBrief(BaseModel):
    id: str
    username: str
    display_name: str
    role: str

class RefreshRequest(BaseModel):
    refresh_token: str
```

```python
# backend/app/schemas/user.py
class UserCreateSchema(BaseModel):
    username: str = Field(..., min_length=1, max_length=50)
    display_name: str = Field(..., min_length=1, max_length=50)
    password: str = Field(..., min_length=8, max_length=128)
    role: str = Field("USER", pattern="^(ADMIN|USER)$")

class UserUpdateSchema(BaseModel):
    display_name: Optional[str] = Field(None, min_length=1, max_length=50)
    role: Optional[str] = Field(None, pattern="^(ADMIN|USER)$")

class UserResponse(BaseModel):
    id: str
    username: str
    display_name: str
    role: str
    is_active: bool
    last_login_at: Optional[datetime]
    created_at: datetime
```

### 3.6 错误码明细

| 错误码 | HTTP | 触发场景 |
|---|---|---|
| 1101 | 401 | 用户名或密码错误 |
| 1102 | 403 | 账号已被禁用 |
| 1103 | 400 | admin 账号不允许修改用户名 |
| 1104 | 400 | admin 账号不允许修改角色 |
| 1105 | 400 | 系统内置管理员账号不可删除 |
| 1106 | 400 | 账号 'admin' 为系统保留 |
| 1107 | 400 | 用户名「管理员」为系统保留 |
| 1108 | 400 | admin 账号不允许禁用 |

### 3.7 测试边界

| 测试场景 | 验证点 |
|---|---|
| 正常登录 | 返回双 Token + 用户信息，last_login_at 更新 |
| 错误密码登录 | 返回 401，不泄露"用户存在"信息 |
| 禁用账号登录 | 返回 403 + 明确提示 |
| Token 过期自动刷新 | 前端拦截器自动调用 refresh，用户无感知 |
| admin 账号保护 | 修改用户名/角色/禁用/删除均返回 400 |
| 保留规则校验 | 新建用户 username=Admin（大小写）返回 400 |
| RBAC 权限 | USER 角色访问 /users 接口返回 403 |
| 密码强度 | 少于 8 位返回 Pydantic 校验错误 |

---

## 4. M2 — 项目管理模块

### 4.1 模块内部架构

```
┌────────────────────────────────────────────────────────────────┐
│                    M2 项目管理                                   │
│                                                                │
│  ┌─── API 路由层 ──────────────────────────────────────────┐   │
│  │ projects.py (CRUD/status/dashboard)                      │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           ▼                                    │
│  ┌─── 业务服务层 ──────────────────────────────────────────┐   │
│  │ ProjectService: CRUD、启停控制、软删除                    │   │
│  │ DashboardService: 五层仪表板数据聚合计算                  │   │
│  └──────────┬───────────────────────┬──────────────────────┘   │
│             ▼                       ▼                          │
│  ┌─── 数据层 ──────┐   ┌─── 跨模块数据聚合 ─────────────┐     │
│  │ Project 模型     │   │ 接口覆盖率 ← M4                │     │
│  └─────────────────┘   │ 自动化用例统计 ← M8                  │     │
│                        │ 执行记录 ← M9                  │     │
│                        │ 趋势数据 ← M10                 │     │
│                        └────────────────────────────────┘     │
└────────────────────────────────────────────────────────────────┘
```

### 4.2 数据模型 DDL

```sql
CREATE TABLE `project` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `name`            VARCHAR(50)   NOT NULL                    COMMENT '项目名称',
  `description`     TEXT          DEFAULT NULL                COMMENT '项目描述',
  `source_path`     VARCHAR(500)  DEFAULT NULL                COMMENT '项目源码路径（服务端服务器绝对路径）',
  `is_active`       BOOLEAN       NOT NULL DEFAULT TRUE       COMMENT '是否启用',
  `created_by`      CHAR(36)      DEFAULT NULL                COMMENT '创建人 ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `fk_project_created_by` FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';
```

### 4.3 Dashboard 数据聚合算法

```python
# backend/app/services/dashboard_service.py

class DashboardService:
    """项目概览仪表板数据聚合"""

    def __init__(self, db: AsyncSession, project_id: str):
        self.db = db
        self.project_id = project_id

    async def get_dashboard(self) -> DashboardData:
        """并行聚合五层仪表板所需的全部数据"""
        stats = await asyncio.gather(
            self._calc_health_score(),
            self._calc_kpi_cards(),
            self._calc_trend_data(),
            self._calc_module_coverage(),
            self._calc_risk_top5()
        )
        return DashboardData(health_score=stats[0], kpi_cards=stats[1],
                            trend_data=stats[2], module_coverage=stats[3], risk_top5=stats[4])

    async def _calc_health_score(self) -> HealthScore:
        """
        质量健康度计算（0-100）：
        综合评分 = 通过率得分×35% + 覆盖率得分×25% + 稳定性得分×25% + 效率得分×15%
        """
        pass_rate = await self._get_latest_pass_rate()
        coverage = await self._get_api_coverage_rate()
        stability = await self._get_stability_score()
        efficiency = await self._get_efficiency_score()
        score = pass_rate * 0.35 + coverage * 0.25 + stability * 0.25 + efficiency * 0.15
        level = "优秀" if score >= 90 else "良好" if score >= 75 else "一般" if score >= 60 else "较差"
        return HealthScore(score=round(score, 1), level=level,
                          dimensions={"pass_rate": pass_rate, "coverage": coverage,
                                     "stability": stability, "efficiency": efficiency})
```

### 4.4 服务层设计

```python
# backend/app/services/project_service.py

class ProjectService:
    async def create_project(self, data: ProjectCreateSchema, user_id: str) -> Project:
        """
        创建项目：
        1. 校验项目名称唯一性
        2. 创建 Project 记录
        3. 自动创建两个系统默认接口分组（「全部」和「未分组」，is_system=True）
        """
        project = Project(name=data.name, description=data.description, created_by=user_id)
        self.db.add(project)
        await self.db.flush()
        for group_name in ["全部", "未分组"]:
            self.db.add(ApiModule(project_id=project.id, name=group_name, service_prefix="", is_system=True))
        await self.db.flush()
        return project

    async def delete_project(self, project_id: str) -> None:
        """软删除项目：设置 is_active=False"""
        project = await self._find_project(project_id)
        project.is_active = False
        await self.db.flush()
```

### 4.5 错误码明细

| 错误码 | HTTP | 触发场景 |
|---|---|---|
| 1201 | 409 | 项目名称已存在 |
| 1202 | 400 | 项目已停用，不可执行操作 |
| 1203 | 404 | 项目不存在 |

---

## 5. M3 — 环境配置管理模块

### 5.1 模块内部架构

```
┌────────────────────────────────────────────────────────────────┐
│                    M3 环境配置管理                                │
│                                                                │
│  ┌─── API 路由层 ──────────────────────────────────────────┐   │
│  │ environments.py (CRUD/activate/test)                     │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           ▼                                    │
│  ┌─── 业务服务层 ──────────────────────────────────────────┐   │
│  │ EnvironmentService:                                      │   │
│  │   CRUD、激活切换（互斥）、JSON Schema 校验、连接测试       │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           ▼                                    │
│  ┌─── 数据层 ──────────────────────────────────────────────┐   │
│  │ Environment 模型（config JSON 字段存储配置数据）           │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

### 5.2 数据模型 DDL

```sql
CREATE TABLE `environment` (
  `id`              CHAR(36)      NOT NULL                    COMMENT 'UUID 主键',
  `project_id`      CHAR(36)      NOT NULL                    COMMENT '所属项目 ID',
  `name`            VARCHAR(50)   NOT NULL                    COMMENT '环境名称',
  `description`     TEXT          DEFAULT NULL                COMMENT '环境描述',
  `config`          JSON          NOT NULL                    COMMENT '环境配置 JSON',
  `is_active`       BOOLEAN       NOT NULL DEFAULT FALSE      COMMENT '是否为当前激活环境',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_name` (`project_id`, `name`),
  KEY `idx_project_active` (`project_id`, `is_active`),
  CONSTRAINT `fk_env_project` FOREIGN KEY (`project_id`) REFERENCES `project`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='环境配置表';
```

**config JSON 必填字段：** `host`（目标服务地址）、`host_authorization`（认证信息）。可选字段：`wss`、`nacos`、`nacos_accessToken`、`company`。支持自定义扩展字段。

### 5.3 服务层关键方法

```python
# backend/app/services/environment_service.py

class EnvironmentService:
    async def activate_environment(self, project_id: str, env_id: str) -> None:
        """
        激活环境（互斥切换）：
        事务内先将该项目所有环境 is_active=False，再将目标环境 is_active=True
        """
        await self.db.execute(
            update(Environment).where(
                Environment.project_id == project_id, Environment.is_active == True
            ).values(is_active=False))
        env = await self._find_env(project_id, env_id)
        env.is_active = True
        await self.db.flush()

    async def delete_environment(self, project_id: str, env_id: str) -> None:
        """激活环境不可删除"""
        env = await self._find_env(project_id, env_id)
        if env.is_active:
            raise BusinessError(1303, "当前激活环境不可删除，请先切换到其他环境")
        await self.db.delete(env)

    async def test_connection(self, project_id: str, env_id: str) -> ConnectionTestResult:
        """使用 httpx 发送 HEAD 请求测试连通性，返回延迟毫秒数"""
        env = await self._find_env(project_id, env_id)
        async with httpx.AsyncClient(timeout=10.0) as client:
            try:
                start = time.time()
                resp = await client.head(env.config["host"])
                return ConnectionTestResult(success=True, message=f"HTTP {resp.status_code}",
                                           latency_ms=int((time.time() - start) * 1000))
            except Exception as e:
                return ConnectionTestResult(success=False, message=str(e))
```

### 5.4 错误码明细

| 错误码 | HTTP | 触发场景 |
|---|---|---|
| 1301 | 409 | 环境名称在该项目下已存在 |
| 1302 | 400 | config JSON Schema 校验失败 |
| 1303 | 400 | 当前激活环境不可删除 |
| 1304 | 400 | 连接测试失败 |

---

## 6. M4 — 接口文档模块

### 6.1 模块内部架构

```
┌─── API Route Layer ──────────────────────────────────────────────────────────┐
│  apis.py          modules.py         swagger.py                             │
│  (接口 CRUD/调试/批量)  (分组 CRUD)     (导入/同步)                             │
└───────────────────────────┬──────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────────────────────┐
│                         Service Layer                                        │
│  ┌──────────────────┐ ┌────────────────────┐ ┌──────────────────────┐       │
│  │ ApiModuleService  │ │ ApiEndpointService │ │ SwaggerImportService │       │
│  │ · create_default  │ │ · create           │ │ · parse_swagger      │       │
│  │   _modules()      │ │ · delete (保护链)   │ │ · _resolve_ref()     │       │
│  │ · create()        │ │ · batch_delete()   │ │ · import_endpoints() │       │
│  │ · update()        │ │ · batch_update()   │ └──────────────────────┘       │
│  │ · delete(孤儿迁移) │ │ · debug_endpoint() │ ┌──────────────────────┐       │
│  └──────────────────┘ └────────────────────┘ │ ApiSyncService       │       │
│                                               │ · compare()          │       │
│                                               │ · execute_sync()     │       │
│                                               └──────────────────────┘       │
└───────────────────────────┬──────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────────────────────┐
│  ┌─ Data Layer ──────────┐  ┌─ Engine Adapter ────────────────────────┐    │
│  │ ApiModule (ORM)        │  │ EngineAdapter.debug_request()           │    │
│  │ ApiEndpoint (ORM)      │  │ (httpx.AsyncClient → 被测服务)          │    │
│  │ ApiSyncHistory (ORM)   │  └────────────────────────────────────────┘    │
│  │ ApiKeyword (引用检查)   │                                                │
│  │ Keyword (下游追溯)      │                                                │
│  └────────────────────────┘                                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 DDL

#### 6.2.1 api_module 表（接口分组）

```sql
CREATE TABLE api_module (
    id              CHAR(36)     NOT NULL DEFAULT (UUID()) COMMENT '主键 UUID v4',
    project_id      CHAR(36)     NOT NULL COMMENT '所属项目 ID',
    name            VARCHAR(100) NOT NULL COMMENT '分组名称',
    service_prefix  VARCHAR(100) DEFAULT NULL COMMENT '服务前缀，如 /users',
    description     TEXT         DEFAULT NULL COMMENT '分组描述',
    source_type     ENUM('SWAGGER_IMPORT','MANUAL') NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
    swagger_file    VARCHAR(500) DEFAULT NULL COMMENT '导入的 Swagger 文件路径',
    is_system       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统默认分组（全部/未分组）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_project_id (project_id),
    UNIQUE INDEX uk_project_name (project_id, name),
    CONSTRAINT fk_module_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口分组';
```

**系统默认分组规则：** 每个项目创建时自动生成 `全部`（is_system=1）和 `未分组`（is_system=1）两个系统分组。系统分组不可编辑、不可删除。删除自定义分组时，其下接口自动移入「未分组」。

#### 6.2.2 api_endpoint 表（接口）

```sql
CREATE TABLE api_endpoint (
    id              CHAR(36)      NOT NULL DEFAULT (UUID()) COMMENT '主键 UUID v4',
    module_id       CHAR(36)      NOT NULL COMMENT '所属分组 ID',
    name            VARCHAR(200)  NOT NULL COMMENT '接口名称',
    path            VARCHAR(500)  NOT NULL COMMENT '请求路径，如 /api/users/list',
    method          ENUM('GET','POST','PUT','PATCH','DELETE') NOT NULL COMMENT 'HTTP 方法',
    description     TEXT          DEFAULT NULL COMMENT '接口描述',
    parameters      JSON          DEFAULT NULL COMMENT '请求参数 [{name,in,type,required,description}]',
    request_body    JSON          DEFAULT NULL COMMENT '请求体 Schema（POST/PUT/PATCH）',
    responses       JSON          DEFAULT NULL COMMENT '响应定义 {statusCode: schema}',
    headers         JSON          DEFAULT NULL COMMENT '请求头 [{name,type,required,description,value}]',
    content_type    VARCHAR(100)  NOT NULL DEFAULT 'application/x-www-form-urlencoded' COMMENT '内容类型（Swagger 未声明 consumes 时默认 form-urlencoded）',
    source_type     ENUM('SWAGGER_IMPORT','MANUAL') NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
    is_active       TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否启用',
    is_deprecated   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已废弃（同步后标记）',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_module_id (module_id),
    UNIQUE INDEX uk_module_path_method (module_id, path, method),
    INDEX idx_is_active (is_active),
    INDEX idx_source_type (source_type),
    CONSTRAINT fk_endpoint_module FOREIGN KEY (module_id) REFERENCES api_module(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口定义';
```

**索引说明：** `uk_module_path_method` 确保同一分组内 path+method 唯一，用于 Swagger 增量导入时按 path+method 匹配已有接口。

#### 6.2.3 api_sync_history 表（同步历史）

```sql
CREATE TABLE api_sync_history (
    id              CHAR(36)      NOT NULL DEFAULT (UUID()) COMMENT '主键 UUID v4',
    project_id      CHAR(36)      NOT NULL COMMENT '项目 ID',
    module_id       CHAR(36)      DEFAULT NULL COMMENT '同步的目标分组 ID',
    swagger_file    VARCHAR(500)  DEFAULT NULL COMMENT '上传的 Swagger 文件名',
    added_count     INT           NOT NULL DEFAULT 0 COMMENT '新增接口数',
    updated_count   INT           NOT NULL DEFAULT 0 COMMENT '更新接口数',
    deprecated_count INT          NOT NULL DEFAULT 0 COMMENT '废弃接口数',
    failed_count    INT           NOT NULL DEFAULT 0 COMMENT '失败数',
    diff_detail     JSON          DEFAULT NULL COMMENT '差异明细 JSON',
    operated_by     CHAR(36)      DEFAULT NULL COMMENT '操作人 ID',
    synced_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    PRIMARY KEY (id),
    INDEX idx_project_synced (project_id, synced_at DESC),
    CONSTRAINT fk_sync_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口同步历史';
```

### 6.3 服务层设计

#### 6.3.1 ApiModuleService（分组管理）

```python
class ApiModuleService:
    """接口分组管理服务"""

    async def create_default_modules(self, db: AsyncSession, project_id: str) -> None:
        """项目创建时自动创建「全部」和「未分组」两个系统分组"""
        for name in ["全部", "未分组"]:
            db.add(ApiModule(project_id=project_id, name=name, is_system=True))
        await db.flush()

    async def create(self, db: AsyncSession, project_id: str,
                     data: ModuleCreate) -> ApiModule:
        """创建自定义分组，名称不可与系统分组重复"""
        exists = await db.scalar(
            select(ApiModule).where(
                ApiModule.project_id == project_id,
                ApiModule.name == data.name
            )
        )
        if exists:
            raise BusinessError(1401, "分组名称已存在")
        module = ApiModule(
            project_id=project_id, name=data.name,
            service_prefix=data.service_prefix, description=data.description,
            source_type="MANUAL"
        )
        db.add(module)
        await db.flush()
        return module

    async def update(self, db: AsyncSession, module_id: str,
                     data: ModuleUpdate) -> ApiModule:
        """更新分组，系统分组不可编辑"""
        module = await db.get(ApiModule, module_id)
        if module.is_system:
            raise BusinessError(1402, "系统默认分组不可编辑")
        for field, value in data.model_dump(exclude_unset=True).items():
            setattr(module, field, value)
        await db.flush()
        return module

    async def delete(self, db: AsyncSession, module_id: str) -> None:
        """删除分组，系统分组不可删除；分组下接口自动移入「未分组」"""
        module = await db.get(ApiModule, module_id)
        if module.is_system:
            raise BusinessError(1403, "系统默认分组不可删除")
        # 查找「未分组」
        ungrouped = await db.scalar(
            select(ApiModule).where(
                ApiModule.project_id == module.project_id,
                ApiModule.is_system == True,
                ApiModule.name == "未分组"
            )
        )
        # 将其下接口移入「未分组」
        await db.execute(
            update(ApiEndpoint).where(
                ApiEndpoint.module_id == module_id
            ).values(module_id=ungrouped.id)
        )
        await db.delete(module)
```

#### 6.3.2 ApiEndpointService（接口文档）

```python
class ApiEndpointService:
    """接口文档服务，包含 CRUD、删除保护、批量操作、在线调试"""

    async def check_endpoint_dependencies(
        self, db: AsyncSession, endpoint_id: str
    ) -> DependencyCheckResult:
        """
        三级依赖检查：接口 → 接口关键字 → Action → 自动化用例
        返回 DependencyCheckResult(has_dependencies, keywords, cases)
        """
        # 第一级：查找关联的 ApiKeyword
        api_keywords = await db.scalars(
            select(ApiKeyword).where(ApiKeyword.endpoint_id == endpoint_id)
        )
        if not api_keywords:
            return DependencyCheckResult(has_dependencies=False)

        keywords_info = []
        cases_info = []
        for ak in api_keywords:
            kw = await db.get(Keyword, ak.keyword_id)
            # 第二级：查找引用该关键字的 Action（通过 nodes JSON）
            actions = await db.scalars(
                select(Action).where(
                    Action.project_id == kw.project_id,
                    func.json_contains(
                        Action.nodes, f'"{kw.id}"', '$.keyword_id'
                    )
                )
            )
            action_list = []
            for act in actions:
                act_kw = await db.scalar(
                    select(Keyword).where(Keyword.ref_id == act.id)
                )
                # 第三级：查找引用该 Action 的自动化用例
                cases = await db.scalars(
                    select(AutoCase).where(
                        func.json_contains(
                            AutoCase.steps, f'"{act_kw.id}"', '$.keyword_id'
                        )
                    )
                )
                action_list.append({
                    "name": act_kw.name, "category": act_kw.category,
                    "case_count": len(cases)
                })
                for c in cases:
                    ckw = await db.get(Keyword, c.keyword_id)
                    cases_info.append({
                        "case_name": ckw.name,
                        "action_name": act_kw.name
                    })
            keywords_info.append({
                "keyword_name": kw.name, "category": kw.category,
                "actions": action_list
            })

        return DependencyCheckResult(
            has_dependencies=True,
            keywords=keywords_info, cases=cases_info
        )

    async def delete_endpoint(
        self, db: AsyncSession, endpoint_id: str
    ) -> None:
        """删除接口，有依赖时拒绝删除"""
        deps = await self.check_endpoint_dependencies(db, endpoint_id)
        if deps.has_dependencies:
            raise DependencyError(
                code=1404,
                message="接口被关键字引用，无法删除",
                details=deps.to_dict()
            )
        endpoint = await db.get(ApiEndpoint, endpoint_id)
        await db.delete(endpoint)

    async def batch_delete(
        self, db: AsyncSession, project_id: str, endpoint_ids: list[str]
    ) -> BatchDeleteResult:
        """批量删除：逐条执行依赖检查，无依赖的直接删除，有依赖的跳过"""
        deleted, skipped = [], []
        for eid in endpoint_ids:
            deps = await self.check_endpoint_dependencies(db, eid)
            if deps.has_dependencies:
                ep = await db.get(ApiEndpoint, eid)
                skipped.append({
                    "id": eid, "name": ep.name,
                    "dependency_count": len(deps.keywords)
                })
            else:
                ep = await db.get(ApiEndpoint, eid)
                await db.delete(ep)
                deleted.append(eid)
        return BatchDeleteResult(
            success=len(deleted), failed=len(skipped),
            failed_details=skipped
        )

    async def batch_update(
        self, db: AsyncSession, endpoint_ids: list[str],
        action: str, target_module_id: str | None = None
    ) -> int:
        """
        批量启用/禁用/移动分组
        action: 'enable' | 'disable' | 'move'
        """
        if action == "enable":
            stmt = update(ApiEndpoint).where(
                ApiEndpoint.id.in_(endpoint_ids)
            ).values(is_active=True)
        elif action == "disable":
            stmt = update(ApiEndpoint).where(
                ApiEndpoint.id.in_(endpoint_ids)
            ).values(is_active=False)
        elif action == "move" and target_module_id:
            stmt = update(ApiEndpoint).where(
                ApiEndpoint.id.in_(endpoint_ids)
            ).values(module_id=target_module_id)
        else:
            raise BusinessError(1000, "无效的批量操作类型")
        result = await db.execute(stmt)
        return result.rowcount

    async def debug_endpoint(
        self, db: AsyncSession, project_id: str,
        endpoint_id: str, data: DebugRequest
    ) -> DebugResponse:
        """接口在线调试：加载环境配置 → 构建请求 → 发送 → 返回响应"""
        endpoint = await db.get(ApiEndpoint, endpoint_id)
        env = await db.get(Environment, data.environment_id)
        # 合并参数
        path_params = {p["name"]: data.params.get(p["name"], "")
                       for p in endpoint.parameters or []
                       if p.get("in") == "path"}
        query_params = {p["name"]: data.params.get(p["name"], "")
                        for p in endpoint.parameters or []
                        if p.get("in") == "query"}
        url = env.config["host"].rstrip("/") + endpoint.path.format(**path_params)
        headers = dict(data.headers or {})
        if "host_authorization" in env.config:
            headers["Authorization"] = env.config["host_authorization"]
        # 发送请求
        async with httpx.AsyncClient(timeout=30.0) as client:
            start = time.time()
            resp = await client.request(
                method=endpoint.method, url=url,
                params=query_params, json=data.body,
                headers=headers
            )
            elapsed = int((time.time() - start) * 1000)
        return DebugResponse(
            status_code=resp.status_code,
            body=resp.json() if resp.headers.get("content-type", "").startswith("application/json") else resp.text,
            headers=dict(resp.headers),
            elapsed_ms=elapsed
        )

    async def list_endpoints(
        self, db: AsyncSession, project_id: str,
        page: int = 1, page_size: int = 20,
        module_id: str | None = None, method: str | None = None,
        source_type: str | None = None, keyword: str | None = None
    ) -> PageResponse[ApiEndpoint]:
        """分页查询接口列表，支持按分组/方法/来源/关键词筛选"""
        query = select(ApiEndpoint).join(ApiModule).where(
            ApiModule.project_id == project_id
        )
        if module_id:
            query = query.where(ApiEndpoint.module_id == module_id)
        if method:
            query = query.where(ApiEndpoint.method == method)
        if source_type:
            query = query.where(ApiEndpoint.source_type == source_type)
        if keyword:
            query = query.where(
                or_(
                    ApiEndpoint.name.contains(keyword),
                    ApiEndpoint.path.contains(keyword)
                )
            )
        query = query.order_by(ApiEndpoint.created_at.desc())
        return await paginate(db, query, page, page_size)
```

#### 6.3.3 SwaggerImportService（Swagger 导入解析）

```python
class SwaggerImportService:
    """Swagger 2.0 / OpenAPI 3.0 JSON 导入解析服务"""

    def parse_swagger(self, swagger_json: dict) -> list[ParsedEndpoint]:
        """
        解析 Swagger 2.0 / OpenAPI 3.0 JSON，提取所有接口定义
        复用 postman-engine 的 ConvertApiDoc 解析逻辑

        Content-Type 解析规则：
        - Swagger 2.0: 优先取 operation.consumes[0]，回退到 root.consumes[0]
        - OpenAPI 3.0: 取 requestBody.content 的第一个 mediaType 键
        - 以上均未获取到时，默认 "application/x-www-form-urlencoded"
        - 解析出的 Content-Type 写入 headers JSON 数组，同时存入 contentType 字段
        """
        paths = swagger_json.get("paths", {})
        definitions = swagger_json.get("definitions", {})
        root_consumes = swagger_json.get("consumes", [])
        parsed = []
        for path, methods in paths.items():
            for method, spec in methods.items():
                if method.upper() not in ("GET","POST","PUT","PATCH","DELETE"):
                    continue
                params = self._extract_params(spec, definitions)
                request_body = self._extract_request_body(spec, definitions)
                responses = self._extract_responses(spec, definitions)
                content_type = self._resolve_content_type(
                    spec, root_consumes
                )
                headers = self._build_headers(spec, content_type)
                parsed.append(ParsedEndpoint(
                    path=path, method=method.upper(),
                    name=spec.get("summary", f"{method.upper()} {path}"),
                    description=spec.get("description", ""),
                    parameters=params, request_body=request_body,
                    responses=responses, headers=headers,
                    content_type=content_type,
                    tags=spec.get("tags", [])
                ))
        return parsed

    def _resolve_content_type(
        self, spec: dict, root_consumes: list[str]
    ) -> str:
        """
        解析接口的 Content-Type
        - Swagger 2.0: operation.consumes[0] > root.consumes[0]
        - OpenAPI 3.0: requestBody.content 的第一个 mediaType
        - 未获取到时默认 "application/x-www-form-urlencoded"
        """
        # OpenAPI 3.0: requestBody.content
        request_body = spec.get("requestBody")
        if request_body and "content" in request_body:
            media_types = list(request_body["content"].keys())
            if media_types:
                return media_types[0]
        # Swagger 2.0: consumes
        op_consumes = spec.get("consumes", [])
        if op_consumes:
            return op_consumes[0]
        if root_consumes:
            return root_consumes[0]
        # 默认
        return "application/x-www-form-urlencoded"

    def _resolve_ref(self, ref: str, definitions: dict) -> dict:
        """解析 $ref 引用，展开嵌套 DTO"""
        ref_name = ref.split("/")[-1]
        schema = definitions.get(ref_name, {})
        result = {}
        for prop_name, prop_def in schema.get("properties", {}).items():
            if "$ref" in prop_def:
                result[prop_name] = self._resolve_ref(
                    prop_def["$ref"], definitions
                )
            elif prop_def.get("type") == "array" and "$ref" in prop_def.get("items", {}):
                result[prop_name] = {
                    "type": "array",
                    "items": self._resolve_ref(
                        prop_def["items"]["$ref"], definitions
                    )
                }
            else:
                result[prop_name] = prop_def
        return result

    def _extract_params(self, spec: dict, definitions: dict) -> list[dict]:
        """提取 Path/Query/Header 参数"""
        params = []
        for p in spec.get("parameters", []):
            if p.get("in") in ("path", "query", "header"):
                params.append({
                    "name": p["name"], "in": p["in"],
                    "type": p.get("type", "string"),
                    "required": p.get("required", False),
                    "description": p.get("description", "")
                })
        return params

    def _extract_request_body(self, spec: dict, definitions: dict) -> dict | None:
        """提取请求体 Schema（body 参数），展开 $ref"""
        for p in spec.get("parameters", []):
            if p.get("in") == "body":
                schema = p.get("schema", {})
                if "$ref" in schema:
                    return self._resolve_ref(schema["$ref"], definitions)
                return schema
        return None

    def _extract_responses(self, spec: dict, definitions: dict) -> dict:
        """提取响应定义，展开 $ref"""
        responses = {}
        for code, resp in spec.get("responses", {}).items():
            schema = resp.get("schema", {})
            if "$ref" in schema:
                schema = self._resolve_ref(schema["$ref"], definitions)
            responses[str(code)] = {
                "description": resp.get("description", ""),
                "schema": schema
            }
        return responses

    async def import_endpoints(
        self, db: AsyncSession, project_id: str,
        module_id: str, parsed: list[ParsedEndpoint],
        selected_indices: list[int],
        default_headers: dict | None = None
    ) -> ImportResult:
        """
        增量导入：按 path+method 匹配
        - 已存在 → 更新参数定义、headers、content_type
        - 不存在 → 新增
        - default_headers: 同步配置的默认请求头，合并到每个接口的 headers 中
          合并规则：默认请求头覆盖同名已有请求头，但 Content-Type 始终跳过
          （Content-Type 由 Swagger 规范的 consumes/requestBody 决定，默认请求头不介入）
        """
        added, updated = 0, 0
        endpoints_list = []
        for idx in selected_indices:
            ep = parsed[idx]
            # 合并默认请求头（Content-Type 始终保留 Swagger 解析值）
            merged_headers = self._merge_headers(
                ep.headers, default_headers
            )
            existing = await db.scalar(
                select(ApiEndpoint).join(ApiModule).where(
                    ApiModule.project_id == project_id,
                    ApiEndpoint.path == ep.path,
                    ApiEndpoint.method == ep.method
                )
            )
            if existing:
                existing.parameters = ep.parameters
                existing.request_body = ep.request_body
                existing.responses = ep.responses
                existing.name = ep.name
                existing.description = ep.description
                existing.headers = merged_headers
                existing.content_type = ep.content_type
                existing.source_type = "SWAGGER_IMPORT"
                updated += 1
                endpoints_list.append({"status": "updated", "path": ep.path, "method": ep.method})
            else:
                new_ep = ApiEndpoint(
                    module_id=module_id, name=ep.name, path=ep.path,
                    method=ep.method, description=ep.description,
                    parameters=ep.parameters, request_body=ep.request_body,
                    responses=ep.responses, headers=merged_headers,
                    content_type=ep.content_type,
                    source_type="SWAGGER_IMPORT"
                )
                db.add(new_ep)
                added += 1
                endpoints_list.append({"status": "added", "path": ep.path, "method": ep.method})
        await db.flush()
        return ImportResult(added=added, updated=updated, endpoints=endpoints_list)

    @staticmethod
    def _merge_headers(
        existing_headers: list[dict] | None,
        default_headers: dict | None
    ) -> list[dict]:
        """
        合并默认请求头到已有 headers
        - 默认请求头覆盖同名已有请求头（大小写不敏感）
        - Content-Type 始终跳过：由 Swagger 解析值决定，不受默认请求头影响
        """
        if not default_headers:
            return existing_headers or []
        result = list(existing_headers or [])
        default_names = {k.lower() for k in default_headers}
        # 移除已有的同名请求头（Content-Type 除外）
        result = [
            h for h in result
            if h["name"].lower() not in default_names
            or h["name"].lower() == "content-type"
        ]
        # 追加默认请求头（跳过 Content-Type）
        for k, v in default_headers.items():
            if k.lower() == "content-type":
                continue
            result.append({"name": k, "type": "string",
                          "required": False, "value": v})
        return result
```

#### 6.3.4 ApiSyncService（接口同步）

```python
class ApiSyncService:
    """接口同步差异对比与执行服务"""

    async def compare(
        self, db: AsyncSession, module_id: str,
        swagger_json: dict
    ) -> SyncDiffResult:
        """
        对比 Swagger 文件与现有接口的差异
        按 path+method 作为唯一标识进行匹配
        """
        import_svc = SwaggerImportService()
        parsed = import_svc.parse_swagger(swagger_json)
        # 加载分组内现有接口
        existing = await db.scalars(
            select(ApiEndpoint).where(ApiEndpoint.module_id == module_id)
        )
        existing_map = {(ep.path, ep.method): ep for ep in existing}
        swagger_map = {(ep.path, ep.method): ep for ep in parsed}

        added, updated, deprecated = [], [], []
        for key, ep in swagger_map.items():
            if key not in existing_map:
                added.append(ep)
            else:
                changes = self._compare_fields(existing_map[key], ep)
                if changes:
                    updated.append({"endpoint": ep, "changes": changes})
        for key, ep in existing_map.items():
            if key not in swagger_map:
                deprecated.append(ep)

        return SyncDiffResult(added=added, updated=updated, deprecated=deprecated)

    def _compare_fields(self, existing: ApiEndpoint,
                        parsed: ParsedEndpoint) -> list[str]:
        """比较两个接口的字段差异，返回变更字段列表"""
        changes = []
        if existing.parameters != parsed.parameters:
            changes.append("parameters")
        if existing.request_body != parsed.request_body:
            changes.append("request_body")
        if existing.responses != parsed.responses:
            changes.append("responses")
        return changes

    async def execute_sync(
        self, db: AsyncSession, project_id: str,
        module_id: str, diff: SyncDiffResult,
        sync_selected: SyncSelection,
        operated_by: str
    ) -> SyncResult:
        """执行同步操作"""
        added_count, updated_count, deprecated_count, failed_count = 0, 0, 0, 0
        # 新增接口
        for ep in diff.added:
            if ep.path in sync_selected.added_paths:
                db.add(ApiEndpoint(
                    module_id=module_id, name=ep.name, path=ep.path,
                    method=ep.method, description=ep.description,
                    parameters=ep.parameters, request_body=ep.request_body,
                    responses=ep.responses, source_type="SWAGGER_IMPORT"
                ))
                added_count += 1
        # 更新接口
        for item in diff.updated:
            ep = item["endpoint"]
            if ep.path in sync_selected.updated_paths:
                existing = await db.scalar(
                    select(ApiEndpoint).where(
                        ApiEndpoint.module_id == module_id,
                        ApiEndpoint.path == ep.path,
                        ApiEndpoint.method == ep.method
                    )
                )
                existing.parameters = ep.parameters
                existing.request_body = ep.request_body
                existing.responses = ep.responses
                updated_count += 1
        # 废弃接口（勾选删除的执行依赖检查，有依赖的标记废弃保留）
        endpoint_svc = ApiEndpointService()
        for ep in diff.deprecated:
            if ep.id in sync_selected.deprecated_ids:
                deps = await endpoint_svc.check_endpoint_dependencies(db, ep.id)
                if not deps.has_dependencies:
                    await db.delete(ep)
                    deprecated_count += 1
                else:
                    ep.is_deprecated = True
                    failed_count += 1
        # 记录同步历史
        await db.add(ApiSyncHistory(
            project_id=project_id, module_id=module_id,
            added_count=added_count, updated_count=updated_count,
            deprecated_count=deprecated_count, failed_count=failed_count,
            operated_by=operated_by
        ))
        await db.flush()
        return SyncResult(
            success=added_count + updated_count + deprecated_count,
            skipped=failed_count,
            added=added_count, updated=updated_count,
            deprecated=deprecated_count
        )
```

### 6.4 Pydantic Schema

```python
# ── Request Schemas ──

class ModuleCreate(BaseModel):
    name: str = Field(..., max_length=100, description="分组名称")
    service_prefix: str | None = Field(None, max_length=100, description="服务前缀")
    description: str | None = None

class ModuleUpdate(BaseModel):
    name: str | None = Field(None, max_length=100)
    service_prefix: str | None = None
    description: str | None = None

class EndpointCreate(BaseModel):
    name: str = Field(..., max_length=200)
    path: str = Field(..., max_length=500)
    method: Literal["GET","POST","PUT","PATCH","DELETE"]
    module_id: str = Field(..., description="所属分组 ID")
    description: str | None = None
    parameters: list[dict] | None = None
    request_body: dict | None = None
    responses: dict | None = None

class EndpointUpdate(BaseModel):
    name: str | None = None
    path: str | None = None
    method: str | None = None
    module_id: str | None = None
    description: str | None = None
    parameters: list[dict] | None = None
    request_body: dict | None = None
    responses: dict | None = None

class SwaggerImportRequest(BaseModel):
    """Swagger 导入请求（文件通过 UploadFile 接收）"""
    group_mode: Literal["existing", "new", "auto_tags"] = Field(
        ..., description="分组模式：已有分组/创建新分组/按 tags 自动拆分"
    )
    group_id: str | None = Field(None, description="已有分组 ID（group_mode=existing）")
    new_group_name: str | None = Field(None, description="新分组名称（group_mode=new）")
    new_group_prefix: str | None = Field(None, description="新分组前缀")
    selected_indices: list[int] = Field(..., description="选中的接口索引列表")

class SyncRequest(BaseModel):
    """接口同步请求"""
    module_id: str
    selected: SyncSelection

class DebugRequest(BaseModel):
    environment_id: str
    params: dict = Field(default_factory=dict)
    body: dict | None = None
    headers: dict | None = None

class BatchRequest(BaseModel):
    action: Literal["enable", "disable", "move", "delete"]
    ids: list[str]
    group_id: str | None = Field(None, description="目标分组 ID（move 时必填）")

# ── Response Schemas ──

class ModuleResponse(BaseModel):
    id: str
    name: str
    service_prefix: str | None
    description: str | None
    source_type: str
    is_system: bool
    api_count: int = Field(description="分组内接口数量")
    created_at: datetime

class EndpointResponse(BaseModel):
    id: str
    module_id: str
    module_name: str | None = None
    name: str
    path: str
    method: str
    description: str | None
    parameters: list[dict] | None
    request_body: dict | None
    responses: dict | None
    source_type: str
    is_active: bool
    is_deprecated: bool
    created_at: datetime
    updated_at: datetime

class ImportResultResponse(BaseModel):
    added: int
    updated: int
    endpoints: list[dict]

class SyncDiffResponse(BaseModel):
    added: list[EndpointResponse]
    updated: list[dict]  # {endpoint: EndpointResponse, changes: list[str]}
    deprecated: list[EndpointResponse]

class DebugResponse(BaseModel):
    status_code: int
    body: Any
    headers: dict
    elapsed_ms: int

class BatchResultResponse(BaseModel):
    success: int
    failed: int
    failed_details: list[dict] | None = None

class DependencyCheckResponse(BaseModel):
    has_dependencies: bool
    keywords: list[dict] | None = None
    cases: list[dict] | None = None
```

### 6.5 API 路由签名

```python
router = APIRouter(prefix="/api/v1/projects/{project_id}/apis", tags=["M4 接口文档"])

# ── 分组管理 ──
@router.get("/modules", response_model=list[ModuleResponse])
async def list_modules(project_id: str, db: AsyncSession = Depends(get_db)):
    """获取项目下所有接口分组（含接口数量统计）"""

@router.post("/modules", response_model=ModuleResponse, status_code=201)
async def create_module(project_id: str, data: ModuleCreate,
                        db: AsyncSession = Depends(get_db)):
    """创建接口分组"""

@router.put("/modules/{module_id}", response_model=ModuleResponse)
async def update_module(project_id: str, module_id: str, data: ModuleUpdate,
                        db: AsyncSession = Depends(get_db)):
    """更新接口分组"""

@router.delete("/modules/{module_id}", status_code=204)
async def delete_module(project_id: str, module_id: str,
                        db: AsyncSession = Depends(get_db)):
    """删除接口分组（系统分组不可删，接口自动移入未分组）"""

# ── 接口文档 ──
@router.get("", response_model=PageResponse[EndpointResponse])
async def list_endpoints(
    project_id: str, page: int = 1, page_size: int = 20,
    module_id: str | None = None, method: str | None = None,
    source_type: str | None = None, keyword: str | None = None,
    db: AsyncSession = Depends(get_db)
):
    """分页查询接口列表"""

@router.post("", response_model=EndpointResponse, status_code=201)
async def create_endpoint(project_id: str, data: EndpointCreate,
                          db: AsyncSession = Depends(get_db)):
    """手动创建接口"""

@router.get("/{endpoint_id}", response_model=EndpointResponse)
async def get_endpoint(project_id: str, endpoint_id: str,
                       db: AsyncSession = Depends(get_db)):
    """获取接口详情"""

@router.put("/{endpoint_id}", response_model=EndpointResponse)
async def update_endpoint(project_id: str, endpoint_id: str,
                          data: EndpointUpdate,
                          db: AsyncSession = Depends(get_db)):
    """更新接口"""

@router.delete("/{endpoint_id}", status_code=204)
async def delete_endpoint(project_id: str, endpoint_id: str,
                          db: AsyncSession = Depends(get_db)):
    """删除接口（含三级依赖检查）"""

@router.get("/{endpoint_id}/dependencies", response_model=DependencyCheckResponse)
async def get_dependencies(project_id: str, endpoint_id: str,
                           db: AsyncSession = Depends(get_db)):
    """查询接口依赖关系"""

# ── Swagger 导入与同步 ──
@router.post("/import/swagger", response_model=ImportResultResponse)
async def import_swagger(project_id: str, file: UploadFile,
                         group_mode: str = Form(...),
                         group_id: str | None = Form(None),
                         new_group_name: str | None = Form(None),
                         selected_indices: str = Form(...),
                         db: AsyncSession = Depends(get_db)):
    """Swagger 2.0 / OpenAPI 3.0 JSON 文件导入（三步向导后端接口）"""

@router.post("/sync", response_model=SyncDiffResponse | SyncResultResponse)
async def sync_apis(project_id: str, file: UploadFile,
                    module_id: str = Form(...),
                    action: str | None = Form(None),
                    selected: str | None = Form(None),
                    db: AsyncSession = Depends(get_db)):
    """接口同步：上传 Swagger 文件对比差异，确认后执行同步"""

# ── 调试与批量 ──
@router.post("/{endpoint_id}/debug", response_model=DebugResponse)
async def debug_endpoint(project_id: str, endpoint_id: str,
                         data: DebugRequest,
                         db: AsyncSession = Depends(get_db)):
    """接口在线调试"""

@router.post("/batch", response_model=BatchResultResponse)
async def batch_operation(project_id: str, data: BatchRequest,
                          db: AsyncSession = Depends(get_db)):
    """批量操作（启用/禁用/移动/删除）"""
```

### 6.6 错误码明细与测试边界

| 错误码 | HTTP | 触发场景 |
|---|---|---|
| 1401 | 409 | 分组名称已存在 |
| 1402 | 400 | 系统默认分组不可编辑 |
| 1403 | 400 | 系统默认分组不可删除 |
| 1404 | 409 | 接口被关键字引用，不可删除 |
| 1405 | 409 | 同分组下 path+method 已存在 |
| 1406 | 400 | Swagger 文件解析失败（格式错误或非 2.0/OpenAPI 3.0 版本） |
| 1407 | 400 | 无效的批量操作类型 |

**测试边界：**

| 场景类型 | 测试点 |
|---|---|
| 正常流程 | 创建/编辑/删除自定义分组；创建/编辑/删除接口（无依赖） |
| Swagger 导入 | 文件解析正确性（paths/parameters/definitions/$ref 展开） |
| Swagger 导入 | 增量导入：已有接口更新参数、新接口追加、不删除多余接口 |
| Swagger 导入 | 三种分组模式：导入到已有分组 / 创建新分组 / 按 tags 自动拆分 |
| Swagger 导入 | Content-Type 解析：consumes/requestBody 优先级正确，无声明时默认 form-urlencoded |
| Swagger 导入 | 默认请求头合并：同名请求头覆盖，Content-Type 始终保留 Swagger 解析值 |
| 接口同步 | 差异对比：新增/参数变更/已废弃三类识别准确性 |
| 接口同步 | 执行同步：新增写入、变更更新参数、废弃有依赖标记保留/无依赖删除 |
| 删除保护 | 有 ApiKeyword 关联时拒绝删除，返回完整依赖链 |
| 批量操作 | 批量启用/禁用/移动分组的正确性；批量删除逐条依赖检查 |
| 在线调试 | 不同 HTTP 方法的请求构建、路径参数替换、环境变量注入 |
| 分组管理 | 删除自定义分组时接口自动移入「未分组」；系统分组不可编辑/删除 |


## 7. M11 — 测试代码库模块

### 7.1 模块内部架构

```
┌────────────────────────────────────────────────────────────────────┐
│                    M11 测试代码库 (repository)                      │
│                                                                    │
│  ┌─── Controller 层 ─────────────────────────────────────────┐    │
│  │ CodeRepositoryController:                                 │    │
│  │   列表 / 新建 / 编辑 / 删除 / 拉取 / 拉取记录               │    │
│  └───────────────────────────┬────────────────────────────────┘    │
│                              ▼                                     │
│  ┌─── Service 层 ───────────────────────────────────────────┐    │
│  │ CodeRepositoryService:                                    │    │
│  │   CRUD（重名校验/密码加解密）、pull（CLONE/PULL 分支）、    │    │
│  │   本地目录管理（buildLocalDir/递归删除）                    │    │
│  └───────┬──────────────────────────────┬─────────────────────┘    │
│          ▼                              ▼                          │
│  ┌─── JGit 集成 ────────────┐   ┌─── 数据层 ─────────────────┐   │
│  │ Git.cloneRepository()     │   │ CodeRepository             │   │
│  │ FetchCommand/PullCommand │   │ CodeRepositoryPullLog      │   │
│  │ UsernamePassword          │   │ (MyBatis-Plus Mapper)      │   │
│  │ CredentialsProvider       │   └────────────────────────────┘   │
│  └───────────────────────────┘                                    │
│                                                                    │
│  ┌─── 公共工具（platform-api）────────────────────────────────┐   │
│  │ AesCryptoUtil: 凭证 AES-128/CBC 加解密（enc: 前缀）          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘
```

**三模块分层（与平台既有模式一致）：**
- `platform-api`：`com.platform.repository.dto`（5 个 DTO）+ `com.platform.common.util.AesCryptoUtil`
- `platform-data`：`com.platform.repository.entity`（CodeRepository、CodeRepositoryPullLog）+ `mapper`（两个 BaseMapper）
- `platform-server`：`com.platform.repository.controller` + `service`（业务逻辑与 JGit 调用）

### 7.2 数据模型 DDL

#### 7.2.1 code_repository 表（测试代码仓库）

```sql
CREATE TABLE `code_repository` (
  `id`               bigint       NOT NULL AUTO_INCREMENT,
  `project_id`       bigint       NOT NULL                COMMENT '所属项目 ID',
  `name`             varchar(50)  NOT NULL                COMMENT '仓库名称（项目内唯一）',
  `git_url`          varchar(500) NOT NULL                COMMENT 'Git 仓库地址',
  `branch`           varchar(100) DEFAULT NULL            COMMENT '拉取分支（NULL=仓库默认分支）',
  `description`      varchar(255) DEFAULT NULL            COMMENT '仓库描述',
  `auth_username`    varchar(200) DEFAULT NULL            COMMENT '认证用户名',
  `auth_password`    varchar(1000) DEFAULT NULL           COMMENT '认证密码/Token（AES 加密，enc: 前缀）',
  `local_path`       varchar(500) DEFAULT NULL            COMMENT '本地存储相对路径（{projectId}/{repoId}）',
  `last_pull_at`     datetime     DEFAULT NULL            COMMENT '最近拉取时间',
  `last_pull_status` varchar(20)  DEFAULT NULL            COMMENT '最近拉取状态：RUNNING/SUCCESS/FAILED',
  `last_commit_id`   varchar(64)  DEFAULT NULL            COMMENT '最近拉取成功后的 HEAD commit id',
  `created_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_repository_project_name` (`project_id`, `name`),
  CONSTRAINT `fk_code_repository_project` FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试代码仓库表';
```

#### 7.2.2 code_repository_pull_log 表（拉取历史）

```sql
CREATE TABLE `code_repository_pull_log` (
  `id`            bigint        NOT NULL AUTO_INCREMENT,
  `repository_id` bigint        NOT NULL                COMMENT '所属仓库 ID',
  `pull_type`     varchar(10)   NOT NULL                COMMENT '拉取类型：CLONE 克隆 / PULL 增量更新',
  `branch`        varchar(100)  DEFAULT NULL            COMMENT '本次拉取分支（NULL=默认分支）',
  `status`        varchar(20)   NOT NULL                COMMENT '状态：RUNNING/SUCCESS/FAILED',
  `commit_id`     varchar(64)   DEFAULT NULL            COMMENT '拉取成功后的 HEAD commit id',
  `message`       varchar(2000) DEFAULT NULL            COMMENT '结果信息（失败原因/成功说明）',
  `duration_ms`   bigint        DEFAULT NULL            COMMENT '拉取耗时（毫秒）',
  `created_at`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pull_log_repo` (`repository_id`),
  CONSTRAINT `fk_pull_log_repository` FOREIGN KEY (`repository_id`) REFERENCES `code_repository`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库拉取历史表';
```

**字典：** `repository_pull_status`（RUNNING 拉取中 / SUCCESS 成功 / FAILED 失败），前端状态文案经 `useDict` 获取。

### 7.3 服务层设计（CodeRepositoryService）

```java
// backend/platform-server/src/main/java/com/platform/repository/service/CodeRepositoryService.java

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeRepositoryService {

    // ── CRUD ──────────────────────────────────────────────
    public List<RepositoryResponse> listByProject(Long projectId);
    public RepositoryResponse create(Long projectId, RepositoryCreateRequest request);
    // 校验项目存在(PROJECT_NOT_FOUND) + 项目内重名(2201)；密码非空时 AesCryptoUtil.encrypt 加密入库
    public RepositoryResponse update(Long projectId, Long repoId, RepositoryUpdateRequest request);
    // 密码留空保持不变；名称变更时排除自身做重名校验
    public void delete(Long projectId, Long repoId);
    // 物理删除记录 + FileUtil.del 递归删除本地目录 {storage-path}/{projectId}/{repoId}；pull_log 级联删除

    // ── 拉取（同步执行 + 历史记录）─────────────────────────
    @Transactional
    public PullResultResponse pull(Long projectId, Long repoId);
    // 失败不抛异常：捕获 GitAPIException/IOException 等转为 success=false 返回 HTTP 200

    public List<PullLogResponse> getPullLogs(Long projectId, Long repoId);
    // 默认返回最近 20 条（ORDER BY id DESC LIMIT 20）

    // ── 私有方法 ──────────────────────────────────────────
    private CodeRepository findById(Long projectId, Long repoId);          // 2200 REPOSITORY_NOT_FOUND
    private void checkNameDuplicate(Long projectId, String name, Long excludeId); // 2201 重名
    private File buildLocalDir(Long projectId, Long repoId);               // {storage-path}/{projectId}/{repoId}
    private boolean isGitRepository(File dir);                             // .git 目录存在判定
    private PullResultResponse doClone(CodeRepository repo, File localDir);   // CLONE 分支
    private PullResultResponse doPull(CodeRepository repo, File localDir);    // PULL 分支（fetch + checkout + pull）
    private void checkoutBranchIfNeeded(Git git, String branch) throws GitAPIException;
    // 当前分支 ≠ 配置分支时 checkout；本地不存在则从远程跟踪分支创建并关联上游
    private CredentialsProvider buildCredentials(CodeRepository repo);    // 用户名 + 解密密码
    private String resolveHeadCommitId(Git git) throws IOException;       // repository.resolve(HEAD)
    private void finishPullLog(CodeRepositoryPullLog log, String status, String commitId, String message, long durationMs);
    private void updateRepositoryAfterPull(CodeRepository repo, boolean success, String commitId, String localPath);
}
```

**pull 核心流程（拉取状态机）：**

```
                 ┌──────────────────────────────────────┐
                 │  pull(projectId, repoId) 入口          │
                 └──────────────────┬───────────────────┘
                                    ▼
                 ┌──────────────────────────────────────┐
                 │ findById 校验仓库存在（2200）           │
                 │ buildLocalDir 计算本地目录              │
                 └──────────────────┬───────────────────┘
                                    ▼
              ┌── isGitRepository(dir)? ──────────────────┐
              │ 否（不存在/残留）        │ 是                  │
              ▼                        ▼                    │
   ┌──────────────────┐    ┌──────────────────────┐        │
   │ INSERT pull_log   │    │ INSERT pull_log       │        │
   │ pull_type=CLONE   │    │ pull_type=PULL        │        │
   │ status=RUNNING    │    │ status=RUNNING        │        │
   └─────────┬────────┘    └──────────┬───────────┘        │
             ▼                        ▼                     │
   ┌──────────────────┐    ┌──────────────────────┐        │
   │ Git.clone         │    │ fetch（同步远程引用    │        │
   │  .setURI(gitUrl)  │    │  setRemoveDeletedRefs）│       │
   │  指定分支时         │    │ checkoutBranchIfNeeded │      │
   │  setBranch(refs/  │    │ PullCommand            │       │
   │  heads/{branch})  │    └──────────┬───────────┘        │
   │  setTimeout(300s) │               │                     │
   └─────────┬────────┘               │                     │
             └──────────┬──────────────┘                     │
                        ▼                                    │
             （认证：UsernamePasswordCredentialsProvider      │
               仅当 authUsername 与解密密码均非空）             │
                        │                                    │
             ┌──────────┴──────────┐                         │
             ▼ 成功                 ▼ 失败(GitAPIException/  │
        ┌─────────┐               IOException)               │
        │resolve  │        ┌──────────────────┐              │
        │HEAD     │        │ CLONE: 清理残留目录 │              │
        │commitId │        │ message=e原因      │              │
        └────┬────┘        └────────┬─────────┘              │
             ▼                      ▼                        │
        ┌──────────────────────────────────┐                 │
        │ finishPullLog:                    │                 │
        │  SUCCESS（commitId、durationMs）   │                 │
        │  FAILED（message、durationMs）     │                 │
        │ updateRepositoryAfterPull:        │                 │
        │  回写 last_pull_* / local_path    │                 │
        └──────────────────┬───────────────┘                 │
                           ▼                                 │
        ┌──────────────────────────────────────┐             │
        │ 返回 PullResultResponse               │             │
        │  成功: success=true + commitId        │             │
        │  失败: success=false + message        │             │
        │  （均为 HTTP 200，前端按业务结果提示）  │             │
        └──────────────────────────────────────┘
```

**凭证加密（AesCryptoUtil）：**

```java
// backend/platform-api/src/main/java/com/platform/common/util/AesCryptoUtil.java
public static String encrypt(String plain, String key);  // AES-128/CBC/PKCS5Padding，随机 IV 前置 + Base64，返回 enc: + base64
public static String decrypt(String cipher, String key); // 剥离 enc: 前缀解密；无前缀旧值原样返回
```

### 7.4 DTO 设计

| DTO | 字段 | 说明 |
|---|---|---|
| `RepositoryCreateRequest` | projectId、name(@NotBlank @Size(max=50))、gitUrl(@NotBlank @Size(max=500))、branch(@Size(max=100))、description(@Size(max=255))、authUsername(@Size(max=200))、authPassword(@Size(max=500)) | 新建请求 |
| `RepositoryUpdateRequest` | 同上 | authPassword 留空=保持不变 |
| `RepositoryResponse` | id、projectId、name、gitUrl、branch、description、authUsername、hasAuth、localPath、lastPullAt、lastPullStatus、lastCommitId、createdAt | 不回传密码，仅 hasAuth 布尔 |
| `PullResultResponse` | logId、success、pullType、branch、commitId、message、durationMs、finishedAt | 拉取结果（失败也 200） |
| `PullLogResponse` | id、pullType、branch、status、commitId、message、durationMs、createdAt | 拉取历史条目 |

### 7.5 API 路由签名（CodeRepositoryController）

```java
@RestController
@RequestMapping("/api/v1/projects/{projectId}/repositories")
public class CodeRepositoryController {

    @GetMapping                       ApiResponse<List<RepositoryResponse>> list(@PathVariable Long projectId);
    @PostMapping                      ApiResponse<RepositoryResponse> create(@PathVariable Long projectId,
                                                            @Valid @RequestBody RepositoryCreateRequest request);
    @PostMapping("/{repoId}")          ApiResponse<RepositoryResponse> update(@PathVariable Long projectId, @PathVariable Long repoId,
                                                            @Valid @RequestBody RepositoryUpdateRequest request);
    @PostMapping("/{repoId}/delete")   ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long repoId);
    @PostMapping("/{repoId}/pull")     ApiResponse<PullResultResponse> pull(@PathVariable Long projectId, @PathVariable Long repoId);
    @GetMapping("/{repoId}/pull-logs") ApiResponse<List<PullLogResponse>> pullLogs(@PathVariable Long projectId, @PathVariable Long repoId);
}
```

**配置项（application.yml）：**

```yaml
repository:
  storage-path: ./data/repos        # 代码存储根目录（相对启动目录）
  crypto-key: AutoTestRepo2026      # 凭证 AES 密钥（16 字节，可覆盖）
  clone-timeout-seconds: 300        # JGit 克隆/拉取超时（秒）
```

### 7.6 错误码明细与测试边界

**错误码（M11 段 2200-2299）：**

| 错误码 | HTTP | 触发场景 |
|---|---|---|
| 2200 REPOSITORY_NOT_FOUND | 404 | 仓库不存在或不属于当前项目 |
| 2201 REPOSITORY_NAME_DUPLICATE | 409 | 项目内仓库名称已存在 |
| 2202 REPOSITORY_CRYPTO_ERROR | 500 | 凭证 AES 加解密失败 |

**测试边界：**

| 场景类型 | 测试点 |
|---|---|
| 正常流程 | 新建/编辑/删除仓库；名称长度与必填校验；项目内重名校验（编辑排除自身） |
| 代码拉取 | 首次拉取触发 CLONE；再次拉取触发 PULL；指定分支克隆正确分支 |
| 代码拉取 | PULL 前 fetch 同步远程引用；配置分支与当前分支不一致时自动切换；远程新建分支本地不存在时从跟踪分支创建 |
| 代码拉取 | 错误 Git 地址返回 success=false + 具体原因（HTTP 200），拉取历史记录 FAILED |
| 代码拉取 | CLONE 失败后清理残留目录，下次拉取可重新克隆 |
| 认证 | 私有仓库用户名+密码/Token 认证成功拉取；认证信息错误时记录失败原因 |
| 认证 | 密码 AES 加密入库（enc: 前缀）；列表接口不回传密码仅 hasAuth；编辑留空保持不变 |
| 拉取历史 | 每次拉取（含失败）产生一条记录；RUNNING→SUCCESS/FAILED 状态更新；耗时统计；最近 20 条排序 |
| 删除 | 删除仓库同时递归清理本地代码目录；pull_log 级联删除；删除确认提示包含本地目录警告 |
| 权限 | 菜单 project:repositories；按钮 project:repo:add/pull/edit/delete/logs 各自控制 |