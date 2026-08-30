# auto-test-platform 需求规格说明书

> 版本：v1.3 | 通用项目管理平台（通用化修订）  
> 文档状态：修订稿

---

## 1. 项目概述

### 1.1 项目背景

auto-test-platform 是一个**通用的项目管理平台**，面向任意行业的 HTTP API 自动化测试场景，不绑定特定业务领域。平台提供 Web 化的全流程管理能力，覆盖接口管理、关键字编排、用例组织、执行调度和报告分析。

平台的核心引擎源自 **postman-tool**——一个面向乘用车换电站站控系统的自动化测试工具（基于 Python 3.6.8 开发，采用关键字驱动测试方法论，通过四层业务模型组织测试代码）。引擎经过通用化抽象改造后，已脱离原始业务绑定，可服务于任意项目的关键字驱动测试。

postman-tool 引擎具备以下核心能力：

1. **Swagger → API 客户端自动生成**：解析 Swagger 2.0 JSON 文档，自动生成 Python HTTP 客户端代码
2. **关键字字典 AST 自动生成**：通过 AST 解析 Python 源码中 `@KwDecorator` 装饰的方法，自动生成中文描述到函数的映射字典
3. **HTTP/HTTPS 通信**：封装被测系统 API 请求，支持多种鉴权方式（Bearer Token、动态密码等）
4. **WebSocket 实时通信**：支持 WebSocket 消息收发，可适配不同认证和路由模式
5. **配置中心管理**：支持外部配置中心的读写操作
6. **远程命令执行**：通过测试服务接口执行预定义的 Shell 命令和 SQL 查询

当前 postman-tool 以纯代码/CLI 方式运行，存在以下痛点：

- **操作门槛高**：测试人员需要理解 Python 代码结构、目录约定和命令行操作
- **无可视化管理**：关键字、测试用例、环境配置均通过源码文件和配置文件管理，不直观
- **缺少执行调度**：不支持定时任务和执行历史追踪，仅通过控制台和日志文件查看结果
- **无报告分析**：测试结果仅在日志文件中输出，缺乏结构化报告和趋势分析
- **无全流程管理**：从 Swagger 导入、关键字生成、用例编排到执行分析各环节割裂，依赖手动操作
- **多环境切换繁琐**：通过修改配置文件切换环境，易出错

### 1.2 项目目标

基于 postman-tool 的核心引擎，开发 **auto-test-platform**——一个通用的项目管理平台，提供 Web 化全流程管理能力：

1. **关键字可视化操作**：在线浏览、搜索、管理关键字字典
2. **测试用例编排**：拖拽式用例编排，支持参数化配置
3. **环境配置管理**：多环境统一配置中心
4. **执行调度**：手动触发 + 定时任务
5. **报告分析**：结构化报告、趋势图表、导出（PDF/Excel）、历史对比
6. **简单登录**：管理员创建账号，简单登录认证（用户注册功能后续迭代开发）

### 1.3 四层封装体系

平台采用四层封装体系，自底向上逐层组装，每一层封装后形成新的可复用单元：

```
┌──────────────────────────────────────────────────────────────────────┐
│  第四层：测试用例                                                      │
│  用例步骤调用接口关键字/工具方法关键字/Action 关键字 + 逻辑控制               │
├──────────────────────────────────────────────────────────────────────┤
│  第三层：Action 关键字                                                 │
│  组合接口关键字 + 工具方法关键字 + 其他 Action 关键字 + 逻辑控制             │
│  通过流程画布编排节点连线，执行引擎按画布拓扑顺序执行                       │
│  封装为可复用的业务动作单元，可被其他 Action 关键字和测试用例调用              │
├──────────────────────────────────────────────────────────────────────┤
│  第二层：关键字封装层                                                   │
│  接口关键字：接口 + 预设测试数据（同一接口不同参数 → 不同关键字）              │
│  工具方法关键字：工具方法 + keyword 字段（不超过20字符）                    │
├──────────────────────────────────────────────────────────────────────┤
│  第一层：基础能力层                                                     │
│  接口：被测系统的 HTTP REST 接口定义，由 Swagger 导入或手动创建               │
│  工具方法：Python 辅助函数，提供数据转换、加密等通用能力                      │
└──────────────────────────────────────────────────────────────────────┘
```

每一层封装关系：
- 接口 + 测试数据 = **接口关键字**（同一接口可因不同参数配置生成多个接口关键字）
- 工具方法 + keyword 字段 = **工具方法关键字**（每个工具方法最多一个关键字）
- 接口关键字 + 工具方法关键字 + 其他 Action 关键字 + 逻辑控制 = **Action 关键字**（支持 Action 嵌套调用）
- 接口关键字 + 工具方法关键字 + Action 关键字 + 逻辑控制 = **测试用例**（用例可直接调用三种关键字）

### 1.4 设计原则

| 原则 | 说明 |
|---|---|
| 引擎复用 | 后端直接复用 postman-tool 的核心引擎（关键字解析、AST 转换、HTTP/WSS 通信） |
| 外部路径引用 | 平台仅管理接口/关键字/用例的业务数据，源码文件和生成代码由用户在本地维护，执行时指定项目源码路径 |
| 通用化抽象 | 平台不绑定特定业务领域，支持任意项目的关键字驱动测试 |
| 专业美观 | Web 界面设计追求专业、简洁、高效，符合工程工具的使用习惯 |
| 渐进增强 | 核心功能优先交付，扩展功能迭代完善 |

### 1.5 postman-tool 引擎参考实现

> 以下基于 postman-tool 源码（refact-1.1 分支）的深入分析，展示引擎的具体实现方式，为平台引擎复用提供参考。auto-test-platform 作为通用测试平台，不绑定以下特定业务细节；这些内容仅作为引擎能力的一个实现示例。

#### 1.5.1 项目目录结构

```
postman-tool/
├── convertAllFile.py              # 入口脚本（按序执行 6 个转换任务）
├── env.json                        # 多环境配置（useEnv 字段切换环境）
├── requirements.txt                # 依赖：loguru / pytest / pytz / requests / websockets
├── common/                         # 公共基础层（引擎复用的核心）
│   ├── handlerKw/                  #   关键字处理引擎
│   │   ├── kwDecorator.py          #     @KwDecorator 装饰器（中文描述标记）
│   │   ├── handlerKw.py            #     HandlerKw.kw() 关键字封装（lambda 包装）
│   │   ├── runKw.py                #     RunKw 执行器（ts_run/act_run/api_run/tools_run）
│   │   ├── convertKw.py            #     ConvertKeyKw AST 解析器 → 生成关键字字典文件
│   │   └── convertAllKw.py         #     批量转换入口（WSS/ACT/API/NCS/TC 五类）
│   ├── handlerSwagger/             #   Swagger 代码生成
│   │   ├── convertSwagger.py       #     ConvertApiDoc 转换器（解析 $ref、展开 DTO）
│   │   └── convertAllSwagger.py    #     Swagger 批量转换入口
│   ├── http/                       #   HTTP 请求封装
│   │   ├── httpMethod.py           #     HttpMethod 请求参数验证 + 构建(path,method,data)三元组
│   │   └── httpRq.py               #     HttpRq 请求执行器（自动 Authorization + 响应日志）
│   └── utils/                      #   工具函数库
│       ├── handleEnv.py            #     HandleEnv 环境配置读取（从 env.json）
│       ├── handlerAssert.py        #     HandlerAssert 断言引擎（equal/include/true 等 6 种断言）
│       ├── handlerFile.py          #     HandlerFile 文件操作（目录创建 / JSON 读取）
│       ├── handlerNacos.py         #     HandlerNacos Nacos HTTP 客户端（GET/POST/DELETE）
│       ├── handlerWss.py           #     HandlerWss WebSocket 客户端（新站 Wss_New / 旧站 Wss_old）
│       ├── logConfig.py            #     LogConfig loguru 双路日志（控制台 + 文件按日轮转）
│       ├── getProject.py           #     GetProject 项目路径查找（向上查找 'postman-tool' 目录）
│       └── tools/                  #     基础工具
│           ├── handlerDataType.py  #       数据类型转换（float/uint16/uint32 → short，Modbus 用）
│           ├── handlerEncrypt.py   #       MD5 加密
│           └── handlerTime.py      #       时间戳/时间字符串/日期字符串
├── company/                        # 业务层（按公司/版本组织）
│   ├── qkl/                        #   时代电服（主要业务）
│   │   ├── new/                    #     新站 v3（微服务架构）
│   │   │   ├── common/             #       共享层（api/ / ncs/ / wss/ / sh/ / utils/）
│   │   │   ├── kw/                 #       WSS 关键字字典
│   │   │   ├── source/swaggerApi/  #       Swagger JSON 源文件（15 个）
│   │   │   └── version/v3/         #       业务版本层（act/ dao/ ts/ kw/）
│   │   └── old/                    #     旧站 v1（单体架构）
│   │       ├── common/             #       共享层（api/ / wss/ / utils/）
│   │       ├── kw/                 #       WSS 关键字字典
│   │       ├── source/swaggerApi/  #       Swagger JSON 源文件（8 个）
│   │       └── version/v1/         #       业务版本层（act/ dao/ ts/ kw/）
│   └── qj/                         #   另一家公司占位（待扩展）
└── logs/                           # 日志输出目录（运行时自动创建）
```

#### 1.5.2 四层业务模型（version 层内）

postman-tool 在业务版本层（`version/v3/` 或 `version/v1/`）内采用四层组织：

```
┌───────────────────────────────────────────────────────────────────┐
│  ts/（测试脚本层）                                                      │
│  test_*.py → 导入 kw 字典 → RunKw.act_run(KeyDict, '关键字') 执行   │
│  每个 test_*.py 与 act/ 模块一一对应，方法使用 @KwDecorator 标注         │
├───────────────────────────────────────────────────────────────────┤
│  act/（业务动作层）                                                     │
│  按模块组织（battery_manage/swap_manage 等）                       │
│  @KwDecorator 标注方法 → RunKw.api_run(KeyDict, '关键字') 调用API   │
│  多个 ACT 模块采用“三态模拟”模式（执行中/完毕/错误）模拟设备操作       │
├───────────────────────────────────────────────────────────────────┤
│  dao/（数据访问层）                                                     │
│  api/set_*.py → 封装业务请求数据，调用自动生成的 API 客户端            │
│  ncs/ncs_*.py → 封装 Nacos 配置操作模板（get + post_*）              │
├───────────────────────────────────────────────────────────────────┤
│  kw/（关键字层，自动生成，禁止手动修改）                                   │
│  kw_act/key_*.py   ← 从 act/ 源文件 AST 解析生成                  │
│  kw_api/key_*.py   ← 从 dao/api/ 源文件 AST 解析生成              │
│  kw_ncs/key_*.py   ← 从 dao/ncs/ 源文件 AST 解析生成              │
│  kw_tc/key_*.py    ← 从 ts/ 源文件 AST 解析生成                   │
│  格式：KeyXxx = {"中文描述": HandlerKw.kw(Class.method), ...}       │
└───────────────────────────────────────────────────────────────────┘
```

> **命名体系对照**：概念层（§1.3）与引擎实现层（§1.5.2）采用不同命名体系，其对应关系如下：
>
> | 概念层 | 引擎实现层 | 说明 |
> |---|---|---|
> | 第一层：基础能力层（接口 + 工具方法） | `dao/`（数据访问层） | 封装 HTTP 请求与配置操作 |
> | 第二层：关键字封装层 | `kw/`（关键字层） | 自动生成，禁止手动修改 |
> | 第三层：Action 关键字 | `act/`（业务动作层） | 按业务模块组织 |
> | 第四层：测试用例 | `ts/`（测试脚本层） | 测试入口，调用接口关键字/工具方法关键字/Action 关键字 |

#### 1.5.3 新旧站版本差异

| 特性 | 新站 v3 (`company/qkl/new`) | 旧站 v1 (`company/qkl/old`) |
|---|---|---|
| 架构 | 微服务，每个服务独立 Swagger 文档 | 单体，API 集中在 api_manager |
| API 客户端 | 15 个（battery/camera/car-station/dcci/emu/energy/fault/manager/plc/swap-flow/video/virtual-battery/virtual-car-station/virtual-dcci/virtual-plc） | 8 个（cloud/dcc-pdu/manager/rbc/virtual-car-machine/virtual-dcci/virtual-modbus/virtual-plc） |
| Nacos 配置 | 7 个配置文件（swap-common/swap-common-station/swap-temp-common/virtual-battery/virtual-car-station/virtual-dcci/virtual-plc） | 无（dao/ncs/ 仅有 `__init__.py`） |
| WSS 接口 | 统一 `/ws` 入口，4 种消息类型（currentProcessData/volume/fireMoveWarn/underBox） | 3 个独立路径（/manager/webSocketOfManager、/manager/newWebSocket、/rbc/webSocket） |
| Shell/SQL | 通过 `/autotest` 服务执行（ExecSh.api_test_shell/api_test_sql） | 无（common/sh/ 仅有 `__init__.py`） |
| 动态密码 | QKLPwd: `MD5('5ncp31:' + 日期)[8:24]`，通过 QKLHttp.Req() 自动注入 | 无 |
| ACT 模块数 | 9 个（battery_manage/config_manage/device_manage/energy_manage/event_manage/fault_diagnosis/home/software_ota/swap_manage） | 15 个（多数为骨架，仅 swap_channel 已完整实现） |
| TS 模块数 | 9 个（与 ACT 一一对应） | 17 个（比 ACT 多出 charge_manage 和 home） |
| 鉴权方式 | Bearer Token + MD5 动态密码（双重认证） | 仅 Bearer Token |

#### 1.5.4 代码生成管道

`convertAllFile.py` 是项目的入口脚本，按顺序执行 6 个转换任务：

```
1. Swagger 转换：
   source/swaggerApi/*.json → ConvertApiDoc → common/api/api_*.py
   （解析 paths/methods/parameters/definitions，支持 $ref 引用和嵌套 DTO 展开）

2~6. 关键字字典转换（源文件 → 关键字字典）：
   2. WSS: common/wss/*.py → kw/key_*.py
   3. ACT: version/v*/act/**/*.py → version/v*/kw/kw_act/**/key_*.py
   4. API: version/v*/dao/api/set_*.py → version/v*/kw/kw_api/key_*.py
   5. NCS: version/v*/dao/ncs/ncs_*.py → version/v*/kw/kw_ncs/key_*.py
   6. TC:  version/v*/ts/test_*.py → version/v*/kw/kw_tc/key_*.py
```

转换规则：
- 跳过 `__init__.py`，递归处理目录下所有 `.py` 文件
- 通过 AST 解析 `@KwDecorator` 装饰器的方法，提取类名、方法名、中文描述和参数列表
- 生成 `KeyXxx = {"中文描述": HandlerKw.kw(Class.method), ...}` 格式的字典文件
- 关键字按描述文本排序后写入文件

#### 1.5.5 鉴权机制

| 场景 | 机制 | 实现 |
|---|---|---|
| 站控 API | Bearer Token（JWT） | `env.json` → `host_authorization` 字段 |
| 测试服务（`/autotest`） | MD5 动态密码 | `QKLPwd`: `MD5('5ncp31:' + 日期)[8:24]`，通过 `QKLHttp.Req()` 自动注入 `other_authorization` |
| Nacos | accessToken | `env.json` → `nacos_accessToken` 字段 |
| WebSocket（新站） | userId 认证 | 连接后先发送 `{"userId": "567"}` 再发送业务数据 |

#### 1.5.6 环境配置结构

`env.json` 支持多环境切换，每个环境包含以下配置字段：

```json
{
  "useEnv": "test",
  "test": {
    "company": "qkl",
    "host": "https://pdd-station-supercharge-test.catles.com",
    "host_authorization": "Bearer eyJhbGci...",
    "nacos": "https://nacos-pdd-test.catles.com:4484",
    "nacos_accessToken": "eyJhbGci...",
    "wss": "wss://pdd-station-supercharge-test.catles.com"
  }
}
```

当前已配置环境：test、test01、test03（完整配置）；him、him01、him03、other（配置待填充）。

#### 1.5.7 核心执行链路

```
HTTP 请求链路：
  业务方法(act/*.py) → RunKw.api_run(KeySetXxx, '关键字', *args)
    → HandlerKw.kw(SetXxx.api_xxx)(*args) → ApiXxx.api_xxx(data)  [自动生成]
      → HttpRq.req(*HttpMethod.http_info(path, method, data))
        → HandleEnv.get_env_host() + get_env_authorization()
          → requests.request() with Bearer Token

WebSocket 链路（新站）：
  NewWss.current_process_data_wss() → QKLWss.ws(data) → (path='/ws', data)
    → HandlerWss.Wss_New(userId='567', path, data)
      → websockets.sync.client.connect(wss://host/ws) → send(userId) + send(data) + recv()

Nacos 配置链路：
  NcsSwapCommonStation.get() → QKLNcsMethod.get(data) → (path, 'get', data)
    → HandlerNacos.NacosReq(path, method, data) → requests.request() with accessToken

Shell/SQL 链路：
  QKLSh.current_time() → ExecSh.api_test_shell(cmd_id=1)
    → HttpRq.req(path='/autotest/api/test/shell', method='POST', data={id:1})
```

#### 1.5.8 代码规范

- **类设计**：所有业务类使用 `@staticmethod`，纯静态工具类模式，不使用 `__init__`
- **装饰器顺序**：`@staticmethod` 必须在 `@KwDecorator` **之上**（外层），顺序不可颠倒
- **命名规范**：
  - API 客户端：`api_*.py` → `Api*`（PascalCase），服务名：去掉 `api_` 前缀，下划线转连字符
  - DAO/API：`set_*.py` → `Set*`
  - NCS 配置：`ncs_*.py` → `Ncs*`
  - WSS：`*_wss.py` → `*Wss`
  - 关键字字典：`key_*.py` → `Key*`
  - ACT 动作：按模块组织，测试脚本：`test_*.py` → `Test*`
- **自动生成文件**：`common/api/api_*.py` 和 `kw/key_*.py` 由代码生成器自动产生，禁止手动修改
- **日志规范**：loguru TRACE 级别，控制台彩色输出 + 文件按日轮转（`logs/{YYYY-MM-DD}.log`）

---

## 2. 技术架构

### 2.1 技术栈

#### 前端

| 技术 | 版本 | 选型理由 |
|---|---|---|
| Vue 3 | 3.5+ | Composition API，性能优异，生态成熟 |
| TypeScript | 6.x | 类型安全，提升大型项目可维护性 |
| Vite | 8.x | 极速开发体验，HMR 即时生效 |
| Element Plus | 2.x | 企业级组件库，专业、克制、无 AI 感 |
| Pinia | 4.x | Vue 3 官方状态管理，轻量直观 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 客户端，拦截器统一处理 |
| ECharts | 5.x | 数据可视化图表 |
| Monaco Editor | - | 代码编辑器（关键字脚本编辑、JSON/YAML 编辑） |
| VueDraggable | - | 拖拽排序（测试步骤编排） |
| dayjs | 1.x | 日期处理 |

#### 后端

| 技术 | 版本 | 选型理由 |
|---|---|---|
| Java | 1.8 | 主流企业级语言，LTS 版本保证长期支持，生态成熟稳定 |
| Spring Boot | 2.7 | 企业级框架标杆，自动配置、内嵌容器、生态成熟、社区活跃 |
| Spring Security | 5.7 | 认证鉴权框架，与 Spring Boot 深度集成，支持 OAuth2/JWT |
| JJWT | 0.11 | JWT 认证，成熟的 Java JWT 库 |
| MyBatis-Plus | 3.5+ | 增强型 ORM，简化 CRUD，支持代码生成、分页、多租户 |
| Flyway | 8.x | 数据库版本迁移工具，SQL 脚本管理、自动执行、回滚支持 |
| RabbitMQ | 3.x | 消息队列，异步事件驱动（测试执行触发、状态通知） |
| MySQL | 8.0+ | 广泛使用，运维成熟，原生 JSON 类型支持 |
| Redis | 7.x | 缓存、会话管理、分布式锁 |
| XXL-Job | 2.4+ | 分布式任务调度（可视化管理、分片广播） |
| Spring Async | - | 服务内异步任务执行（@Async + ThreadPoolTaskExecutor） |
| javax.validation | - | 数据校验（Spring Boot 内置，注解驱动） |
| OkHttp | 4.12 | HTTP 客户端，支持 HTTP/2、异步请求 |
| JGit | 5.13 | 纯 Java 实现的 Git 客户端库（测试代码库克隆/拉取，无需服务器安装 Git） |
| Groovy ScriptEngine | JSR-223 | 工具方法沙箱执行 |
| swagger-parser | - | Swagger/OpenAPI 文档解析 |
| JavaParser | - | 工具方法源码 AST 解析 |
| EasyExcel | 3.x | Excel 报告导出，阿里开源，低内存消耗 |
| iText / OpenPDF | - | PDF 报告生成 |
| SpringDoc | 1.x | 自动 OpenAPI/Swagger 文档生成 |

### 2.2 系统架构

系统采用前后端分离架构，整体分为前端服务和后端服务两大部分：

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        前端服务（Vue 3 SPA）                              │
│                                                                          │
│  接口关键字 │ Action 关键字 │ 用例编排 │ 环境配置 │ 执行调度 │ 报告分析 │ 设置 │
└─────────────────────────────────┬────────────────────────────────────────┘
                                  │ HTTP REST / WebSocket
┌─────────────────────────────────▼────────────────────────────────────────┐
│                         后端服务（Spring Boot 单体应用）                   │
│                                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ 认证模块  │ │ 项目管理  │ │ 接口文档  │ │关键字管理 │ │ 测试/执行/报告│   │
│  │ M1认证   │ │ M2项目   │ │ M4接口   │ │ M5接口KW │ │ M8用例      │   │
│  │ 用户管理  │ │ M3环境   │ │          │ │ M6工具KW │ │ M9执行      │   │
│  │ 系统配置  │ │          │ │          │ │ M7Action │ │ M10报告     │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│                                                                          │
│  ┌─── 基础设施 ───────────────────────────────────────────────────┐    │
│  │ RabbitMQ 异步事件 │ XXL-Job 分布式调度 │ WebSocket 实时推送        │    │
│  │ EasyExcel/PDF 报告生成 │ Redis 缓存/分布式锁                       │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─── 执行引擎（内置） ────────────────────────────────────────────┐   │
│  │ HTTP客户端(OkHttp) │ 断言引擎 │ Groovy沙箱 │ Swagger解析器      │   │
│  │ Action流程执行器 │ AST解析器 │ 协议适配器(WSS/Nacos/Shell)         │   │
│  └────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬────────────────────────────────────────┘
       │                    │                    │
  ┌────▼────┐         ┌────▼────┐         ┌─────▼─────┐
  │  MySQL  │         │  Redis  │         │ 文件存储   │
  │ 业务数据  │         │缓存/队列 │         │ 报告/日志  │
  └─────────┘         └─────────┘         └───────────┘
```

### 2.3 部署架构

支持两种部署方式：

**方式一：Docker Compose（推荐）**

```yaml
services:
  nginx:          # 前端静态资源 + API 反向代理
  backend:        # 后端服务（Spring Boot 单体应用，内置执行引擎）
  rabbitmq:       # 消息队列（异步事件驱动）
  mysql:          # MySQL 8.0+
  redis:          # Redis 7.x
```

**方式二：Kubernetes**

```
Nginx Ingress Controller
  ├── /static/*           → frontend (Deployment)
  ├── /api/*              → backend (Deployment + HPA)
  └── /ws/*               → backend (Deployment)
      ↓
RabbitMQ + MySQL + Redis
```

---

## 3. 数据模型

### 3.1 核心实体

```
Project (项目)
├── id: UUID
├── name: String                # 项目名称
├── description: Text           # 项目描述
├── source_path: String         # 项目源码路径（服务端服务器上的绝对路径，引擎运行时读取）
├── created_at: DateTime
├── updated_at: DateTime
└── is_active: Boolean

Environment (环境)
├── id: UUID
├── project_id: FK → Project
├── name: String                # 环境名称 (test/staging/prod)
├── description: Text
├── config: JSON                # 环境配置 JSON (host, host_authorization, wss, nacos, nacos_accessToken, company, 扩展配置等)
├── is_active: Boolean
└── created_at: DateTime

CodeRepository (测试代码仓库)
├── id: UUID
├── project_id: FK → Project
├── name: String                # 仓库名称（项目内唯一）
├── git_url: String             # Git 仓库地址（http(s):// 或 git@）
├── branch: String              # 拉取分支（空=仓库默认分支）
├── description: Text           # 仓库描述
├── auth_username: String       # 认证用户名（私有仓库，兼容 Token 用户名）
├── auth_password: String       # 认证密码/Token（AES 加密存储，enc: 前缀）
├── local_path: String          # 本地代码目录（相对存储根目录，首次拉取成功后回填）
├── last_pull_at: DateTime      # 最近一次拉取时间
├── last_pull_status: String    # 最近一次拉取状态：RUNNING / SUCCESS / FAILED
├── last_commit_id: String      # 最近一次拉取成功后的 HEAD commit ID
├── created_at: DateTime
└── updated_at: DateTime

CodeRepositoryPullLog (仓库拉取历史)
├── id: UUID
├── repository_id: FK → CodeRepository
├── pull_type: String           # 拉取类型：CLONE（首次克隆）/ PULL（增量更新）
├── branch: String              # 拉取分支
├── status: String              # 拉取状态：RUNNING / SUCCESS / FAILED
├── commit_id: String           # 拉取成功后的 HEAD commit ID
├── message: Text               # 结果信息（成功为概要，失败为原因）
├── duration_ms: Integer        # 拉取耗时（毫秒）
├── created_at: DateTime
└── updated_at: DateTime

ApiModule (接口分组)
├── id: UUID
├── project_id: FK → Project
├── name: String                # 分组名称 (如 "用户管理服务", "订单管理服务")
├── service_prefix: String      # 服务前缀 (如 "/users", "/orders")
├── description: Text
├── source_type: Enum           # 来源: SWAGGER_IMPORT / MANUAL
├── swagger_file: String        # 导入的 Swagger 文件路径（可为空）
├── created_at: DateTime
└── updated_at: DateTime

ApiEndpoint (接口)
├── id: UUID                    # 接口唯一标识（主键，全局唯一）
├── module_id: FK → ApiModule
├── name: String                # 接口名称
├── path: String                # 请求路径 (如 "/api/users/list")
├── method: Enum                # 请求方法: GET / POST / PUT / PATCH / DELETE
├── description: Text           # 接口描述
├── parameters: JSON            # 请求参数定义 [{name, in, type, required, description}]
├── request_body: JSON          # 请求体 Schema（POST/PUT）
├── responses: JSON             # 响应定义 {statusCode: schema}
├── content_type: String        # 默认 application/json
├── source_type: Enum           # 来源: SWAGGER_IMPORT / MANUAL
├── is_active: Boolean
├── created_at: DateTime
└── updated_at: DateTime

Keyword (统一关键字)
├── id: UUID
├── project_id: FK → Project
├── keyword_type: Enum          # 来源类型: API / TOOL / ACTION / TEST_CASE（可扩展新类型）
├── ref_id: UUID                # 指向源实体 ID（ApiEndpoint / ToolMethod / Action / TestCase）
├── name: String                # 关键字名称 (如 "获取用户列表_正常查询", "生成登录签名")
├── description: Text           # 关键字描述
├── input_params: JSON          # 输入参数定义 [{name, type, required, default, description}]
├── output_params: JSON         # 输出参数定义 [{name, type/path, description}]
├── config: JSON                # 关键字级别配置（按 keyword_type 存储类型特定配置）
                                  #   API: {test_data, expected_response}
                                  #   TOOL: {preset_params}
                                  #   ACTION: {} (执行细节由 Action 节点树管理)
                                  #   TEST_CASE: {} (执行细节由 TestCase 步骤管理)
├── category: String            # 分类
├── tags: JSON                  # 标签
├── is_active: Boolean
├── created_by: FK → User
├── created_at: DateTime
└── updated_at: DateTime

可扩展性设计：
  - 新增关键字类型只需添加 keyword_type 枚举值 + 对应源实体，无需修改 Keyword 表结构
  - 所有关键字统一支持 input_params（传参）和 output_params（返回）
  - config 字段以 JSON 存储类型特定配置，不同 keyword_type 有不同 Schema

ApiKeyword (接口关键字 — 源实体)
├── id: UUID
├── keyword_id: FK → Keyword    # 关联的关键字元数据 ID
├── endpoint_id: FK → ApiEndpoint  # 绑定的接口
├── created_at: DateTime
└── updated_at: DateTime
  其余字段（name, input_params, output_params, config 等）由 Keyword 统一管理

ToolMethod (工具方法)
├── id: UUID
├── project_id: FK → Project
├── name: String                # 方法名称 (如 "MD5加密", "时间戳转换")
├── keyword: String             # 关键字标识（不超过20个字符，用于 Action 节点引用，项目内唯一）
├── category: String            # 分类 (如 "加密", "时间", "数据转换", "字符串", "自定义")
├── description: Text           # 方法描述
├── code: Text                  # 实现代码（完整函数定义，引擎执行）
├── parameters: JSON            # 参数定义 [{name, type, required, default, description}]
├── return_type: String         # 返回类型
├── return_description: String  # 返回值说明
├── is_builtin: Boolean         # 是否内置方法（内置不可删除，可修改）
├── is_active: Boolean
├── created_by: FK → User
├── created_at: DateTime
└── updated_at: DateTime

  工具方法通过 keyword 字段直接标识其在 Action 节点中的引用关键字，无需独立的 ToolKeyword 实体。

Action (Action 关键字 — 源实体)
├── id: UUID
├── project_id: FK → Project    # 关联项目（满足 Project 1──N Action 关系）
├── nodes: JSON                 # 内部节点树（见下方节点结构定义）
├── created_by: FK → User
├── created_at: DateTime
└── updated_at: DateTime
  其余字段（name, description, input_params, output_params, category, tags 等）由 Keyword 统一管理

--- Action 节点结构 (nodes JSON) ---

节点是 Action 内部的执行单元，通过流程图连线表达执行流程：

节点分为两大类：

1. 关键字节点（方形）：根据 keyword_type 区分引用类型
  - action : 引用其他 Action 关键字，支持嵌套调用
  - api    : 引用接口关键字（API）
  - tool   : 引用工具方法关键字（Tool）
  - 所有关键字节点统一支持 args（传参）和 save_as（返回变量）

2. 逻辑判断节点（菱形）：
  - logic  : 条件表达式（支持脚本语法，由引擎解析执行）
  - 两个分支出口：「是」（满足条件）和「否」（不满足条件）
  - 分别连接不同的后续节点，表达 if/else 逻辑

3. 监听器节点（方形，占位）：
  - listener : 待定义功能

串行与并行表达：
  - 串行：节点间单线连接，依次执行
  - 并行：一个节点多条出线分别连向不同节点（扇出），同时执行；汇合到同一节点时并行结束

节点结构示例：
[
  {
    "type": "api", "keyword_id": "<kw_api>",  # 接口关键字
    "args": {...}, "save_as": "token"
  },
  {
    "type": "logic",                           # 逻辑判断（Python 语法）
    "expression": "status == 'ready'",
    "yes": [
      { "type": "tool", "keyword_id": "<kw_tool>", "args": {...}, "save_as": "tool_result" }
    ],
    "no": [
      { "type": "action", "keyword_id": "<kw_action>", "args": {...}, "save_as": "action_result" }
    ]
  },
  {
    "type": "api", "keyword_id": "<kw_api2>",  # 汇合节点（并行结束后串行执行）
    "args": {...}
  }
]

流程图 UI 与 nodes JSON 映射关系：
  - 节点编排器基于 AntV X6 流程图画布，用户在画布上自由定位节点、通过连接桩创建连线
  - 方形节点（action/api/tool）：上入/下出连接桩
  - 菱形节点（logic）：上入/右侧「是」/下方「否」连接桩
  - 单线连接 = 串行，扇出多线 = 并行，汇合 = 并行结束
  - 保存时前端将流程图拓扑序列化为节点数组 JSON，存储到 Action 的 nodes 字段
  - 加载时将 nodes JSON 反序列化为流程图节点和连线，还原画布状态

TestSuite (测试套件)
├── id: UUID
├── project_id: FK → Project
├── name: String
├── description: Text
├── tags: JSON                  # 标签 ["smoke", "regression"]
├── priority: Enum              # P0/P1/P2/P3
├── once_setup_steps: JSON       # 套件级·整体 Setup 步骤树（可选，整个套件执行前调用一次）
├── once_teardown_steps: JSON    # 套件级·整体 Teardown 步骤树（可选，整个套件执行后调用一次）
├── enable_once_setup_teardown: Boolean  # 是否启用套件级·整体 Setup/Teardown（默认 false）
├── per_case_setup_steps: JSON   # 套件级·每条 Setup 步骤树（可选，每条用例执行前调用）
├── per_case_teardown_steps: JSON # 套件级·每条 Teardown 步骤树（可选，每条用例执行后调用）
├── enable_per_case_setup_teardown: Boolean # 是否启用套件级·每条 Setup/Teardown（默认 false）
├── created_by: FK → User
├── created_at: DateTime
└── updated_at: DateTime

TestCase (测试用例 — 源实体)
├── id: UUID
├── suite_id: FK → TestSuite
├── preconditions: Text         # 前置条件
├── setup_steps: JSON           # 用例级 Setup 步骤树（可选，结构同 steps，该用例执行前调用）
├── teardown_steps: JSON        # 用例级 Teardown 步骤树（可选，结构同 steps，该用例执行后调用）
├── steps: JSON                 # 步骤树（与 Action 节点结构相同）
                                  # 测试用例由「测试步骤」和可选的「校验」组成：
                                  #   - 测试步骤 + 校验：步骤执行后通过断言验证结果
                                  #   - 仅测试步骤：步骤执行后不做断言，仅记录执行结果
                                  # 步骤分为两大类：
                                  #   1. 关键字步骤：统一引用 Keyword 实体
                                  #      ref_id: 关键字 ID
                                  #      args: 传入输入参数
                                  #      save_as: 保存输出为变量
                                  #      assertions: 校验列表（可选，见下方校验结构定义）
                                  #   2. 控制流步骤：parallel / serial / condition / wait
├── priority: Enum
├── timeout: Integer            # 超时秒数
├── is_active: Boolean
├── created_by: FK → User
├── created_at: DateTime
└── updated_at: DateTime
  其余字段（name, description, input_params, output_params, tags 等）由 Keyword 统一管理

--- 校验结构定义 (assertions) ---

校验（assertions）是关键字步骤的可选属性，用于在步骤执行完成后对结果进行断言验证。
每个关键字步骤可配置 0 到多个校验，校验全部通过则该步骤状态为 PASSED，任一校验失败则步骤状态为 FAILED。
未配置校验的步骤仅记录执行结果（请求/响应日志），不做断言判断。

校验结构：
{
  "type": "equal",              # 断言类型: equal / not_equal / include / not_include / true / not_true
  "actual": "${response.code}", # 实际值（支持变量引用）
  "expected": 200,              # 预期值（equal/not_equal/include/not_include 时必填）
  "message": "状态码应为 200"    # 校验描述（可选，用于报告展示）
}

带校验的关键字步骤示例：
{
  "type": "keyword",
  "keyword_id": "<kw_api>",
  "args": {"page": 1, "size": 10},
  "save_as": "api_response",
  "assertions": [
    {"type": "equal", "actual": "${api_response.status_code}", "expected": 200, "message": "状态码应为 200"},
    {"type": "include", "actual": "${api_response.body}", "expected": "success", "message": "响应体应包含 success"}
  ]
}

无校验的关键字步骤示例（仅执行，不断言）：
{
  "type": "keyword",
  "keyword_id": "<kw_action>",
  "args": {"username": "test"},
  "save_as": "login_result"
}

--- Setup/Teardown 结构定义 ---

Setup/Teardown 是测试套件和测试用例的生命周期钩子，用于在测试执行前后自动运行初始化或清理操作。
Setup/Teardown 的步骤树结构与测试用例 steps 完全相同（关键字步骤 + 控制流步骤），支持引用接口关键字、工具方法关键字和 Action 关键字。

SuiteCaseLifecycle（套件内用例级生命周期 — 关联实体）

用于在套件上下文中为每条用例单独配置差异化的 Setup/Teardown，仅在该套件执行该用例时生效。

├── id: UUID
├── suite_id: FK → TestSuite
├── case_id: FK → TestCase
├── setup_steps: JSON            # 套件内该用例的差异化 Setup 步骤树（可选）
├── teardown_steps: JSON         # 套件内该用例的差异化 Teardown 步骤树（可选）
├── created_at: DateTime
└── updated_at: DateTime

四层 Setup/Teardown 体系（四种均可独立配置，也可都不配置）：

| 层级 | 配置位置 | 存储字段 | 执行时机 | 独立开关 |
|------|----------|----------|----------|----------|
| 套件级·整体 | 套件编辑器 | TestSuite.once_setup_steps / once_teardown_steps | 整个套件执行前/后（仅一次） | enable_once_setup_teardown |
| 套件级·每条 | 套件编辑器 | TestSuite.per_case_setup_steps / per_case_teardown_steps | 套件中每条用例执行前/后（统一相同步骤） | enable_per_case_setup_teardown |
| 套件内用例级 | 套件编辑器（每条用例） | SuiteCaseLifecycle.setup_steps / teardown_steps | 该套件中该用例执行前/后（差异化） | 有步骤即生效 |
| 用例级 | 用例编辑器 | TestCase.setup_steps / teardown_steps | 该用例在所有场景执行前/后 | 有步骤即生效 |

四层嵌套执行顺序（四者全部配置时）：
1. Suite Setup（整体，enable_once=true 时仅执行一次）
2.   Suite Setup（每条，enable_per_case=true 时每次循环执行）
3.     Suite-Case Setup（套件内用例级 setup_steps，有步骤时执行）
4.       Case Setup（用例级 setup_steps，有步骤时执行）
5.         Case 步骤（steps）
6.       Case Teardown（用例级 teardown_steps，有步骤时执行）
7.     Suite-Case Teardown（套件内用例级 teardown_steps，有步骤时执行）
8.   Suite Teardown（每条，enable_per_case=true 时每次循环执行）
9. Suite Teardown（整体，enable_once=true 时仅执行一次）

Setup/Teardown 错误处理：
- Setup 失败：跳过当前用例/套件后续步骤，标记为 ERROR
- Teardown 失败：不影响用例结果记录，但在报告中记录 Teardown 错误
- Setup/Teardown 的执行日志独立记录，与用例步骤日志区分

TestPlan (测试计划)
├── id: UUID
├── project_id: FK → Project
├── name: String
├── description: Text
├── suite_ids: JSON             # 关联的测试套件 ID 列表
├── environment_id: FK → Environment
├── schedule_cron: String       # 定时执行 cron 表达式 (可为空)
├── is_active: Boolean
├── created_by: FK → User
├── created_at: DateTime
└── updated_at: DateTime

TestExecution (测试执行记录)
├── id: UUID
├── plan_id: FK → TestPlan
├── environment_id: FK → Environment
├── trigger_type: Enum          # MANUAL / SCHEDULED / CI（CI 为预留，第一期仅开放 Webhook 接口）
├── status: Enum                # PENDING / RUNNING / COMPLETED / FAILED / CANCELLED
├── total_cases: Integer
├── passed_cases: Integer
├── failed_cases: Integer
├── skipped_cases: Integer
├── duration_ms: Integer        # 总耗时（毫秒）
├── started_at: DateTime
├── finished_at: DateTime
├── triggered_by: FK → User
└── created_at: DateTime

TestResult (测试结果明细)
├── id: UUID
├── execution_id: FK → TestExecution
├── case_id: FK → TestCase
├── status: Enum                # PASSED / FAILED / SKIPPED / ERROR
├── actual_result: Text         # 实际结果
├── expected_result: Text       # 预期结果
├── error_message: Text         # 错误信息
├── logs: JSON                  # 执行日志（每步骤的 req/res 详情）
├── duration_ms: Integer
├── started_at: DateTime
└── finished_at: DateTime

User (用户)
├── id: UUID
├── username: String (unique)
├── password_hash: String
├── display_name: String
├── role: Enum                  # ADMIN / USER
├── is_active: Boolean
└── created_at: DateTime
```

### 3.2 实体关系

```
Project 1──N Environment
Project 1──N CodeRepository
CodeRepository 1──N CodeRepositoryPullLog
Project 1──N ApiModule
Project 1──N Keyword
Project 1──N ToolMethod
Project 1──N Action
Project 1──N TestSuite
Project 1──N TestPlan

ApiModule 1──N ApiEndpoint

ApiEndpoint 1──N Keyword  (一个接口可通过 Keyword 配置多组参数，生成多个关键字，keyword_type=API)

ToolMethod 与 Action 通过 nodes JSON 关联（Action 节点可直接引用 ToolMethod，通过 keyword 字段标识）

Action N──N Keyword  (通过 nodes JSON 关联，keyword 节点引用 Keyword)
Action N──N ToolMethod (通过 nodes JSON 关联，可直接引用工具方法)

TestSuite 1──N TestCase
TestSuite 1──N SuiteCaseLifecycle  (通过关联实体为每条用例配置差异化 Setup/Teardown)
SuiteCaseLifecycle N──1 TestCase
TestCase N──N Keyword   (通过 steps JSON 关联 Keyword)
TestCase N──N TestCase  (通过 steps JSON 关联，支持用例嵌套调用)

TestPlan N──N TestSuite (通过 suite_ids JSON 关联)
TestPlan 1──N TestExecution

TestExecution 1──N TestResult
TestResult N──1 TestCase
```

### 3.3 后端架构与执行引擎说明

> auto-test-platform 后端采用 **Java 1.8 + Spring Boot 2.7** 单体应用架构，所有功能模块打包在一个 Spring Boot 应用中，执行引擎内嵌于应用内部，无外部 Python 依赖。
>
> **后端服务包含以下功能模块：**
>
> | 模块 | 说明 |
> |---|---|
> | M1 认证与用户管理 | JWT 签发/刷新、RBAC、用户管理、全局配置 |
> | M2 项目管理 | 项目 CRUD、概览仪表板、项目设置 |
> | M3 环境配置 | 环境 CRUD、JSON 配置编辑、连接测试 |
> | M4 接口文档 | 接口分组、Swagger 导入、接口 CRUD、调试、同步、批量操作 |
> | M5 接口关键字管理 | 接口关键字 CRUD、测试数据配置、删除保护 |
> | M6 工具方法关键字管理 | 工具方法 CRUD、代码沙箱执行、在线测试 |
> | M7 Action 关键字管理 | Action CRUD、流程画布编排、调试、引用管理 |
> | M8 测试用例管理 | 套件、用例 CRUD、步骤编排、参数化、四层 Setup/Teardown |
> | M9 测试执行与调度 | 测试计划、执行触发、实时状态推送 |
> | M10 测试报告与分析 | 执行详情、执行历史、趋势分析、报告导出 |
> | M11 测试代码库 | Git 仓库登记、JGit 代码克隆/拉取、认证凭证加密存储、拉取历史 |
>
> **模块间通信：**
> - 所有模块在同一应用内，通过 Spring Bean 依赖注入直接调用，无网络开销
> - 异步任务：RabbitMQ + Spring AMQP（执行触发、状态通知）
> - 共享存储：MySQL 单实例，各模块操作各自表
> - 缓存共享：Redis 单实例（JWT 黑名单、分布式锁、执行状态）
>
> **执行引擎（内置）：**
> 原 postman-tool Python 引擎的全部能力已用 Java 重新实现，内嵌于后端的 `engine` 包中：
>
> | 能力 | Java 实现方案 |
> |---|---|
> | HTTP 客户端 | OkHttp 4.12，支持 HTTP/2、异步请求 |
> | 断言引擎 | 自研 DSL 解析器 + JSONPath，支持 status_code / body / header / jsonpath 断言 |
> | 工具方法沙箱 | Groovy `ScriptEngine` (JSR-223)，白名单 import + 黑名单 class + 超时/内存限制 |
> | Swagger 解析 | `io.swagger.parser.v3` 官方 Java 库，支持 OpenAPI 2.0 / 3.0 |
> | AST 解析 | `com.github.javaparser`，解析工具方法源码结构 |
> | 协议适配器 | OkHttp + WebSocket API，支持 WSS / Nacos HTTP / Shell 执行 |
>
> **执行引擎内部接口（应用内部调用，非 REST API）：**
> - `KeywordExecutor.execute()`：执行测试用例（加载环境配置、用例步骤、关键字数据）
> - `SandboxEngine.execute()`：在线测试工具方法
> - `ActionExecutor.debug()`：调试 Action
> - `KeywordExecutor.debugCase()`：调试用例
>
> **架构优势：**
> - 统一纯 Java 技术栈，无 Python 依赖，运维复杂度大幅降低
> - 单体应用部署简单，开发调试效率高
> - 模块间直接调用，无网络延迟开销
> - 可通过多实例部署 + Nginx 负载均衡实现水平扩展

---

## 4. 功能需求

### 4.1 项目管理

#### 4.1.1 项目列表

- 展示所有项目卡片/列表视图
- 支持按名称搜索、按状态筛选
- 显示项目基础信息：名称、描述、用例数量、最近执行状态

#### 4.1.2 项目详情

- 项目基本信息编辑（名称、描述、源码路径）
- **源码路径配置**：指定项目在服务端服务器上的源码目录绝对路径，引擎运行时从该路径读取源文件和生成的代码
- 测试概览：质量健康度、资源统计（接口/用例/关键字/套件）、执行分析（通过率/结果分布/趋势）、模块覆盖、质量风险监控

#### 4.1.3 项目设置

- 项目启停（停用后不可执行测试）
- 项目删除（软删除，关联数据归档）

### 4.2 环境配置管理

#### 4.2.1 环境列表

- 展示项目下所有环境
- 支持新增、编辑、删除环境
- 环境快速切换（设为当前激活环境）

#### 4.2.2 环境配置编辑器

- JSON 配置编辑器（Monaco Editor），支持语法高亮、格式化
- 配置字段说明（字段名与引擎侧 `env.json` 保持对齐）：
  - `host`：目标服务地址
  - `host_authorization`：认证信息（Bearer Token、API Key 等）
  - `wss`：WebSocket 地址（可选）
  - `nacos`：外部配置中心（Nacos）地址（可选）
  - `nacos_accessToken`：配置中心认证令牌（可选）
  - `company`：公司/租户编码（可选）
  - 支持自定义扩展字段
- 配置校验：JSON 格式校验、必填字段校验
- 连接测试：保存前可测试目标服务连通性

### 4.3 接口管理

接口管理负责管理被测系统的 HTTP 接口定义，是关键字和测试步骤的底层数据来源。

#### 4.3.1 接口分组

- 按服务/模块对接口进行分组管理（如 "用户管理服务"、"订单管理服务"）
- 系统默认分组：每个项目自动创建「全部」和「未分组」，**不可编辑、不可删除**
- 自定义分组 CRUD（创建、编辑、删除），删除自定义分组时其下接口自动移入「未分组」
- 分组内接口数量统计
- 支持从 Swagger 文件自动创建分组

#### 4.3.2 Swagger 导入

导入流程（分步向导）：

**第一步：上传文件 & 选择分组**
- 上传 Swagger 2.0 JSON 文件
- 选择目标接口分组：
  - 导入到已有分组（下拉选择）
  - 创建新分组（输入分组名称 + 服务前缀）
  - 按 Swagger `tags` 自动拆分到多个分组（每个 tag 对应一个分组，不存在的 tag 自动创建）

**第二步：解析预览**
- 自动解析文件内容：
  - `paths`：提取所有接口路径和 HTTP 方法
  - `parameters`：提取路径参数、查询参数、请求体参数
  - `definitions`：解析 `$ref` 引用，展开嵌套 DTO 和数组结构
- 展示解析出的接口列表，支持勾选要导入的接口
- 增量导入：已有接口按 path+method 匹配，更新参数定义，不删除多余接口

**第三步：确认导入**
- 展示导入摘要（新增 N 条、更新 M 条）
- 确认后执行导入
- 导入后自动提取接口元数据写入数据库（不生成代码文件，与适配方案保持一致）

#### 4.3.3 接口列表

- 按分组树形展示 / 平铺列表切换
- 搜索：按接口名称、路径模糊搜索
- 筛选：按 HTTP 方法、分组、来源筛选
- 统计：各分组接口数量、总接口数

#### 4.3.4 接口删除保护

接口是关键字和测试步骤的基础数据，删除时必须执行依赖检查，禁止直接删除被引用的接口。

**删除前依赖检查流程：**

```
删除接口
  ↓
检查关联关键字（ApiKeyword.endpoint_id）
  ├── 无关联 → 允许删除
  └── 有关联 → 拒绝删除，展示依赖详情
          ↓
       列出所有关联关键字
          ↓
       每个关联关键字 → 列出引用该关键字的测试用例
```

**依赖详情弹窗展示：**

| 层级 | 展示信息 |
|---|---|
| 关联关键字 | 关键字名称、所属模块、关联用例数 |
| 关联测试用例 | 用例名称、所属套件、优先级 |

**解除依赖路径：**
- 方式一：进入关键字详情，取消关联该接口（解除 `endpoint_id` 绑定）
- 方式二：删除不再需要的关键字（需先解除该关键字与用例的关联）
- 方式三：从用例步骤中移除引用该关键字的步骤

#### 4.3.5 接口编辑

- **基础信息**：接口名称、路径、HTTP 方法、描述
- **请求参数**：
  - Path 参数表格（参数名、类型、是否必填、说明）
  - Query 参数表格
  - Header 参数表格
- **请求体**（POST/PUT）：
  - JSON Schema 编辑器
  - 支持手动定义字段（名称、类型、必填、描述）
  - 支持从示例 JSON 自动推断 Schema
- **响应定义**：
  - 多状态码响应定义（200/400/500 等）
  - 响应体 JSON Schema
- **调试**：选择环境后直接发送请求测试接口
  - 填写参数值 → 发送请求 → 展示响应结果

#### 4.3.6 接口同步

- 重新上传 Swagger 文件，与现有接口对比
- 差异展示：新增接口、参数变更、已废弃接口
- 确认后执行同步更新
- 同步历史记录

#### 4.3.7 批量操作

- 批量启用/禁用接口
- 批量移动到其他分组
- 批量删除（同样执行依赖检查，仅删除无关联的接口，有依赖的接口跳过并提示）

### 4.4 关键字管理

关键字管理统一管理平台中的所有可调用单元。基于 Keyword 统一实体，通过 keyword_type 区分不同类型：

- **接口关键字** (API)：接口 + 预设测试数据
- **工具方法** (TOOL)：工具方法自身的关键字字段（不超过20个字符），用于 Action 节点直接引用
- **Action 关键字** (ACTION)：关键字组合 + 逻辑控制
- **测试用例关键字** (TEST_CASE)：测试用例作为关键字复用

所有类型的关键字统一支持外部传参（通过 `args`）和返回值（通过 `save_as`），实现灵活的数据流转。新增关键字类型只需扩展 keyword_type 枚举值 + 对应源实体。

```
接口 + 测试数据 = 接口关键字 (keyword_type=API)
工具方法.keyword 字段 = 工具方法引用标识（在 Action 节点中直接引用）
关键字组合 + 逻辑控制 = Action 关键字 (keyword_type=ACTION)
测试用例作为关键字 = 测试用例关键字 (keyword_type=TEST_CASE)
```

#### 4.4.1 接口关键字

接口关键字是对接口的封装层：为一个接口配置预设的测试数据，形成一个可复用的接口关键字。同一个接口可以配置多组不同测试数据，生成多个接口关键字（如正常查询、边界值、异常场景）。

```
接口 + 测试数据 = 接口关键字
```

##### 4.4.1.1 接口关键字列表

- 按分类分组展示（用户管理、订单管理、支付流程等）
- 搜索：按名称、分类、关联接口路径筛选
- 列表显示：名称、关联接口、测试数据概要、被 Action 引用次数
- 统计：各分类接口关键字数量

##### 4.4.1.2 创建接口关键字

- **选择接口**：从接口列表中选择目标接口（自动加载接口的参数定义）
- **配置测试数据**：
  - 根据接口参数定义自动表单化：Path 参数、Query 参数、请求体
  - 填写预设参数值（支持变量引用 `${var}`）
  - 支持多组数据值（数据驱动场景）
- **设置预期响应**（可选）：
  - 定义预期响应状态码、响应体关键字段
  - 用于执行时自动断言
- **基础信息**：名称、描述、分类、标签
- **快速创建**：支持在接口详情页直接生成接口关键字（自动填充参数）

##### 4.4.1.3 接口关键字详情

- 关联接口信息：接口名称、路径、HTTP 方法
- 测试数据表格：参数名、类型、预设值、说明
- 预期响应定义
- 引用关系：查看哪些 Action 引用了该接口关键字

##### 4.4.1.4 删除保护

被 Action 引用的接口关键字不可直接删除
- 需先解除 Action 引用，才能删除

##### 4.4.1.5 接口关键字传参与返回机制

接口关键字同样支持外部传参和返回机制，在 Action 节点中被调用时实现灵活的数据流转。

**传参机制：**

- 接口关键字基于关联接口的参数定义，在创建时配置预设测试数据（`test_data`）
- 调用方（Action 节点）通过 `args` 覆盖预设测试数据中的部分或全部参数值（外部传参）
- 参数值支持变量引用 `${var}`
- 未覆盖的参数使用预设测试数据中的值

**返回机制：**

- 接口关键字执行后返回 HTTP 响应（状态码、响应头、响应体）
- 调用方通过 `save_as` 将响应体存入执行上下文变量
- 下游节点可通过 `${变量名}` 引用该响应数据
- 支持 `expected_response` 定义预期响应，执行时自动断言

**Action 节点中调用示例：**

```json
{
  "type": "keyword",
  "ref_type": "api",
  "keyword_id": "<keyword_id>",
  "args": {
    "page": 1,
    "size": "${page_size}"
  },
  "save_as": "api_response",
  "expected_response": {
    "status_code": 200
  }
}
```

数据流转：`预设测试数据 → args 外部覆盖 → 组装 HTTP 请求 → 发送请求 → 响应体通过 save_as 存入上下文 → 下游通过 ${api_response} 引用`

#### 4.4.2 工具方法

工具方法是测试过程中的辅助能力，用于数据转换、加密、时间处理等通用操作。与关键字不同，工具方法不直接调用被测系统接口，而是提供纯计算/转换能力。

##### 4.4.2.1 工具方法列表

- 按分类分组展示（加密、时间、数据转换、字符串、自定义等）
- 搜索：按名称、分类筛选
- 统计：各分类方法数量
- 区分内置方法和用户自定义方法

##### 4.4.2.2 内置工具方法

平台预置常用工具方法，开箱即用。以下方法直接复用 postman-tool 的 `common/utils/tools/` 模块源码：

**加密工具**

| 方法名 | 说明 | 参数 | 返回值 |
|---|---|---|---|
| md5_encrypt | MD5 哈希（UTF-8 编码后计算） | text: str | str（32 位小写十六进制） |
| base64_encode | Base64 编码 | text: str | str |
| base64_decode | Base64 解码 | text: str | str |

**时间工具**

| 方法名 | 说明 | 参数 | 返回值 |
|---|---|---|---|
| time_now_timestamp | 获取当前时间戳（秒级精度） | 无 | int |
| time_now_time | 获取当前时间字符串（Asia/Shanghai 时区） | 无 | str（YYYY-MM-DD HH:MM:SS.ffffff） |
| get_now_date | 获取当前日期字符串 | 无 | str（YYYY-MM-DD） |

**数据转换工具**

| 方法名 | 说明 | 参数 | 返回值 |
|---|---|---|---|
| json_to_string | JSON 对象转字符串 | obj: dict | str |
| string_to_json | 字符串转 JSON 对象 | text: str | dict |
| url_encode | URL 编码 | text: str | str |
| url_decode | URL 解码 | text: str | str |

**平台扩展工具方法（待实现）**

以下方法平台应内置，以覆盖更多通用测试场景：

| 方法名 | 分类 | 说明 | 参数 | 返回值 |
|---|---|---|---|---|
| random_string | 字符串 | 生成随机字符串 | length: int | str |
| random_int | 字符串 | 生成指定范围的随机整数 | min: int, max: int | int |
| regex_extract | 字符串 | 正则表达式提取 | text: str, pattern: str | str |
| hmac_sha256 | 加密 | HMAC-SHA256 签名 | text: str, key: str | str |

##### 4.4.2.3 自定义工具方法

- **创建工具方法**：
  - 方法名称、关键字（不超过 20 个字符）、分类（可选已有分类或新建）、描述
  - **Python 代码编辑器**（Monaco Editor）：
    - 用户编写完整的 Python 函数实现
    - 支持语法高亮、自动补全
    - 代码规范：必须定义为 `def method_name(...)` 形式，`return` 返回结果
    - 仅允许使用平台白名单内的标准库（防沙箱逃逸）
  - 参数定义：名称、类型、是否必填、默认值、说明
  - 返回类型和返回值说明
- **代码模板**：提供代码骨架，自动填充参数和返回值模板
- **在线测试**：填写参数值 → 执行方法 → 展示返回值和耗时

##### 4.4.2.4 工具方法安全限制

- **沙箱执行**：工具方法代码在受限的 Python 沙箱中执行
- **允许的库**：`json`, `math`, `random`, `datetime`, `hashlib`, `base64`, `re`, `uuid` 等安全库
- **禁止操作**：
  - 禁止文件读写（`open`, `os`, `sys`）
  - 禁止网络访问（`requests`, `socket`）
  - 禁止子进程执行（`subprocess`, `exec`）
- **超时控制**：单次执行超时默认 5s
- **内存限制**：单次执行内存上限 64MB

##### 4.4.2.5 工具方法传参与返回机制

工具方法支持完整的外部传参和返回值机制，使其能在 Action 节点和测试用例中被灵活调用。

**传参机制：**

- 工具方法通过 `parameters` 字段定义输入参数（名称、类型、是否必填、默认值、说明）
- 调用方（Action 节点或测试用例步骤）通过 `args` 传入实际参数值
- 参数值支持变量引用 `${var}`，可引用执行上下文中的上游变量或环境变量
- 必填参数未传值且无默认值时，执行前报错

**返回机制：**

- 工具方法通过 `return` 语句返回结果，返回类型由 `return_type` 定义
- 调用方通过 `save_as` 将返回值存入执行上下文变量
- 下游节点可通过 `${变量名}` 引用该返回值
- 未设置 `save_as` 时，返回值仅记录在执行日志中，不存入上下文

**Action 节点中调用示例：**

```json
{
  "type": "keyword",
  "ref_type": "tool_method",
  "tool_method_id": "<tool_method_id>",
  "args": {
    "text": "${user_name}",
    "salt": "abc123"
  },
  "save_as": "encrypted_value"
}
```

执行流程：`args 传入参数 → 工具方法执行 → return 返回值 → save_as 存入上下文 → 下游通过 ${encrypted_value} 引用`

**在线测试：**

- 在工具方法编辑页可直接填写参数值进行在线测试，展示返回值和执行耗时
- 支持查看参数类型校验结果

**类型校验：**

- 参数定义中的 `type` 字段用于传参时的类型校验
- 不匹配时在调试模式下给出警告，执行模式下报错中断

##### 4.4.2.6 工具方法关键字

一个工具方法只能有一个关键字，定义在工具方法实体的 `keyword` 字段中，由用户在工具方法编辑页的"基础信息"中自定义输入，长度不超过 20 个字符。

**关键字规则：**

- 关键字在工具方法编辑页"基础信息"卡片中配置，与方法名称、分类、描述同级
- 长度不超过 20 个字符
- 同一项目内关键字唯一（用于 Action 节点精确引用）
- 关键字在 Action 节点编排器中作为该工具方法的引用标识

**Action 节点中的引用方式：**

- Action 节点通过 `ref_type=tool_method` 直接引用 ToolMethod
- 通过 `args` 传入参数值（支持变量引用 `${var}`）
- 通过 `save_as` 保存工具方法返回结果为变量（返回）
- 数据流转与接口关键字一致

#### 4.4.3 Action 关键字

Action 关键字是接口关键字和测试用例之间的编排层。一个 Action 组合一个或多个接口关键字/工具方法，通过逻辑控制（串行、并行、条件判断、等待）封装成可复用的业务动作单元。

```
接口关键字 + 工具方法关键字 + 逻辑控制 = Action 关键字
```

##### 4.4.3.1 Action 关键字列表

- 按分类分组展示（登录认证、用户管理、订单处理等）
- 搜索：按名称、分类、标签筛选
- 列表显示：名称、描述、内部节点数量、被引用用例数
- 统计：各分类 Action 关键字数量

##### 4.4.3.2 Action 编辑器

- **基础信息**：名称、描述、分类、标签
- **输入/输出参数定义**：
  - 输入参数：名称、类型、是否必填、默认值、说明（用例调用时传入）
  - 输出参数：名称、类型、说明（Action 执行后返回给用例）
- **节点编排器**（核心功能）：
  - 可视化树形编排界面，支持拖拽添加节点
  - **节点类型**：
    - **关键字节点**：统一引用 Keyword 实体，通过 `args` 传入输入参数，通过 `save_as` 保存输出为变量；支持按 keyword_type（API/TOOL/ACTION/TEST_CASE）筛选
    - **工具方法直引节点**：直接引用 ToolMethod（绕过 Keyword 封装层），通过 `args` 直接传参，通过 `save_as` 保存返回结果
    - **串行节点**：子节点按顺序依次执行（默认根节点类型）
    - **并行节点**：子节点同时执行，全部完成后继续
    - **条件节点**：根据表达式判断走 then/else 分支，支持变量引用 `${var}`
    - **等待节点**：固定等待时长（毫秒）
  - 节点支持嵌套：并行节点内可嵌套串行/条件节点
  - 节点间变量传递：上游节点的 `save_as` 变量可在下游节点通过 `${var}` 引用
  - 画布工具栏：缩放控制、删除选中、**一键格式化**（基于拓扑排序自动排列节点为整洁的层级布局，逻辑判断分支「是」左「否」右）、**一键清空**（确认后移除所有节点和连线）、全屏编辑

##### 4.4.3.3 Action 调试

- 选择环境后直接执行 Action
- 填写输入参数值
- 实时展示每个节点的执行状态、耗时、请求/响应日志
- 变量值实时展示：查看每个 `save_as` 变量的实际值

##### 4.4.3.4 Action 引用管理

- 查看哪些测试用例引用了该 Action 关键字
- 删除保护：被用例引用的 Action 关键字不可删除，需先解除引用

### 4.5 测试用例管理

#### 4.5.1 测试套件

- 套件 CRUD（创建、编辑、删除）
- 套件内用例排序、批量操作
- 套件标签管理

#### 4.5.2 用例列表

- 列表/卡片视图切换
- 筛选：按套件、标签、优先级、状态筛选
- 搜索：用例名称、描述模糊搜索
- 批量操作：启用/禁用、移动、删除、打标签

#### 4.5.3 用例编辑器

- **基础信息**：用例名称、描述、前置条件、优先级、标签
- **步骤编排**（核心功能）：
  - 拖拽排序步骤顺序
  - 步骤树结构（与 Action 节点树相同，支持嵌套逻辑控制）：
    - **关键字步骤**：统一引用 Keyword 实体，传入输入参数，保存输出变量；支持按 keyword_type 筛选
    - **串行节点**：子步骤按顺序执行
    - **并行节点**：子步骤同时执行
    - **条件节点**：根据变量表达式走 then/else 分支
    - **等待节点**：固定等待时长
  - 变量引用：通过 `${变量名}` 引用上游 Action 的输出变量或环境变量
  - 步骤支持复制、删除
- **校验配置**（可选）：
  - 每个关键字步骤可配置 0 到多个校验（断言），未配置校验的步骤仅执行不断言
  - 校验类型：equal / not_equal / include / not_include / true / not_true（复用 HandlerAssert 断言引擎）
  - 校验配置项：断言类型、实际值（支持 `${变量名}` 引用步骤输出或环境变量）、预期值、校验描述
  - 用例组成形式：
    - **测试步骤 + 校验**：步骤执行后通过断言验证结果，适用于需要明确验证的场景
    - **仅测试步骤**：步骤执行后不做断言，仅记录执行日志，适用于流程验证或前置准备场景
- **参数化**：
  - 支持数据驱动：为 Action 输入参数设置多组数据，自动生成多条执行
  - 数据源：手动输入表格、CSV 导入

#### 4.5.4 用例调试

- 单用例试运行：选择环境后直接执行
- 实时日志：WebSocket 推送执行过程中的请求/响应日志
- 调试结果：每步骤的通过/失败状态、实际值、错误信息

### 4.6 测试计划与执行

#### 4.6.1 测试计划

- 计划 CRUD
- 关联测试套件（多选）
- 绑定执行环境
- 定时执行配置：
  - Cron 表达式编辑器（可视化）
  - 启停定时任务
  - 执行时间预览

#### 4.6.2 执行触发

- **手动执行**：在计划详情页点击“立即执行”
  - 可选择覆盖环境配置
- **定时执行**：APScheduler 按 cron 表达式自动触发
- **CI/CD 触发（预留）**：提供 Webhook 接口供外部系统（Jenkins/GitLab CI 等）触发计划执行，第一期仅开放接口，不做前端配置页
- 执行状态实时推送（WebSocket）：
  - PENDING → RUNNING → COMPLETED/FAILED/CANCELLED
  - 当前执行进度（第 N/M 个用例）

#### 4.6.3 执行队列

- 展示当前待执行、正在执行的队列
- 支持取消正在执行的测试
- 并发控制：同一项目同时只允许一个执行任务（可配置）

### 4.7 测试报告与分析

#### 4.7.1 执行详情

- 执行概要：总数、通过、失败、跳过、耗时
- 通过率进度环/饼图
- 用例结果列表：每条用例的状态、耗时、错误摘要
- 用例详情展开：每步骤的请求/响应日志、断言结果
- 失败用例快速定位（仅显示失败项）

#### 4.7.2 执行历史

- 按时间线展示所有执行记录
- 筛选：按项目、计划、状态、时间范围筛选
- 每次执行的概要统计

#### 4.7.3 趋势分析

- 通过率趋势折线图（按天/周/月）
- 执行耗时趋势
- 各模块失败率热力图
- 高频失败用例 TOP 10

#### 4.7.4 历史对比

- 选择两次执行记录进行对比
- 对比维度：通过率变化、新增失败、修复用例、耗时变化
- 差异高亮展示

#### 4.7.5 报告导出

- PDF 导出：包含概要、图表、详细结果
- Excel 导出：结构化数据，便于二次分析
- 导出范围：单次执行报告 / 自定义时间范围汇总报告

### 4.8 用户认证

#### 4.8.1 登录

- 用户名 + 密码登录
- JWT Token 认证（Access Token + Refresh Token）
- 记住登录状态

#### 4.8.2 用户管理（管理员）

- 用户列表
- 管理员手动创建/编辑/禁用/删除用户（不支持自助注册，后续迭代开发）
- 角色分配：ADMIN（全权限）/ USER（测试执行+查看）
- 菜单权限控制：顶部用户头像下拉菜单中的【系统设置】仅当当前用户角色为 ADMIN 时显示；USER 角色隐藏该入口。
- **admin 账号保护**：账号为 `admin` 的系统内置管理员账号，不允许修改用户名（`display_name`）、不允许分配角色、不允许禁用、不允许删除；在用户管理列表中仅显示「重置密码」操作按钮；在个人资料页中用户名、账号、角色字段均为只读。
- **用户删除规则**：除账号为 `admin` 的系统内置管理员外，其他用户均允许删除；删除前需二次确认，删除后数据不可恢复。
- **用户名保留规则**：新建用户或修改用户名时，用户名（`display_name`）不能为「管理员」，该名称为系统保留，仅供 `admin` 账号使用。
- **账号保留规则**：新建用户或修改账号时，账号（`username`）不能使用 `admin`（不区分大小写），该账号为系统内置管理员专用。

### 4.9 系统设置

#### 4.9.1 全局配置

- 执行超时默认值
- 并发执行上限
- 日志保留天数
- 报告保留天数

#### 4.9.2 通知配置（预留）

- 邮件通知：执行完成后发送结果摘要
- Webhook 通知：对接企业微信/钉钉

### 4.10 测试代码库

测试代码库负责登记项目关联的 Git 仓库并拉取代码到平台服务器本地，供测试执行使用。基于 JGit（纯 Java 实现的 Git 客户端库）完成克隆与增量更新，服务器无需安装 Git。

#### 4.10.1 仓库登记

- 项目内登记多个 Git 仓库：仓库名称（项目内唯一，≤50 字符）、Git 地址（http(s):// 或 git@ 形式，≤500 字符）、分支（留空使用仓库默认分支，≤100 字符）、描述
- 仓库列表展示：名称、Git 地址、分支、认证（有/无）、最近拉取状态、最近拉取时间、最近 Commit（截断 8 位）
- 编辑仓库时认证密码留空表示保持不变，密码不回显、不回传（仅返回是否已配置认证）
- 删除仓库需二次确认，同时物理删除仓库记录与服务器本地已拉取的代码目录（拉取历史随外键级联删除）

#### 4.10.2 代码拉取

- 拉取策略（本地存储目录按 `{存储根目录}/{projectId}/{repoId}` 组织，以 repoId 命名，仓库名可修改不影响目录）：
  - 本地目录不存在 → 克隆（CLONE）
  - 本地目录已存在 → 增量更新（PULL），拉取前先 fetch 同步远程引用；配置分支与当前分支不一致时自动切换分支
- 指定分支时克隆/更新该分支，留空使用仓库默认分支
- 拉取成功后回写仓库表：最近拉取状态、拉取时间、HEAD Commit ID、本地存储路径
- 拉取失败不中断业务：以 HTTP 200 + 业务失败状态（success=false）返回，错误信息保存至拉取历史
- 克隆失败时自动清理残留目录，保证下次可重新克隆
- 拉取超时可配置（默认 300 秒）；前端拉取请求超时单独放宽至 10 分钟（防止大仓库超时）；同一仓库拉取期间按钮置 loading 防重复触发

#### 4.10.3 认证与加密

- 支持私有仓库认证：认证用户名 + 密码/Access Token（用户名密码凭证方式，兼容 Token 场景）
- 凭证加密存储：AES-128/CBC/PKCS5Padding 加密，随机 IV 前置拼接后 Base64，密文带 `enc:` 前缀；密钥可配置
- 查询接口不回传密码，仅返回是否配置认证（hasAuth 布尔值）

#### 4.10.4 拉取历史

- 每次拉取（含失败）记录一条历史：类型（CLONE 克隆/PULL 更新）、分支、状态（RUNNING 拉取中/SUCCESS 成功/FAILED 失败）、Commit ID、错误信息、耗时
- 拉取记录抽屉展示最近 20 条历史
- 状态文案由数据字典 `repository_pull_status` 统一维护

---

## 5. 页面结构

### 5.1 导航结构

```
顶部导航栏
├── Logo + 项目名称
├── 全局搜索框
├── 环境快速切换下拉
└── 用户头像下拉
    ├── 个人资料
    ├── 修改密码
    ├── 系统设置（仅 ADMIN 可见）
    └── 退出登录

左侧导航菜单（项目维度）
├── 📊 项目概览（仪表盘）
├── ⚙ 环境配置
├── 📦 测试代码库
├── 🌐 接口管理
├── 🔑 关键字管理
│   ├── 接口关键字
│   ├── 工具方法
│   ├── Action 关键字
│   └── 测试用例关键字
├── 📋 测试套件
├── 📝 测试用例
├── 📅 测试计划
├── ▶ 执行记录
├── 📈 报告分析
│   ├── 趋势分析
│   └── 历史对比
└── ⚙ 系统设置
    ├── 用户管理
    └── 全局配置
```

### 5.2 核心页面清单

| 页面 | 路径 | 说明 |
|---|---|---|
| 登录 | `/login` | 登录页面 |
| 项目列表 | `/projects` | 所有项目卡片视图 |
| 项目概览 | `/projects/:id` | 项目仪表盘 |
| 环境配置 | `/projects/:id/environments` | 环境列表 + 配置编辑器 |
| 测试代码库 | `/projects/:id/repositories` | 仓库列表 + 拉取记录抽屉 |
| 接口管理 | `/projects/:id/apis` | 接口分组 + 接口列表 |
| 接口导入 | `/projects/:id/apis/import` | Swagger 导入向导 |
| 接口编辑 | `/projects/:id/apis/:apiId/edit` | 接口参数/请求体/响应编辑 |
| 接口调试 | `/projects/:id/apis/:apiId/debug` | 接口调试（发请求看响应） |
| 接口关键字 | `/projects/:id/keywords?type=api` | 接口关键字列表（按分类分组） |
| 创建接口关键字 | `/projects/:id/keywords/api/create` | 选择接口 + 配置测试数据 |
| 接口关键字编辑 | `/projects/:id/keywords/api/:kwId/edit` | 编辑测试数据 + 预期响应 |
| 工具方法 | `/projects/:id/keywords?type=tool` | 工具方法及工具方法关键字列表 |
| 工具方法编辑 | `/projects/:id/keywords/tool/:toolId/edit` | 工具方法代码编辑器 + 参数定义 |
| Action 关键字 | `/projects/:id/keywords?type=action` | Action 关键字列表 |
| Action 关键字编辑 | `/projects/:id/keywords/action/:actionId/edit` | Action 节点编排器 |
| Action 调试 | `/projects/:id/keywords/action/:actionId/debug` | Action 调试执行 |
| 测试套件 | `/projects/:id/suites` | 套件列表 + 详情 |
| 用例列表 | `/projects/:id/cases` | 用例列表视图 |
| 用例编辑 | `/projects/:id/cases/:caseId/edit` | 用例步骤编排器 |
| 测试计划 | `/projects/:id/plans` | 计划列表 + 详情 |
| 计划编辑 | `/projects/:id/plans/:planId/edit` | 计划配置 |
| 执行记录 | `/projects/:id/executions` | 执行历史列表 |
| 执行详情 | `/projects/:id/executions/:execId` | 单次执行报告 |
| 趋势分析 | `/projects/:id/analytics/trends` | 图表分析页 |
| 历史对比 | `/projects/:id/analytics/compare` | 执行对比页 |
| 用户管理 | `/settings/users` | 用户 CRUD |
| 全局配置 | `/settings/config` | 系统参数配置 |

---

## 6. API 设计

### 6.1 API 规范

- RESTful 风格
- 统一响应格式：`{ "code": 0, "message": "success", "data": {} }`
- 分页格式：`{ "items": [], "total": N, "page": 1, "page_size": 20 }`
- 版本前缀：`/api/v1/`
- 认证：`Authorization: Bearer <JWT Token>`

### 6.2 API 模块概览

#### 认证模块

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/auth/login` | 用户登录 |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| POST | `/api/v1/auth/logout` | 退出登录 |

#### 项目模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects` | 项目列表 |
| POST | `/api/v1/projects` | 创建项目 |
| GET | `/api/v1/projects/:id` | 项目详情 |
| PUT | `/api/v1/projects/:id` | 更新项目 |
| DELETE | `/api/v1/projects/:id` | 删除项目 |
| GET | `/api/v1/projects/:id/dashboard` | 项目仪表盘数据 |

#### 环境模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/environments` | 环境列表 |
| POST | `/api/v1/projects/:pid/environments` | 创建环境 |
| GET | `/api/v1/projects/:pid/environments/:id` | 环境详情 |
| PUT | `/api/v1/projects/:pid/environments/:id` | 更新环境 |
| DELETE | `/api/v1/projects/:pid/environments/:id` | 删除环境 |
| POST | `/api/v1/projects/:pid/environments/:id/test` | 测试连接 |

#### 测试代码库模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/repositories` | 仓库列表 |
| POST | `/api/v1/projects/:pid/repositories` | 新建仓库 |
| POST | `/api/v1/projects/:pid/repositories/:id` | 编辑仓库（密码留空保持不变） |
| POST | `/api/v1/projects/:pid/repositories/:id/delete` | 删除仓库（含本地代码目录） |
| POST | `/api/v1/projects/:pid/repositories/:id/pull` | 拉取代码（克隆/增量更新，失败也返回 HTTP 200 + 业务状态） |
| GET | `/api/v1/projects/:pid/repositories/:id/pull-logs` | 拉取历史（最近 20 条） |

#### 接口模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/apis` | 接口列表（支持树形/平铺） |
| GET | `/api/v1/projects/:pid/apis/modules` | 接口分组列表 |
| POST | `/api/v1/projects/:pid/apis/modules` | 创建接口分组 |
| PUT | `/api/v1/projects/:pid/apis/modules/:id` | 更新接口分组 |
| DELETE | `/api/v1/projects/:pid/apis/modules/:id` | 删除接口分组 |
| GET | `/api/v1/projects/:pid/apis/:id` | 接口详情 |
| POST | `/api/v1/projects/:pid/apis` | 手动创建接口 |
| PUT | `/api/v1/projects/:pid/apis/:id` | 更新接口 |
| DELETE | `/api/v1/projects/:pid/apis/:id` | 删除接口 |
| POST | `/api/v1/projects/:pid/apis/import/swagger` | Swagger 文件导入 |
| POST | `/api/v1/projects/:pid/apis/sync` | 接口同步（重新导入 Swagger） |
| POST | `/api/v1/projects/:pid/apis/:id/debug` | 接口调试（发送请求） |
| POST | `/api/v1/projects/:pid/apis/batch` | 批量操作（启用/禁用/移动/删除） |

#### 关键字模块

统一的关键字管理 API，基于 Keyword 统一实体，通过 keyword_type 区分接口关键字、工具方法关键字、Action 关键字、测试用例关键字四种类型。

**通用接口：**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/keywords` | 关键字列表（通过 `type` 参数筛选：api/tool/action/test_case） |
| GET | `/api/v1/projects/:pid/keywords/categories` | 分类列表（支持按 `type` 筛选） |

**接口关键字：**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/keywords/api` | 接口关键字列表 |
| GET | `/api/v1/projects/:pid/keywords/api/:id` | 接口关键字详情 |
| POST | `/api/v1/projects/:pid/keywords/api` | 创建接口关键字 |
| PUT | `/api/v1/projects/:pid/keywords/api/:id` | 更新接口关键字 |
| DELETE | `/api/v1/projects/:pid/keywords/api/:id` | 删除（引用保护） |
| POST | `/api/v1/projects/:pid/apis/:apiId/quick-keyword` | 从接口快速生成接口关键字 |

**工具方法：**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/keywords/tool` | 工具方法列表（支持按分类筛选） |
| GET | `/api/v1/projects/:pid/keywords/tool/categories` | 工具方法分类列表 |
| GET | `/api/v1/projects/:pid/keywords/tool/:id` | 工具方法详情 |
| POST | `/api/v1/projects/:pid/keywords/tool` | 创建工具方法（含 keyword 字段） |
| PUT | `/api/v1/projects/:pid/keywords/tool/:id` | 更新工具方法（含代码和 keyword） |
| DELETE | `/api/v1/projects/:pid/keywords/tool/:id` | 删除工具方法（内置不可删） |
| POST | `/api/v1/projects/:pid/keywords/tool/:id/test` | 在线测试执行 |

**Action 关键字：**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/keywords/action` | Action 关键字列表 |
| GET | `/api/v1/projects/:pid/keywords/action/categories` | Action 分类列表 |
| GET | `/api/v1/projects/:pid/keywords/action/:id` | Action 关键字详情 |
| POST | `/api/v1/projects/:pid/keywords/action` | 创建 Action 关键字 |
| PUT | `/api/v1/projects/:pid/keywords/action/:id` | 更新（含节点编排） |
| DELETE | `/api/v1/projects/:pid/keywords/action/:id` | 删除（引用保护） |
| POST | `/api/v1/projects/:pid/keywords/action/:id/debug` | Action 调试执行 |
| GET | `/api/v1/projects/:pid/keywords/action/:id/references` | 查看引用该 Action 关键字的用例 |

#### 用例模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/suites` | 套件列表 |
| POST | `/api/v1/projects/:pid/suites` | 创建套件 |
| PUT | `/api/v1/projects/:pid/suites/:id` | 更新套件 |
| DELETE | `/api/v1/projects/:pid/suites/:id` | 删除套件 |
| GET | `/api/v1/projects/:pid/cases` | 用例列表 |
| POST | `/api/v1/projects/:pid/cases` | 创建用例 |
| GET | `/api/v1/projects/:pid/cases/:id` | 用例详情 |
| PUT | `/api/v1/projects/:pid/cases/:id` | 更新用例 |
| DELETE | `/api/v1/projects/:pid/cases/:id` | 删除用例 |
| POST | `/api/v1/projects/:pid/cases/:id/debug` | 用例调试执行 |
| POST | `/api/v1/projects/:pid/cases/import` | 用例批量导入 |

#### 执行模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/plans` | 计划列表 |
| POST | `/api/v1/projects/:pid/plans` | 创建计划 |
| PUT | `/api/v1/projects/:pid/plans/:id` | 更新计划 |
| DELETE | `/api/v1/projects/:pid/plans/:id` | 删除计划 |
| POST | `/api/v1/projects/:pid/plans/:id/execute` | 手动执行计划 |
| POST | `/api/v1/projects/:pid/plans/:id/schedule` | 配置定时任务 |
| GET | `/api/v1/projects/:pid/executions` | 执行记录列表 |
| GET | `/api/v1/projects/:pid/executions/:id` | 执行详情 |
| POST | `/api/v1/projects/:pid/executions/:id/cancel` | 取消执行 |
| GET | `/api/v1/projects/:pid/executions/:id/results` | 执行结果明细 |
| POST | `/api/v1/webhook/execute/:planId` | CI/CD Webhook 触发（预留，无需 JWT，使用 Token 校验） |

#### 报告模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/projects/:pid/analytics/trends` | 趋势分析数据 |
| GET | `/api/v1/projects/:pid/analytics/compare` | 历史对比数据 |
| GET | `/api/v1/projects/:pid/analytics/top-failures` | 高频失败用例 |
| GET | `/api/v1/projects/:pid/executions/:id/export/pdf` | 导出 PDF 报告 |
| GET | `/api/v1/projects/:pid/executions/:id/export/excel` | 导出 Excel 报告 |
| GET | `/api/v1/projects/:pid/analytics/export/pdf` | 导出汇总报告 PDF |
| GET | `/api/v1/projects/:pid/analytics/export/excel` | 导出汇总报告 Excel |

#### 用户模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/users` | 用户列表 |
| POST | `/api/v1/users` | 创建用户 |
| PUT | `/api/v1/users/:id` | 更新用户 |
| DELETE | `/api/v1/users/:id` | 删除用户（admin 账号不可删） |
| PATCH | `/api/v1/users/:id/status` | 启用/禁用用户 |

#### 系统模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/settings` | 获取全局配置 |
| PUT | `/api/v1/settings` | 更新全局配置 |

### 6.3 WebSocket 接口

| 路径 | 说明 |
|---|---|
| `/ws/execution/:executionId` | 执行过程实时日志推送 |
| `/ws/case/:caseId/debug` | 用例调试实时日志 |

---

## 7. 核心引擎复用方案

> 本节描述如何将 postman-tool 的核心引擎抽取并改造为平台的通用引擎层。postman-tool 作为引擎的第一个实现实例，其改造方案具有通用性，可应用于其他项目的引擎接入。

### 7.1 引擎层抽取

将 postman-tool 的 `common/` 目录抽取为独立 Python 包 `postman-engine`，作为 auto-test-platform 后端的内部依赖。以下模块直接复用源码，无需重写：

```
postman-engine/
├── handlerKw/                   # 关键字处理引擎（完整复用）
│   ├── kwDecorator.py           #   @KwDecorator 装饰器类（中文描述标记）
│   ├── handlerKw.py             #   HandlerKw.kw() 关键字封装（lambda 包装）
│   ├── runKw.py                 #   RunKw 执行器（ts_run/act_run/api_run/tools_run 四种场景）
│   ├── convertKw.py             #   ConvertKeyKw AST 解析器（源文件 → 关键字字典）
│   └── convertAllKw.py          #   批量转换入口（WSS/ACT/API/NCS/TC 五类）
├── handlerSwagger/              # Swagger 代码生成（完整复用）
│   ├── convertSwagger.py        #   ConvertApiDoc 转换器（$ref 解析 + DTO 展开 + 代码生成）
│   └── convertAllSwagger.py     #   Swagger 批量转换入口
├── http/                        # HTTP 通信（复用 + 适配）
│   ├── httpMethod.py            #   HttpMethod 请求参数验证 + (path,method,data) 三元组构建
│   └── httpRq.py                #   HttpRq 请求执行器（GET/POST/DELETE，自动 Authorization + 响应日志）
└── utils/                       # 工具库（复用 + 适配）
    ├── handleEnv.py             #   HandleEnv 环境配置读取（适配改造）
    ├── handlerAssert.py         #   HandlerAssert 断言引擎（6 种断言：equal/not_equal/include/not_include/true/not_true）
    ├── handlerFile.py           #   HandlerFile 文件操作
    ├── handlerNacos.py          #   配置中心 HTTP 客户端（适配为通用配置中心接口）
    ├── handlerWss.py            #   WebSocket 客户端（适配为通用 WebSocket 协议适配器）
    ├── logConfig.py             #   LogConfig loguru 日志（适配改造）
    ├── getProject.py            #   GetProject 项目路径查找（适配改造）
    └── tools/                   #   基础工具（完整复用）
        ├── handlerDataType.py   #     数据类型转换工具（通用化改造）
        ├── handlerEncrypt.py    #     MD5 加密
        └── handlerTime.py       #     时间戳/时间字符串/日期字符串
```

### 7.2 适配改造

| 改造点 | 说明 |
|---|---|
| 环境配置数据源 | 从读取 `env.json` 文件改为从数据库 `Environment.config` JSON 字段读取，复用 `HandleEnv` 接口，替换底层实现 |
| 关键字存储 | 从 Python 字典文件（`KeyXxx = {...}`）改为数据库 `Keyword` 表，运行时根据 `keyword_type` 动态构建字典；保留 `HandlerKw.kw()` 和 `RunKw` 执行器逻辑 |
| 执行上下文 | 增加执行 ID 关联，将请求/响应日志和断言结果写入 `TestResult.logs` JSON 字段而非仅文件 |
| 项目路径 | 从 `GetProject.get_project_path()` 硬编码查找 `postman-tool` 目录改为从数据库 `Project.source_path` 读取 |
| 日志输出 | 从 `logConfig.py` 的文件日志改为结构化日志写入数据库，同时保留控制台日志用于调试 |
| Swagger 导入 | 复用 `ConvertApiDoc` 解析器，将生成结果写入数据库 `ApiEndpoint` 表而非生成 Python 文件 |
| AST 关键字提取 | 复用 `ConvertKeyKw._parse_source()` 解析逻辑，将结果写入 `Keyword` 表而非生成 `key_*.py` 文件 |
| 鉴权机制 | 支持多种鉴权模式（Bearer Token、动态密码、API Key 等），根据环境配置自动选择，支持通过插件扩展新的鉴权方式 |
| WebSocket 模式 | 抽象为通用 WebSocket 协议适配器，支持可配置的认证方式和消息路由模式 |

### 7.3 执行流程（微服务架构适配后）

```
Web 触发执行
  → 后端创建 TestExecution 记录
  → 发布执行消息到 RabbitMQ
  → 消费者接收消息，启动异步执行任务
  → 从数据库获取环境配置（Environment.config JSON）→ 注入引擎
  → 从数据库加载测试用例 → 解析 steps JSON
  → 从数据库获取关键字详情
  → 按顺序执行每个 step：
    → 解析 Action 关键字节点树（流程画布数据）
    → 按流程画布中节点连线的拓扑顺序执行：
      - 串行节点依次执行，并行节点（扇出连线）同时执行
      - 逻辑判断节点根据条件表达式选择「是」/「否」分支执行
      - 根据节点引用处理：
      - keyword 节点：从 Keyword 查找，根据 keyword_type 自动执行：
        · API → 查找 ApiEndpoint + 测试数据 → 调用内置 HTTP 客户端执行请求
        · TOOL → 查找 ToolMethod + args 传参 → 调用内置 Groovy 沙箱执行
        · ACTION → 递归解析子 Action 节点树执行
        · TEST_CASE → 递归解析子用例步骤树执行
      - tool_method 节点：直接引用 ToolMethod + args 传参 → 调用内置沙箱执行
    → 捕获请求/响应日志 → 写入 TestResult.logs JSON
    → 若步骤配置了 assertions 校验 → HandlerAssert 逐条断言校验 → 记录通过/失败
    → 若步骤未配置校验 → 跳过断言，仅记录执行结果
  → 全部执行完成 → 更新 TestExecution 统计
  → WebSocket 推送完成通知
```

### 7.4 引擎依赖关系

```
auto-test-platform 后端服务 (Java / Spring Boot)
  ├── controller/  → REST API 层，接收前端请求
  ├── service/     → 业务逻辑层（M1~M10 所有模块）
  │     └── 模块间直接通过 Spring Bean 调用，无网络开销
  ├── engine/      → 执行引擎核心包
  │     ├── KeywordExecutor    → 关键字执行器（统一入口）
  │     ├── HttpClientEngine   → HTTP 客户端（OkHttp）
  │     ├── AssertionEngine    → 断言引擎
  │     ├── SandboxEngine      → 工具方法沙箱（Groovy）
  │     ├── SwaggerParserEngine → Swagger 解析器
  │     └── ActionExecutor     → Action 流程执行器
  ├── mq/          → RabbitMQ 消息生产/消费
  ├── websocket/   → WebSocket 实时推送
  └── mapper/      → 数据库访问层（MyBatis-Plus）
        └── MySQL 单实例，各模块操作各自表
```

---

## 8. 非功能需求

### 8.1 性能要求

| 指标 | 要求 |
|---|---|
| 页面首屏加载 | < 2s |
| API 响应时间 | 普通接口 < 200ms，列表查询 < 500ms |
| 并发用户 | 支持 50+ 并发用户 |
| 测试执行 | 单用例执行超时默认 30s，可配置 |
| 数据存储 | 支持百万级执行记录，利用 MySQL 分区表 + 自动归档 > 90 天数据 |

### 8.2 安全要求

| 要求 | 说明 |
|---|---|
| 认证 | JWT Token，Access Token 有效期 2h，Refresh Token 有效期 7d |
| 密码 | bcrypt 加密存储，最少 8 位 |
| API 权限 | RBAC：ADMIN 全权限，USER 测试操作权限（不可管理用户和系统配置） |
| admin 账号保护 | 账号为 `admin` 的系统内置管理员不可修改用户名、不可分配角色、不可禁用、不可删除，仅允许重置密码 |
| 敏感数据 | 环境配置中的 Token、密码在 API 响应中脱敏显示 |
| 仓库凭证加密 | 测试代码库的认证密码/Token 使用 AES-128/CBC 加密存储（`enc:` 前缀），API 响应不回传明文 |
| CORS | Nginx + Gateway 双重 CORS 配置，仅允许前端域名跨域 |
| SQL 注入 | 使用 MyBatis-Plus 参数化查询（MySQL 预处理语句） |
| XSS | 前端输入输出转义 |

### 8.3 可用性

- 响应式布局，支持 1280px 以上分辨率
- 关键操作有确认提示（删除、批量操作）
- 表单校验即时反馈
- 加载状态明确（骨架屏 / Loading 指示器）
- 操作成功/失败消息提示
- 表格支持列排序、列宽调整

---

## 9. 项目目录结构

```
auto-test-platform/
├── frontend/                       # 前端项目（Vue 3 SPA）
│   ├── src/
│   │   ├── api/                    #   API 接口定义
│   │   ├── assets/                 #   静态资源
│   │   ├── components/             #   通用组件
│   │   ├── composables/            #   Composition API hooks
│   │   ├── layouts/                #   布局组件
│   │   ├── router/                 #   路由配置
│   │   ├── stores/                 #   Pinia 状态管理
│   │   ├── views/                  #   页面视图
│   │   │   ├── login/
│   │   │   ├── project/
│   │   │   ├── environment/
│   │   │   ├── repository/
│   │   │   ├── api/
│   │   │   ├── api-keyword/
│   │   │   ├── tool/
│   │   │   ├── action/
│   │   │   ├── suite/
│   │   │   ├── case/
│   │   │   ├── plan/
│   │   │   ├── execution/
│   │   │   ├── analytics/
│   │   │   └── settings/
│   │   ├── utils/
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── Dockerfile
│
├── backend/                        # 后端服务（Spring Boot 单体应用）
│   ├── src/main/java/com/postman/platform/
│   │   ├── PostmanPlatformApplication.java  # Spring Boot 启动类
│   │   ├── common/                 #   公共模块
│   │   │   ├── response/           #     ApiResponse 统一响应格式
│   │   │   ├── exception/          #     全局异常处理
│   │   │   ├── config/             #     公共配置（Redis, MyBatis-Plus, RabbitMQ）
│   │   │   └── util/               #     工具类
│   │   ├── auth/                   #   认证模块（M1）
│   │   │   ├── controller/         #     AuthController, UserController, SettingsController
│   │   │   ├── service/            #     AuthService, UserService, SettingsService
│   │   │   ├── mapper/             #     UserMapper
│   │   │   ├── entity/             #     User
│   │   │   ├── dto/                #     请求/响应 DTO
│   │   │   ├── security/           #     JwtTokenProvider, JwtAuthenticationFilter
│   │   │   └── config/             #     SecurityConfig, WebMvcConfig
│   │   ├── project/                #   项目管理模块（M2）
│   │   │   ├── controller/         #     ProjectController, EnvironmentController
│   │   │   ├── service/            #     ProjectService, EnvironmentService
│   │   │   ├── mapper/             #     ProjectMapper, EnvironmentMapper
│   │   │   ├── entity/             #     Project, Environment
│   │   │   └── dto/
│   │   ├── api/                    #   接口管理模块（M3/M4）
│   │   │   ├── controller/         #     ApiController, SwaggerController
│   │   │   ├── service/            #     ApiService, SwaggerImportService
│   │   │   ├── mapper/             #     ApiModuleMapper, ApiEndpointMapper
│   │   │   ├── entity/             #     ApiModule, ApiEndpoint
│   │   │   └── dto/
│   │   ├── keyword/                #   关键字管理模块（M5/M6/M7）
│   │   │   ├── controller/         #     KeywordController, ToolController, ActionController
│   │   │   ├── service/            #     KeywordService, ToolService, ActionService
│   │   │   ├── mapper/             #     KeywordMapper, ToolMethodMapper, ActionMapper
│   │   │   ├── entity/             #     Keyword, ApiKeyword, ToolMethod, Action
│   │   │   └── dto/
│   │   ├── execution/              #   执行与报告模块（M8/M9/M10）
│   │   │   ├── controller/         #     SuiteController, CaseController, PlanController, ExecutionController, AnalyticsController
│   │   │   ├── service/            #     SuiteService, CaseService, ExecutionService, AnalyticsService, ReportService
│   │   │   ├── mapper/             #     SuiteMapper, CaseMapper, ExecutionMapper, ResultMapper
│   │   │   ├── entity/             #     TestSuite, TestCase, TestPlan, TestExecution, TestResult
│   │   │   ├── dto/
│   │   │   ├── engine/             #     内置执行引擎（Java 实现）
│   │   │   ├── mq/                 #     ExecutionMessageProducer, ExecutionMessageConsumer
│   │   │   ├── websocket/          #     ExecutionWebSocket
│   │   │   └── config/             #     RabbitMQConfig, AsyncConfig, WebSocketConfig
│   │   ├── repository/              #   测试代码库模块（M11）
│   │   │   ├── controller/         #     CodeRepositoryController
│   │   │   ├── service/            #     CodeRepositoryService（JGit 克隆/拉取）
│   │   │   ├── mapper/             #     CodeRepositoryMapper, CodeRepositoryPullLogMapper
│   │   │   ├── entity/             #     CodeRepository, CodeRepositoryPullLog
│   │   │   └── dto/
│   │   └── filter/                 #   全局过滤器（JWT 鉴权、CORS）
│   │       ├── JwtAuthenticationFilter.java
│   │       └── CorsFilter.java
│   ├── src/main/resources/
│   │   ├── application.yml         #   应用配置
│   │   ├── application-dev.yml     #   开发环境配置
│   │   ├── application-prod.yml    #   生产环境配置
│   │   └── db/migration/           #   Flyway 迁移脚本
│   ├── pom.xml                     #   Maven POM
│   └── Dockerfile
│
├── pom.xml                         # Maven POM（后端父工程）
├── docker-compose.yml              # Docker Compose 编排
├── docker-compose.prod.yml         # 生产环境配置
├── nginx/
│   └── nginx.conf                  # Nginx 配置
└── scripts/
    └── init_data.sh                # 数据库初始化脚本
```

---

## 10. 开发里程碑

### Phase 0：项目基础设施（1 周）

| 任务 | 工期 | 交付物 |
|---|---|---|
| Spring Boot 单体项目搭建（Maven 工程 + 模块包结构） | 2 天 | 可编译运行的后端工程 |
| JWT 认证 + Spring Security 配置 + 全局过滤器 | 1 天 | 认证鉴权框架 |
| RabbitMQ + Spring AMQP 集成 | 1 天 | 异步消息通道 |
| 数据库连接 + MyBatis-Plus + Redis 配置 | 1 天 | 数据访问层基础 |

### Phase 1：基础框架 & 核心功能（4 周）

| 任务 | 工期 | 交付物 |
|---|---|---|
| 项目脚手架搭建（前端 + 后端 + Docker） | 3 天 | 可运行的空项目框架 |
| 数据库模型设计 + Flyway 迁移 | 2 天 | 完整数据库 Schema |
| 用户认证（登录/JWT/用户管理） | 2 天 | 登录流程打通 |
| 项目管理 CRUD | 2 天 | 项目列表 + 详情 |
| 环境配置管理 + 配置编辑器 | 3 天 | 环境 CRUD + Monaco 编辑器 |
| 核心执行引擎（Java 内置实现） | 3 天 | 引擎模块（HTTP 客户端 + Groovy 沙箱） |
| 接口管理（Swagger 导入 + CRUD + 调试） | 5 天 | 接口管理完整功能 |
| 接口关键字管理（创建 + CRUD + 测试数据 + 删除保护） | 4 天 | 接口关键字完整功能 |
| 工具方法管理（内置 + 自定义 + 沙箱 + 在线测试） | 4 天 | 工具方法完整功能 |
| Action 关键字管理（CRUD + 节点编排器 + 调试） | 5 天 | Action 关键字完整功能 |

### Phase 2：用例编排 & 执行引擎（3 周）

| 任务 | 工期 | 交付物 |
|---|---|---|
| 测试套件管理 | 1 天 | 套件 CRUD |
| 用例列表 + 筛选 | 2 天 | 用例列表页 |
| 用例编辑器（Action 调用 + 逻辑控制 + 拖拽） | 7 天 | 核心用例编排器 |
| 参数化 & 数据驱动 | 2 天 | 多组数据配置 |
| RabbitMQ 异步执行集成 | 2 天 | 异步执行框架 |
| 测试执行引擎（手动触发 + 内置引擎调用） | 3 天 | 端到端执行打通 |
| 实时日志推送（WebSocket） | 2 天 | 执行过程实时展示 |

### Phase 3：调度 & 报告分析（3 周）

| 任务 | 工期 | 交付物 |
|---|---|---|
| 测试计划 + XXL-Job 定时任务 | 3 天 | 计划管理 + 定时调度 |
| 执行记录列表 + 详情 | 3 天 | 执行历史完整展示 |
| 报告分析（趋势 + 对比） | 4 天 | ECharts 图表页 |
| 报告导出（PDF + Excel） | 3 天 | 导出功能 |
| 项目仪表盘 | 2 天 | 概览仪表盘 |
| 系统设置页 | 1 天 | 全局配置 |

### Phase 4：优化 & 部署（2 周）

| 任务 | 工期 | 交付物 |
|---|---|---|
| UI 细节打磨 + 交互优化 | 3 天 | 专业级 UI 体验 |
| 后端性能优化（缓存、连接池、异步处理） | 2 天 | 性能达标 |
| Docker Compose 生产配置 | 2 天 | 一键部署 |
| K8s 部署配置 + HPA 弹性伸缩 | 1 天 | 生产级部署方案 |
| 集成测试 + Bug 修复 | 4 天 | 稳定可交付版本 |

**总计：约 13 周**

---

## 11. 术语表

| 术语 | 说明 |
|---|---|
| 关键字 (Keyword) | 统一关键字实体，通过 keyword_type 区分不同类型，所有关键字统一支持传参和返回 |
| keyword_type | 关键字来源类型枚举：API / TOOL / ACTION / TEST_CASE，支持扩展新类型 |
| 接口关键字 (API Keyword) | Keyword(type=API) + ApiEndpoint，接口 + 测试数据的封装 |
| 工具方法关键字 (Tool Keyword) | ToolMethod 实体的 `keyword` 字段（≤20字符），用于 Action 节点和用例步骤直接引用，无需独立 Keyword 实体 |
| Action 关键字 (Action Keyword) | Keyword(type=ACTION) + Action，关键字组合 + 逻辑控制的封装，形成可复用的业务动作单元 |
| 测试用例关键字 (TestCase Keyword) | Keyword(type=TEST_CASE) + TestCase，测试用例作为关键字复用 |
| 接口 (ApiEndpoint) | 被测系统的 HTTP REST 接口定义，由 Swagger 导入或手动创建 |
| 测试代码库 (Code Repository) | 项目登记的 Git 仓库及拉取到服务器本地的代码副本，基于 JGit 克隆/增量更新 |
| 关键字字典 | 关键字名称到函数引用的映射字典，运行时用于查找执行 |
| 测试套件 (Test Suite) | 一组相关测试用例的集合 |
| 测试用例 (Test Case) | 由多个步骤组成的完整测试场景，也可作为关键字被其他用例引用。包含测试步骤和可选的校验（断言） |
| 测试步骤 (Test Step) | 用例中的单个操作，调用关键字或控制流节点，支持逻辑控制和变量传递。关键字步骤可选配置校验（断言） |
| 工具方法 (ToolMethod) | 用户自定义的辅助函数，用于数据转换、加密等，可封装为工具方法关键字在 Action 节点和用例步骤中调用 |
| 测试计划 (Test Plan) | 定义要执行的测试套件集合和执行策略 |
| 测试执行 (Test Execution) | 一次测试计划的运行实例 |
| KwDecorator | postman-tool 的方法装饰器，标记可转换为关键字的方法 |
| AST | Abstract Syntax Tree，用于解析源码提取关键字信息（引擎层 Python 实现） |
| Swagger 2.0 | REST API 文档规范，用于自动生成 API 客户端代码 |
| postman-engine | 从 postman-tool 抽取的核心引擎包，提供关键字解析、HTTP 通信、断言等通用能力 |
| 协议适配器 | 引擎扩展插件，用于对接不同的通信协议（WebSocket、配置中心、远程命令等），可按需接入 |
