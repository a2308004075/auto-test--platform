# auto-test-platform 概要设计文档

> 版本：v1.0  
> 文档状态：初稿  
> 基线文档：[SRS v1.3](SRS.md) | [PRD V1.17](PRD.md) | [UI 原型](../ui/index.html)

---

## 1. 引言

### 1.1 文档目的

本文档是 auto-test-platform 的概要设计文档（High-Level Design），在需求规格说明书（SRS）和产品需求文档（PRD）的基础上，对系统进行模块划分、明确各模块的职责边界与对外接口、描述模块间的依赖关系和数据流转，为后续详细设计和编码提供架构级指导。

### 1.2 项目概述

auto-test-platform 是一个**通用的关键字驱动测试管理平台**，面向任意行业的 HTTP API 自动化测试场景。平台基于 postman-tool 核心引擎，提供 Web 化的全流程管理能力，覆盖接口管理、关键字编排、用例组织、执行调度和报告分析。

核心设计思想为**四层封装体系**：

```
基础能力层 → 关键字封装层 → Action 关键字层 → 测试用例
```

每一层封装后形成新的可复用单元，自底向上逐层组装。

### 1.3 技术栈概要

| 层 | 技术选型 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Ant Design Vue 4.x + Pinia + ECharts + Monaco Editor + AntV X6 |
| 后端 | Java 1.8 + Spring Boot 2.7（单体应用） |
| 后端组件 | Spring Security 5.7 + JJWT 0.11（JWT 认证）+ Spring AMQP（异步消息）+ OkHttp 4.12（HTTP 客户端） |
| 数据持久层 | MyBatis-Plus 3.5 + Flyway 8 + Spring Security 5.7 + Sa-Token + JJWT 0.11 |
| 数据库 | MySQL 8.0+ + Redis 7.x |
| 消息队列 | RabbitMQ 3.x（异步事件驱动：测试执行触发、状态通知） |
| 任务调度 | XXL-Job 2.4+（分布式定时调度）+ Spring Async（服务内异步） |
| 执行引擎 | 内嵌于 pp-execution（OkHttp 4.12 客户端 + Groovy ScriptEngine 沙箱 + swagger-parser + JavaParser） |
| 部署 | Docker Compose（推荐）/ Nginx + Spring Boot 单体应用 |

### 1.4 术语约定

| 术语 | 含义 |
|---|---|
| Keyword | 统一关键字实体，通过 `keyword_type` 区分 API / TOOL / ACTION / TEST_CASE |
| 接口关键字 | Keyword(type=API) + ApiEndpoint，接口 + 测试数据的封装 |
| 工具方法关键字 | ToolMethod 实体，通过 `keyword` 字段标识（≤20字符） |
| Action 关键字 | Keyword(type=ACTION) + Action，通过流程画布编排节点连线 |
| 四层 Setup/Teardown | 套件级·整体、套件级·每条、套件内用例级、用例级四层生命周期钩子 |

---

## 2. 系统架构概览

### 2.1 逻辑架构

系统采用前后端分离架构，整体分为前端层、后端服务层和数据层三个逻辑层次：

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        前端层（Vue 3 SPA）                                │
│                                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ 认证模块  │ │ 项目管理  │ │ 接口文档  │ │ 关键字管理│ │ 测试/执行/报告│   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│  ┌──────────┐ ┌──────────┐                                               │
│  │ 环境配置  │ │ 系统设置  │  + AntV X6 流程画布 + Monaco Editor          │
│  └──────────┘ └──────────┘                                               │
└─────────────────────────────────┬────────────────────────────────────────┘
                                  │ HTTP REST / WebSocket
┌─────────────────────────────────▼────────────────────────────────────────┐
│                  后端服务层（Spring Boot 单体应用）                         │
│                                                                          │
│  JWT 鉴权过滤 │ CORS │ 全局异常处理 │ 统一响应格式                      │
└─────────────────────────────────┬────────────────────────────────────────┘
                                  │ Spring Bean 依赖注入
┌─────────────────────────────────▼────────────────────────────────────────┐
│                         功能模块层                                        │
│                                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ auth     │ │ project  │ │ api      │ │ keyword  │ │ execution    │   │
│  │ 认证模块  │ │ 项目模块  │ │ 接口模块  │ │ 关键字模块│ │ 执行模块      │   │
│  │ M1认证   │ │ M2项目   │ │ M4接口   │ │ M5接口KW │ │ M8用例       │   │
│  │ 用户管理  │ │ M3环境   │ │ 文档管理  │ │ M6工具KW │ │ M9执行       │   │
│  │ 系统配置  │ │ 配置管理  │ │          │ │ M7Action │ │ M10报告      │   │
│  └──────────┘ └──────────┘ └──────────┘ └────┬─────┘ └──────────────┘   │
│                                               │                           │
│  ┌─── 基础设施 ───────────────────────────────┼───────────────────────┐  │
│  │ RabbitMQ 异步事件 │ XXL-Job 定时调度 │ WebSocket 实时推送            │  │
│  │ EasyExcel/PDF 报告生成 │ Redis 缓存                                    │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─── 执行引擎（内置于 execution 模块） ──────────────────────────────────┐  │
│  │ HTTP客户端(OkHttp) │ 断言引擎 │ Groovy沙箱 │ Swagger解析器      │  │
│  │ Action流程执行器 │ AST解析器 │ 协议适配器(WSS/Shell)           │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
       │                    │                    │
  ┌────▼────┐         ┌────▼────┐         ┌─────▼─────┐
  │  MySQL  │         │  Redis  │         │ 文件存储   │
  │ 业务数据 │         │ 缓存    │         │ 报告/日志  │
  └─────────┘         └─────────┘         └───────────┘
```

### 2.2 部署架构

支持两种部署方式：

| 方式 | 组件 | 说明 |
|---|---|---|
| Docker Compose（推荐） | nginx / backend / rabbitmq / mysql / redis | 一键编排，适合开发和中小规模生产 |
| Kubernetes | Nginx Ingress + Spring Boot 后端 Pod + RabbitMQ + MySQL + Redis | 适合大规模生产环境，支持弹性伸缩 |

**Docker Compose 容器编排：**

```yaml
services:
  nginx:          # 前端静态资源 + API 反向代理
  backend:        # Spring Boot 单体应用（包含所有功能模块 M1~M10 + 内置执行引擎）
  rabbitmq:       # 消息队列（异步事件驱动）
  mysql:          # MySQL 8.0+
  redis:          # Redis 7.x（缓存）
```

**Kubernetes 部署拓扑：**

```
Nginx Ingress Controller
  ├── /static/*           → frontend (Deployment)
  ├── /api/*              → backend (Deployment + HPA)
  └── /ws/*               → backend (Deployment)
      ↓
RabbitMQ + MySQL + Redis
```

---

### 2.3 后端功能模块划分

后端采用 Spring Boot 单体应用架构，将 10 个功能模块组织在同一个应用内，按业务领域划分为 5 个模块包：

| 模块包 | 包含模块 | 核心职责 |
|---|---|---|
| auth | M1 | 用户认证、JWT 签发/刷新、RBAC 权限、用户 CRUD、全局配置 |
| project | M2, M3 | 项目管理、环境配置 |
| api | M4 | 接口文档、Swagger 导入、接口调试 |
| keyword | M5, M6, M7 | 接口关键字、工具方法、Action 画布、删除保护链 |
| execution | M8, M9, M10 | 测试套件、用例编排、执行调度、实时推送、报告分析、**内置执行引擎** |

**模块间通信：**

| 通信方式 | 场景 | 技术 |
|---|---|---|
| 同步调用 | 模块间直接方法调用（如执行时获取关键字详情、环境配置） | Spring Bean 依赖注入 |
| 异步消息 | 测试执行触发、执行状态变更通知、报告生成 | RabbitMQ + Spring AMQP |
| 数据存储 | 所有模块共享同一数据库实例，操作各自表 | MySQL |
| 缓存 | JWT 黑名单、执行状态缓存 | Redis |

**模块间依赖关系：**

```
auth ←─── project ←─── api ←─── keyword ←─── execution
  │                                           │
  │                                           │ (内置执行引擎)
  └───────────────────────────────────────────┘
           (所有模块均依赖 auth 的 JWT 验证能力)
```

---

## 3. 模块划分与职责

基于 PRD 和 SRS 的功能需求，将系统划分为以下 **10 个功能模块**：

### 3.1 模块总览

| 序号 | 模块名称 | 需求编号 | 核心职责 | UI 页面数 |
|---|---|---|---|---|
| M1 | 认证与用户管理 | AUTH-001, AUTH-002 | 用户登录认证、JWT Token 管理、RBAC 权限控制、用户 CRUD | 3 |
| M2 | 项目管理 | PM-001 ~ PM-003 | 项目 CRUD、项目概览仪表板、项目设置 | 2 |
| M3 | 环境配置管理 | ENV-001, ENV-002 | 环境 CRUD、JSON 配置编辑、环境激活切换、连接测试 | 1 |
| M4 | 接口文档 | API-001 ~ API-007 | 接口分组、Swagger 导入、接口 CRUD、接口调试、接口同步、批量操作 | 5 |
| M5 | 接口关键字管理 | KW-001 ~ KW-005 | 接口关键字 CRUD、测试数据配置、预期响应、删除保护、传参返回 | 3 |
| M6 | 工具方法关键字管理 | TOOL-001 ~ TOOL-006 | 内置/自定义工具方法管理、代码沙箱执行、在线测试、传参返回 | 3 |
| M7 | Action 关键字管理 | ACT-001 ~ ACT-004 | Action CRUD、流程画布编排、Action 调试、引用管理、删除保护 | 3 |
| M8 | 测试用例管理 | CASE-001 ~ CASE-004 | 套件管理、用例 CRUD、步骤编排器、校验配置、参数化、四层 Setup/Teardown | 4 |
| M9 | 测试执行与调度 | EXEC-001, EXEC-002 | 测试计划 CRUD、执行触发（手动/定时/CI）、实时状态推送 | 4 |
| M10 | 测试报告与分析 | RPT-001 ~ RPT-005 | 执行详情、执行历史、趋势分析、历史对比、PDF/Excel 报告导出 | 2 |

> **系统设置**（SET-001 全局配置、SET-002 通知配置）归入 M1 认证与用户管理模块中，作为系统级配置子功能，不单独成模块。

---

### 3.2 M1 — 认证与用户管理模块

#### 职责

- 提供用户名 + 密码登录认证，基于 JWT 双 Token 机制（Access Token 2h + Refresh Token 7d）
- 实现 RBAC 角色权限控制：ADMIN（全权限）/ USER（测试操作权限）
- 管理员对用户的 CRUD 管理（创建、编辑、禁用、删除、重置密码）
- 个人资料管理（基本信息编辑、修改密码、登录记录查看）
- 全局系统配置管理（执行超时、并发上限、保留策略）
- admin 账号保护（不可修改用户名/账号/角色、不可删除、不可禁用）
- 用户名/账号保留规则校验

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 用户凭据、用户管理操作、全局配置变更 |
| 输出 | JWT Token、用户信息、权限标识、全局配置值 |
| 不负责 | 业务数据的 CRUD（由各自模块负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 登录 | POST | `/api/v1/auth/login` | 用户认证，返回双 Token |
| 刷新 Token | POST | `/api/v1/auth/refresh` | Refresh Token 换取新 Access Token |
| 登出 | POST | `/api/v1/auth/logout` | 标记 Token 失效 |
| 当前用户 | GET | `/api/v1/auth/me` | 返回当前登录用户信息 |
| 用户 CRUD | GET/POST/PUT/DELETE | `/api/v1/users[/:id]` | 管理员用户管理 |
| 用户状态 | PATCH | `/api/v1/users/:id/status` | 启用/禁用 |
| 重置密码 | POST | `/api/v1/users/:id/reset-password` | 管理员重置用户密码 |
| 全局配置 | GET/PUT | `/api/v1/settings` | 获取/更新全局配置 |

#### 数据实体

- `User`：用户实体（username, password_hash, display_name, role, is_active）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 登录页 | `/login` |
| 用户管理 | `/settings/users` |
| 个人资料 | `/settings/profile` |
| 全局配置 | `/settings/config` |

---

### 3.3 M2 — 项目管理模块

#### 职责

- 项目 CRUD（创建、编辑、启停、软删除）
- 项目列表展示（卡片视图、搜索、状态筛选）
- 项目概览仪表板：五层渐进式信息架构（项目信息 → 质量健康度 → KPI 卡片 → 趋势分析 → 覆盖率与风险）
- 项目源码路径配置（指定服务端服务器上的源码目录）

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 项目基础信息、源码路径 |
| 输出 | 项目列表、项目详情、仪表板统计数据 |
| 不负责 | 项目下的资源管理（接口、关键字、用例等由各自模块负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 项目列表 | GET | `/api/v1/projects` | 支持搜索和状态筛选 |
| 创建项目 | POST | `/api/v1/projects` | 创建后自动跳转概览 |
| 项目详情 | GET | `/api/v1/projects/:id` | 含完整字段 |
| 更新项目 | PUT | `/api/v1/projects/:id` | 编辑名称、描述 |
| 启停项目 | PATCH | `/api/v1/projects/:id/status` | 启用/停用 |
| 删除项目 | DELETE | `/api/v1/projects/:id` | 软删除 |
| 项目概览 | GET | `/api/v1/projects/:id/dashboard` | 五层仪表板数据聚合 |

#### 数据实体

- `Project`：项目实体（name, description, source_path, is_active）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 项目列表 | `/projects` |
| 项目概览 | `/projects/:id` |

#### 仪表板数据依赖

项目概览仪表板的数据聚合依赖以下模块提供原始数据：
- M4（接口覆盖率）→ M5（关键字覆盖率）→ M8（用例执行结果）→ M9（执行记录）→ M10（趋势数据）

---

### 3.4 M3 — 环境配置管理模块

#### 职责

- 环境 CRUD（创建、编辑、删除）
- 环境激活切换（每个项目同时只有一个激活环境）
- JSON 配置编辑器（Monaco Editor，语法高亮、格式化、JSON 校验）
- 连接测试（保存前测试目标服务连通性）
- 配置字段与引擎侧 `env.json` 结构对齐（host, host_authorization, wss, nacos 等）

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 环境名称、JSON 配置、激活操作 |
| 输出 | 环境列表、配置 JSON、连接测试结果 |
| 不负责 | 引擎运行时的环境读取（由引擎适配层从 DB 读取） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 环境列表 | GET | `/api/v1/projects/:pid/environments` | 项目下所有环境 |
| 创建环境 | POST | `/api/v1/projects/:pid/environments` | 含 config JSON |
| 更新环境 | PUT | `/api/v1/projects/:pid/environments/:id` | 更新配置 |
| 删除环境 | DELETE | `/api/v1/projects/:pid/environments/:id` | 激活环境不可删 |
| 激活环境 | PATCH | `/api/v1/projects/:pid/environments/:id/activate` | 设为当前激活 |
| 测试连接 | POST | `/api/v1/projects/:pid/environments/:id/test` | 返回延迟毫秒数 |

#### 数据实体

- `Environment`：环境实体（project_id, name, config JSON, is_active）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 环境配置 | `/projects/:id/environments` |

---

### 3.5 M4 — 接口文档模块

#### 职责

- 接口分组管理（系统默认分组「全部」+「未分组」不可编辑删除，自定义分组 CRUD）
- Swagger 2.0 JSON 导入向导（三步：上传 → 解析预览 → 确认导入，支持增量）
- 接口 CRUD（手动创建、编辑基础信息/参数/请求体/响应定义）
- 接口在线调试（选择环境 → 填写参数 → 发送请求 → 展示响应）
- 接口同步（重新上传 Swagger，差异对比，确认后更新）
- 接口删除保护（检查下游关键字和用例依赖，被引用接口禁止删除）
- 批量操作（启用/禁用、移动分组、删除，删除时逐条执行依赖检查）

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | Swagger 文件、接口定义、分组操作、调试请求 |
| 输出 | 接口列表、分组树、调试响应、依赖检查结果 |
| 不负责 | 接口关键字的创建和管理（由 M5 负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 接口列表 | GET | `/api/v1/projects/:pid/apis` | 支持树形/平铺 |
| 分组 CRUD | GET/POST/PUT/DELETE | `/api/v1/projects/:pid/apis/modules[/:id]` | 分组管理 |
| 接口 CRUD | GET/POST/PUT/DELETE | `/api/v1/projects/:pid/apis[/:id]` | 接口文档 |
| Swagger 导入 | POST | `/api/v1/projects/:pid/apis/import/swagger` | 三步向导 |
| 接口同步 | POST | `/api/v1/projects/:pid/apis/sync` | Swagger 差异同步 |
| 接口调试 | POST | `/api/v1/projects/:pid/apis/:id/debug` | 发送请求测试 |
| 批量操作 | POST | `/api/v1/projects/:pid/apis/batch` | 批量启用/禁用/移动/删除 |

#### 数据实体

- `ApiModule`：接口分组（project_id, name, service_prefix, source_type）
- `ApiEndpoint`：接口（module_id, name, path, method, parameters JSON, request_body JSON, responses JSON）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 接口列表 | `/projects/:id/apis` |
| Swagger 导入 | `/projects/:id/apis/import` |
| 接口编辑 | `/projects/:id/apis/:apiId/edit` |
| 接口调试 | `/projects/:id/apis/:apiId/debug` |

---

### 3.6 M5 — 接口关键字管理模块

#### 职责

- 接口关键字 CRUD（选择接口 + 配置测试数据 + 预期响应）
- 分类分组展示、搜索和筛选
- 从接口详情页快速创建接口关键字（自动填充参数）
- 在线调试弹窗（选择环境 → 编辑参数测试值 → 发送请求 → 查看响应和断言结果）
- 删除保护（被 Action 引用的接口关键字不可删除，展示依赖详情）
- 传参返回机制：`args` 覆盖预设测试数据，`save_as` 存入上下文变量
- 引用关系查看（哪些 Action 引用了该关键字）

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 关联接口选择、测试数据配置、预期响应 |
| 输出 | 接口关键字列表、详情、调试结果、依赖检查结果 |
| 不负责 | 接口本身的管理（由 M4 负责）；Action 中的引用管理（由 M7 负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 关键字列表 | GET | `/api/v1/projects/:pid/keywords/api` | 按分类筛选 |
| 创建关键字 | POST | `/api/v1/projects/:pid/keywords/api` | 选择接口 + 配置 |
| 更新关键字 | PUT | `/api/v1/projects/:pid/keywords/api/:id` | 编辑测试数据 |
| 删除关键字 | DELETE | `/api/v1/projects/:pid/keywords/api/:id` | 含依赖检查 |
| 依赖查询 | GET | `/api/v1/projects/:pid/keywords/:id/dependencies` | 引用关系 |
| 快速创建 | POST | `/api/v1/projects/:pid/apis/:apiId/quick-keyword` | 接口快速生成 |

#### 数据实体

- `Keyword`（keyword_type=API）：统一关键字元数据（name, input_params, output_params, config JSON）
- `ApiKeyword`：接口关键字源实体（keyword_id → Keyword, endpoint_id → ApiEndpoint）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 接口关键字列表 | `/projects/:id/keywords?type=api` |
| 创建接口关键字 | `/projects/:id/keywords/api/create` |
| 编辑接口关键字 | `/projects/:id/keywords/api/:kwId/edit` |

---

### 3.7 M6 — 工具方法关键字管理模块

#### 职责

- 内置工具方法管理（加密、时间、数据转换等平台预置方法）
- 自定义工具方法 CRUD（名称、关键字标识、代码、参数定义）
- 代码沙箱执行（白名单库、禁止文件/网络/子进程操作、5s 超时、64MB 内存限制）
- 在线测试（填写参数 → 执行 → 展示返回值和耗时）
- 传参返回机制：`args` 传入参数，`return` 返回结果，`save_as` 存入上下文
- 关键字标识管理（`keyword` 字段，≤20字符，项目内唯一）

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 函数代码、参数定义、关键字标识 |
| 输出 | 工具方法列表、在线测试结果、沙箱执行日志 |
| 不负责 | 工具方法在 Action 节点中的引用管理（由 M7 负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 工具方法列表 | GET | `/api/v1/projects/:pid/tool-methods` | 按分类筛选 |
| 创建工具方法 | POST | `/api/v1/projects/:pid/tool-methods` | 含代码和参数 |
| 更新工具方法 | PUT | `/api/v1/projects/:pid/tool-methods/:id` | 编辑代码和配置 |
| 删除工具方法 | DELETE | `/api/v1/projects/:pid/tool-methods/:id` | 所有记录均可删 |
| 在线测试 | POST | `/api/v1/projects/:pid/tool-methods/:id/test` | 沙箱执行 |

#### 数据实体

- `ToolMethod`：工具方法实体（project_id, name, keyword, category, code, parameters JSON, return_type, is_builtin）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 工具方法关键字列表 | `/projects/:id/keywords?type=tool` |
| 创建工具方法关键字 | `/projects/:id/keywords/tool/create` |
| 编辑工具方法关键字 | `/projects/:id/keywords/tool/:toolId/edit` |

---

### 3.8 M7 — Action 关键字管理模块

#### 职责

- Action 关键字 CRUD（名称、描述、分类、输入/输出参数定义）
- **流程画布编排器**（核心功能，基于 AntV X6）：
  - 三栏布局：左侧元素面板（关键字列表）→ 中间流程图画布 → 右侧属性面板
  - 节点类型：接口关键字（蓝）、工具方法关键字（绿）、Action 关键字（橙）、逻辑判断（紫）、监听器（灰）、断言（红）
  - 串行（单线连接）、并行（扇出多线）、条件分支（菱形节点「是」/「否」出口）
  - 画布工具栏：缩放、删除选中、一键格式化、一键清空、全屏
  - 保存时序列化流程图拓扑为 nodes JSON
- Action 调试（选择环境 → 填写参数 → 按画布拓扑顺序执行 → 展示逐节点结果）
- 引用管理与删除保护（被用例或其他 Action 引用时不可删除）
- Action-to-Action 嵌套调用支持

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 流程画布节点编排数据、输入/输出参数定义 |
| 输出 | Action 列表、画布数据（nodes JSON）、调试执行结果、引用关系 |
| 不负责 | 接口关键字/工具方法关键字自身的 CRUD（由 M5/M6 负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| Action 列表 | GET | `/api/v1/projects/:pid/actions` | 按分类筛选 |
| 创建 Action | POST | `/api/v1/projects/:pid/actions` | 含 nodes JSON |
| 更新 Action | PUT | `/api/v1/projects/:pid/actions/:id` | 含画布数据 |
| 删除 Action | DELETE | `/api/v1/projects/:pid/actions/:id` | 含依赖检查 |
| Action 调试 | POST | `/api/v1/projects/:pid/actions/:id/debug` | 按画布拓扑执行 |
| 引用查询 | GET | `/api/v1/projects/:pid/actions/:id/references` | 被引用关系 |

#### 数据实体

- `Keyword`（keyword_type=ACTION）：统一关键字元数据
- `Action`：Action 源实体（project_id, nodes JSON）

#### UI 页面

| 页面 | 路径 |
|---|---|
| Action 关键字列表 | `/projects/:id/keywords?type=action` |
| Action 编辑器 | `/projects/:id/keywords/action/:actionId/edit` |
| Action 调试 | `/projects/:id/keywords/action/:actionId/debug` |

---

### 3.9 M8 — 测试用例管理模块

#### 职责

- **测试套件管理**：套件 CRUD、套件内用例排序、标签管理、四层 Setup/Teardown 配置
  - 套件级·整体 Setup/Teardown（独立开关，整个套件执行前/后调用一次）
  - 套件级·每条 Setup/Teardown（独立开关，每条用例执行前/后调用）
  - 套件内用例级差异化 Setup/Teardown（通过 SuiteCaseLifecycle 关联实体）
- **测试用例 CRUD**：列表/卡片双视图、多维度搜索筛选（套件/标签/优先级/状态）、批量操作
- **用例步骤编排器**（核心功能，三栏布局与 Action 编辑器交互一致）：
  - 关键字步骤（引用 Keyword，按 keyword_type 筛选 API/TOOL/ACTION）
  - 控制流步骤（串行/并行/条件/等待）
  - 校验配置（6 种断言：equal/not_equal/include/not_include/true/not_true）
  - 变量引用（`${var}` 引用上游输出或环境变量）
- **用例级 Setup/Teardown**（可选，在所有场景通用）
- **参数化数据驱动**：手动输入表格或 CSV 导入
- **用例调试**：单用例在线调试，WebSocket 实时日志推送

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 用例步骤编排数据、校验配置、参数化数据、Setup/Teardown 步骤 |
| 输出 | 用例列表、用例详情、调试结果 |
| 不负责 | 执行调度和执行记录管理（由 M9 负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 套件 CRUD | GET/POST/PUT/DELETE | `/api/v1/projects/:pid/suites[/:id]` | 套件管理 |
| 添加/移除用例 | POST/DELETE | `/api/v1/projects/:pid/suites/:id/cases[/:caseId]` | 套件内用例操作 |
| 用例级生命周期 | GET/PUT/DELETE | `/api/v1/projects/:pid/suites/:id/cases/:caseId/lifecycle` | 差异化 Setup/Teardown |
| 用例列表 | GET | `/api/v1/projects/:pid/cases` | 多维度筛选 |
| 用例 CRUD | GET/POST/PUT/DELETE | `/api/v1/projects/:pid/cases[/:id]` | 用例管理 |
| 用例调试 | POST | `/api/v1/projects/:pid/cases/:id/debug` | WebSocket 实时推送 |
| 批量操作 | POST | `/api/v1/projects/:pid/cases/batch` | 批量启用/禁用/移动/打标签/删除 |

#### 数据实体

- `TestSuite`：测试套件（project_id, name, tags, once_setup_steps, once_teardown_steps, per_case_setup_steps, per_case_teardown_steps, enable 开关）
- `TestCase`：测试用例（suite_id, steps JSON, setup_steps, teardown_steps, assertions, priority）
- `SuiteCaseLifecycle`：套件内用例级生命周期关联实体（suite_id, case_id, setup_steps, teardown_steps）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 测试套件 | `/projects/:id/suites` |
| 用例列表 | `/projects/:id/cases` |
| 用例编辑 | `/projects/:id/cases/:caseId/edit` |

---

### 3.10 M9 — 测试执行与调度模块

#### 职责

- **测试计划管理**：计划 CRUD、关联测试套件（多选）、绑定执行环境
- **定时执行配置**：可视化 Cron 编辑器（分/时/日/月/周），启停开关，执行时间预览
- **执行触发**：
  - 手动执行：点击「立即执行」，可选覆盖环境
  - 定时执行：Spring Task Scheduler 按 cron 表达式自动触发
  - CI/CD Webhook（预留）：提供 Webhook 接口供外部系统触发
- **执行状态管理**：PENDING → RUNNING → COMPLETED/FAILED/CANCELLED
- **实时状态推送**：WebSocket 推送执行进度（第 N/M 个用例）和最终状态
- **执行记录管理**：历史记录列表、多维度筛选、一键重新执行

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 测试计划配置、执行触发指令、定时规则 |
| 输出 | 执行记录、执行状态、实时进度推送 |
| 不负责 | 执行结果的分析和报告生成（由 M10 负责）；用例步骤的具体执行逻辑（委托给引擎层） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 计划列表 | GET | `/api/v1/projects/:pid/plans` | 搜索和状态筛选 |
| 计划 CRUD | GET/POST/PUT/DELETE | `/api/v1/projects/:pid/plans[/:id]` | 计划管理 |
| 手动执行 | POST | `/api/v1/projects/:pid/plans/:id/execute` | 创建执行记录 |
| 定时配置 | POST | `/api/v1/projects/:pid/plans/:id/schedule` | 配置 cron |
| 执行历史 | GET | `/api/v1/projects/:pid/executions` | 多维度筛选 |
| 执行详情 | GET | `/api/v1/projects/:pid/executions/:id` | 含概要统计 |
| 执行结果 | GET | `/api/v1/projects/:pid/executions/:id/cases[/:caseId/steps]` | 用例/步骤级结果 |
| 取消执行 | POST | `/api/v1/projects/:pid/executions/:id/cancel` | 取消正在执行 |
| 执行日志 | GET | `/api/v1/projects/:pid/executions/:id/logs` | 日志查询 |
| CI Webhook | POST | `/api/v1/webhook/execute/:planId` | 外部触发（预留） |
| WebSocket | WS | `/ws/projects/:pid/executions/:id` | 实时状态推送 |

#### 数据实体

- `TestPlan`：测试计划（project_id, suite_ids JSON, environment_id, schedule_cron）
- `TestExecution`：执行记录（plan_id, trigger_type, status, total/passed/failed/skipped_cases, duration_ms）
- `TestResult`：测试结果明细（execution_id, case_id, status, logs JSON, duration_ms）

#### UI 页面

| 页面 | 路径 |
|---|---|
| 测试计划列表 | `/projects/:id/plans` |
| 计划编辑 | `/projects/:id/plans/:planId/edit` |
| 执行记录 | `/projects/:id/executions` |
| 执行详情 | `/projects/:id/executions/:execId` |

---

### 3.11 M10 — 测试报告与分析模块

#### 职责

- **执行详情报告**：概要统计（总数/通过/失败/跳过/耗时）、通过率进度环、用例结果列表、步骤级日志追溯
- **执行历史**：按时间倒序展示、多维度筛选（计划/状态/触发方式/时间范围）、一键重新执行
- **趋势分析**：通过率折线图（天/周/月粒度）、执行耗时趋势、模块失败率热力图、高频失败 TOP 10
- **历史对比**：选择两次执行记录对比（概要差异、新增失败/修复/持续失败用例、耗时对比柱状图）
- **报告导出**：PDF 报告（封面+概要+图表+详细结果+失败详情）、Excel 报告（概要+用例结果+步骤详情三个 Sheet）

#### 边界

| 边界项 | 说明 |
|---|---|
| 输入 | 执行记录 ID、时间范围、对比选择 |
| 输出 | 统计图表数据、对比分析结果、PDF/Excel 文件 |
| 不负责 | 执行过程管理（由 M9 负责） |

#### 对外接口

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 趋势分析 | GET | `/api/v1/projects/:pid/analytics/trends` | 按粒度和时间范围 |
| 高频失败 | GET | `/api/v1/projects/:pid/analytics/top-failures` | TOP N |
| 模块热力图 | GET | `/api/v1/projects/:pid/analytics/module-heatmap` | 失败率热力图 |
| 历史对比 | GET | `/api/v1/projects/:pid/analytics/compare` | 两次执行对比 |
| 导出 PDF | GET | `/api/v1/projects/:pid/reports/:execution_id/pdf` | 单次执行报告 |
| 导出 Excel | GET | `/api/v1/projects/:pid/reports/:execution_id/excel` | 单次执行报告 |
| 汇总 PDF | GET | `/api/v1/projects/:pid/reports/summary/pdf` | 时间范围汇总 |

#### UI 页面

| 页面 | 路径 |
|---|---|
| 执行详情（报告） | `/projects/:id/executions/:execId` |
| 执行历史（含趋势分析/历史对比标签） | `/projects/:id/executions` |


---

## 4. 模块间依赖关系

### 4.1 依赖关系总图

> 下图展示模块级依赖，所有模块均在同一个 Spring Boot 应用内，通过 Spring Bean 依赖注入直接调用。

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                   M1 认证与用户管理 (auth)                                     │
│              (JWT 认证 · RBAC 权限 · 用户管理 · 全局配置)                        │
│              ▲ 所有模块均依赖 JWT 鉴权（通过全局过滤器统一校验）                   │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │ Bean 注入
       ┌───────────────────────────┼───────────────────────────┐
       ▼                           ▼                           ▼
┌─────────────┐           ┌──────────────┐           ┌─────────────────┐
│ M2 项目管理  │           │ M3 环境配置   │           │ M4 接口管理      │
│ (project)    │           │ (project)    │           │ (api)           │
│ (项目CRUD    │           │ (环境CRUD     │           │ (分组·Swagger   │
│  概览仪表板) │           │  JSON配置     │           │  接口CRUD·调试) │
└──────┬──────┘           │  连接测试)    │           └────────┬────────┘
       │                  └──────┬───────┘                    │
       │                         │                            ▼
       │                         │                  ┌──────────────────┐
       │                         │                  │ M5 接口关键字管理  │
       │                         │                  │ (keyword)         │
       │                         │                  │ (测试数据·预期响应 │
       │                         │                  │  删除保护)        │
       │                         │                  └────────┬─────────┘
       │                         │                           │
       │                         │                  ┌──────────────────┐
       │                         │                  │ M6 工具方法关键字  │
       │                         │                  │ (keyword)         │
       │                         │                  │ (代码沙箱·在线测试 │
       │                         │                  │  传参返回)        │
       │                         │                  └────────┬─────────┘
       │                         │                           │
       │                         │              ┌────────────┼────────────┐
       │                         │              ▼            ▼            ▼
       │                         │    ┌───────────────────────────────────────┐
       │                         │    │         M7 Action 关键字管理           │
       │                         │    │ (keyword)                              │
       │                         │    │ (流程画布编排·调试·引用管理·删除保护)    │
       │                         │    │ 引用 → M5(接口关键字)                  │
       │                         │    │ 引用 → M6(工具方法关键字)               │
       │                         │    │ 引用 → M7(自身, Action嵌套)             │
       │                         │    └──────────────────┬────────────────────┘
       │                         │                       │
       │                         │                       ▼
       │                         │    ┌───────────────────────────────────────┐
       │                         │    │         M8 测试用例管理                │
       │                         │    │ (execution)                            │
       │                         │    │ (套件·用例编排·校验·Setup/Teardown)    │
       │                         │    │ 步骤引用 → M5/M6/M7(Keyword)          │
       │                         │    └──────────────────┬────────────────────┘
       │                         │                       │
       │                         ▼                       ▼
       │                  ┌──────────────────────────────────────────────┐
       │                  │            M9 测试执行与调度 (execution)      │
       │                  │ (计划·触发·异步执行·WebSocket实时推送)          │
       │                  │ 直接查询: 读取 M3(环境配置)                    │
       │                  │ 直接查询: 加载 M5/M6/M7(关键字)               │
       │                  │ 直接查询: 加载 M8(用例步骤)                   │
       │                  │ 内置执行引擎: 调用 Java 引擎执行               │
       │                  └──────────────────┬───────────────────────────┘
       │                                     │
       │                                     ▼
       │                  ┌──────────────────────────────────────────────┐
       │                  │            M10 测试报告与分析 (execution)     │
       │                  │ (详情·历史·趋势·对比·PDF/Excel导出)            │
       │                  │ 数据源 → M9(执行记录·测试结果)                │
       │                  │ 聚合查询: M2(项目概览仪表板)                  │
       └──────────────────┤                                              │
                          └──────────────────────────────────────────────┘
```

### 4.2 依赖关系矩阵

下表以行为消费者、列为提供者，标注依赖方向（→ 表示"依赖于"）：

| 消费者 \ 提供者 | M1 | M2 | M3 | M4 | M5 | M6 | M7 | M8 | M9 | M10 |
|---|---|---|---|---|---|---|---|---|---|---|
| **M1** | — | | | | | | | | | |
| **M2** | → | — | | | | | | | | → |
| **M3** | → | → | — | | | | | | | |
| **M4** | → | → | → | — | | | | | | |
| **M5** | → | → | | → | — | | | | | |
| **M6** | → | → | | | | — | | | | |
| **M7** | → | → | | | → | → | → | | | |
| **M8** | → | → | | | → | → | → | — | | |
| **M9** | → | → | → | | | | → | → | — | |
| **M10** | → | → | | | | | | | → | — |

### 4.3 关键依赖链

#### 数据流链路

```
接口定义(M4) → 接口关键字(M5) → Action关键字(M7) → 测试用例(M8) → 执行(M9) → 报告(M10)
                     ↑                ↑
              工具方法关键字(M6) ───────┘
```

#### 执行时依赖链

```
M9 触发执行（发布消息到 RabbitMQ）
  → 直接查询 project 模块: 读取 M3 环境配置（host, authorization 等）
  → 本地查询: 加载 M8 测试用例（steps JSON）
  → 直接查询 keyword 模块: 获取关键字详情，解析步骤树：
      → 接口关键字步骤 → 查找 M5 → 组装 HTTP 请求 → 调用内置 HttpClient 执行
      → 工具方法步骤 → 查找 M6 → 调用内置 Groovy 沙箱执行代码
      → Action 关键字步骤 → 查找 M7 → 解析 nodes JSON → 按画布拓扑递归执行
  → 写入 TestExecution + TestResult → M10 读取分析
```

#### 删除保护依赖链

```
删除接口(M4)
  → 检查接口关键字(M5)引用 → 有引用则拒绝
    → 检查 Action(M7)引用 → 有引用则拒绝
      → 检查测试用例(M8)引用 → 有引用则拒绝
```

---

## 5. 数据模型概览

### 5.1 核心实体关系

```
Project 1──N Environment
Project 1──N ApiModule 1──N ApiEndpoint 1──N Keyword(API)
Project 1──N Keyword（统一关键字元数据，keyword_type 区分类型）
Project 1──N ToolMethod
Project 1──N Action
Project 1──N TestSuite 1──N TestCase
Project 1──N TestPlan N──N TestSuite（suite_ids JSON）
TestPlan 1──N TestExecution 1──N TestResult
TestSuite 1──N SuiteCaseLifecycle N──1 TestCase（差异化 Setup/Teardown）
User 1──N Keyword（created_by）
User 1──N TestExecution（triggered_by）
```

### 5.2 实体清单与职责

| 实体 | 所属模块 | 职责 | 关键字段 |
|---|---|---|---|
| `User` | M1 | 用户账号与角色 | username, password_hash, role, is_active |
| `Project` | M2 | 项目一级组织单元 | name, source_path, is_active |
| `Environment` | M3 | 测试环境配置 | project_id, config JSON, is_active |
| `ApiModule` | M4 | 接口分组 | project_id, name, service_prefix, source_type |
| `ApiEndpoint` | M4 | HTTP 接口定义 | module_id, path, method, parameters JSON, responses JSON |
| `Keyword` | M5/M7/M8 | 统一关键字元数据 | keyword_type(API/TOOL/ACTION/TEST_CASE), ref_id, input_params, output_params, config JSON |
| `ApiKeyword` | M5 | 接口关键字源实体 | keyword_id, endpoint_id |
| `ToolMethod` | M6 | 工具方法 | name, keyword(≤20字符), code, parameters JSON, is_builtin |
| `Action` | M7 | Action 关键字源实体 | project_id, nodes JSON |
| `TestSuite` | M8 | 测试套件 | project_id, once/per_case setup/teardown steps, enable 开关 |
| `TestCase` | M8 | 测试用例 | suite_id, steps JSON, setup_steps, teardown_steps |
| `SuiteCaseLifecycle` | M8 | 套件内用例差异化生命周期 | suite_id, case_id, setup_steps, teardown_steps |
| `TestPlan` | M9 | 测试计划 | suite_ids JSON, environment_id, schedule_cron |
| `TestExecution` | M9 | 执行记录 | plan_id, trigger_type, status, passed/failed/skipped_cases |
| `TestResult` | M9 | 测试结果明细 | execution_id, case_id, status, logs JSON, assertions |

### 5.3 Keyword 统一实体设计

Keyword 是平台的核心抽象实体，通过 `keyword_type` 字段区分四种类型，所有类型共享统一的元数据结构：

```
Keyword
├── keyword_type: API        → ref_id → ApiKeyword → ApiEndpoint（接口 + 测试数据）
├── keyword_type: TOOL       → ref_id → ToolMethod（工具方法，通过 keyword 字段标识）
├── keyword_type: ACTION     → ref_id → Action（流程画布编排）
└── keyword_type: TEST_CASE  → ref_id → TestCase（测试用例复用）
```

**可扩展性**：新增关键字类型只需添加 `keyword_type` 枚举值 + 对应源实体，无需修改 Keyword 表结构。

---

## 6. 接口契约摘要

### 6.1 API 规范

| 规范项 | 约定 |
|---|---|
| 风格 | RESTful |
| 版本前缀 | `/api/v1/` |
| 统一响应 | `{ "code": 0, "message": "success", "data": {} }` |
| 分页格式 | `{ "items": [], "total": N, "page": 1, "page_size": 20 }` |
| 认证方式 | `Authorization: Bearer <JWT Token>` |
| 错误码 | `409` 表示依赖冲突（删除保护触发） |

### 6.2 各模块 API 端点统计

| 模块 | REST 端点数 | WebSocket 端点数 |
|---|---|---|
| M1 认证与用户管理 | 10 | 0 |
| M2 项目管理 | 7 | 0 |
| M3 环境配置管理 | 7 | 0 |
| M4 接口管理 | 11 | 0 |
| M5 接口关键字管理 | 6 | 0 |
| M6 工具方法关键字管理 | 5 | 0 |
| M7 Action 关键字管理 | 6 | 0 |
| M8 测试用例管理 | 12 | 0 |
| M9 测试执行与调度 | 11 | 1 |
| M10 测试报告与分析 | 7 | 0 |
| **合计** | **82** | **1** |

### 6.3 WebSocket 接口

| 路径 | 所属模块 | 说明 |
|---|---|---|
| `/ws/projects/:pid/executions/:id` | M9 | 执行过程实时状态和日志推送 |

---

## 7. 非功能需求摘要

### 7.1 性能要求

| 指标 | 要求 | 影响模块 |
|---|---|---|
| 页面首屏加载 | < 2s | 前端全局 |
| API 响应时间 | 普通 < 200ms，列表查询 < 500ms | 所有后端模块 |
| 并发用户 | 50+ | M1(认证), M9(执行) |
| 单用例执行超时 | 默认 30s，可配置 | M9 |
| 数据存储 | 百万级执行记录，MySQL 分区表 + 自动归档 > 90 天 | M9, M10 |

### 7.2 安全要求

| 要求 | 实现方式 | 影响模块 |
|---|---|---|
| JWT 双 Token | Access Token 2h + Refresh Token 7d | M1 |
| 密码安全 | bcrypt 加密，最少 8 位 | M1 |
| RBAC | ADMIN 全权限 / USER 测试操作权限 | M1，所有模块权限守卫 |
| admin 账号保护 | 不可改名/改角色/禁用/删除 | M1 |
| 敏感数据脱敏 | 环境配置中 Token/密码 API 响应脱敏 | M3 |
| 工具方法沙箱 | 白名单库 + 禁止文件/网络/子进程 + 超时/内存限制 | M6 |

### 7.3 可用性要求

- 响应式布局，支持 1280px 以上分辨率
- 关键操作确认提示（删除、批量操作）
- 表单即时校验反馈
- 明确加载状态（骨架屏 / Loading）
- 表格列排序、列宽调整

---

## 8. 技术风险与约束

### 8.1 单体架构风险

| 风险 | 说明 | 缓解策略 |
|---|---|---|
| 模块耦合风险 | 所有模块在同一应用内，可能出现模块间耦合过紧 | 通过包级别封装和接口抽象保持模块边界清晰；模块间通过 Service 接口调用，避免直接访问 Mapper |
| 扩展性受限 | 单体应用无法按模块独立扩展 | 通过 Spring Profile 和条件配置支持多实例部署；未来如需拆分可基于当前模块包边界拆分 |
| 启动时间 | 随着模块增多，应用启动时间增加 | 合理组织 Bean 加载顺序；使用懒加载策略减少启动时间 |

### 8.2 执行引擎实现风险

| 风险 | 说明 | 缓解策略 |
|---|---|---|
| 沙箱安全性 | Groovy ScriptEngine 执行用户代码，存在安全逃逸风险 | 白名单 import + 黑名单 class + SecurityManager 限制 + 超时/内存控制 |
| 环境配置适配 | 原 postman-tool 引擎从文件读取 env.json，平台需改为从 DB 读取 | execution 模块通过 Spring Bean 直接调用 project 模块获取环境配置，构建 ExecutionContext 传给执行引擎 |
| 关键字存储适配 | 原引擎使用 Python 字典文件，平台使用 DB | 执行引擎从 DB 加载关键字数据，序列化为内部执行格式 |

### 8.3 前端技术风险

| 风险 | 说明 | 缓解策略 |
|---|---|---|
| AntV X6 流程画布复杂度 | Action 编排器基于 X6 的流程图，涉及节点类型、连线、曼哈顿路由、序列化/反序列化 | 先实现核心节点放置和连线，逐步添加高级功能（格式化、清空、右键菜单） |
| Monaco Editor 集成 | 环境配置 JSON 编辑和工具方法代码编辑均依赖 Monaco Editor | 统一封装 Monaco Editor 组件，通过 props 区分 JSON/Python 模式 |

### 8.4 业务约束

| 约束 | 说明 |
|---|---|
| 四层 Setup/Teardown | 九步嵌套执行模型增加了执行引擎的复杂度，需严格保证执行顺序和错误处理 |
| 删除保护链 | 接口 → 接口关键字 → Action → 用例 形成多层依赖链，删除操作需逐级检查 |
| 流程画布执行语义 | Action 画布是执行蓝图，引擎按拓扑排序执行，孤立节点不参与执行 |
| CI/CD 预留 | Webhook 接口预留，第一期仅开放 API，不做前端配置页 |

---

## 9. 附录

### 9.1 UI 页面总览（26 个核心页面）

| 模块 | 页面数 | 页面列表 |
|---|---|---|
| M1 认证与用户管理 | 4 | 登录、用户管理、个人资料、全局配置 |
| M2 项目管理 | 2 | 项目列表、项目概览 |
| M3 环境配置管理 | 1 | 环境配置 |
| M4 接口管理 | 4 | 接口列表、Swagger 导入、接口编辑、接口调试 |
| M5 接口关键字管理 | 3 | 接口关键字列表、创建接口关键字、编辑接口关键字 |
| M6 工具方法关键字管理 | 3 | 工具方法关键字列表、创建工具方法关键字、编辑工具方法关键字 |
| M7 Action 关键字管理 | 3 | Action 关键字列表、Action 编辑器、Action 调试 |
| M8 测试用例管理 | 3 | 测试套件、用例列表、用例编辑 |
| M9 测试执行与调度 | 4 | 测试计划列表、计划编辑、执行记录、执行详情 |
| M10 测试报告与分析 | 0 | （复用 M9 的执行详情和执行历史页面） |

### 9.2 关联文档索引

| 文档 | 路径 | 说明 |
|---|---|---|
| 需求规格说明书 | [SRS.md](SRS.md) | 技术架构、数据模型、API 设计、引擎复用方案、非功能需求 |
| 产品需求文档 | [PRD.md](PRD.md) | 功能需求规范（PM/ENV/API/KW/TOOL/ACT/CASE/EXEC/RPT/AUTH/SET）、UI 设计规范 |
| UI 原型 | [docs/ui/](../ui/index.html) | 26 个核心页面的 HTML 高保真原型 |

### 9.3 开发里程碑参照

| 阶段 | 工期 | 涉及模块/服务 |
|---|---|---|
| Phase 0：项目基础设施 | 1 周 | Spring Boot 脚手架、JWT/RabbitMQ 配置、公共模块 |
| Phase 1：基础框架 & 核心功能 | 4 周 | auth(M1), project(M2/M3), api(M4), keyword(M5/M6/M7) |
| Phase 2：用例编排 & 执行引擎 | 3 周 | execution(M8/M9 + 内置执行引擎) |
| Phase 3：调度 & 报告分析 | 3 周 | execution(M9/M10), XXL-Job 集成 |
| Phase 4：优化 & 部署 | 2 周 | 后端性能优化、集成测试、Docker/K8s 部署配置 |
| **合计** | **13 周** | |
