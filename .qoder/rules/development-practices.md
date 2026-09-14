# 开发实践规范

## 1. 回复语言

所有回复必须使用中文。

## 2. UI 设计质量要求

所有 UI mockups、原型及 PRD 中的界面文字描述，必须共同遵循以下质量标准：

- 视觉风格需专业、美观，杜绝"AI 生成感"——体现为干净的布局、一致的字体、恰当的间距、真实的示例数据和人工打磨的设计语言
- 所有 UI 效果与 PRD 文档中的文字描述必须严格一致，确保需求传达零偏差
- 当用户明确要求"先用文字描述"时，必须优先输出结构化文本（如线框式布局说明、完整 REST API 表格），而非视觉原型

## 3. 前端界面描述全覆盖

前端界面文字描述需覆盖 PRD §11.2 列出的所有 28 个核心页面，不得遗漏或仅选择性描述部分页面。PRD §11.2 是 28 个核心 UI 页面的权威来源。

## 4. PRD 必须包含原型图和流程图

PRD 文档必须在文字需求的同时包含并正式记录原型图（prototype diagrams）和流程图（flowcharts）；这些视觉资产是需求规格的重要组成部分，必须在相关章节（如操作步骤、协议交互、决策逻辑）中引用。

## 5. API 协议范围

API 协议指 postman-platform 项目自身的前、后端 REST API 交互协议，不包括该平台所测试的外部系统 API。

## 6. 代码开发规范

项目后端 Java 代码开发遵循《阿里巴巴 Java 开发手册（泰山版）》作为基线规范，重点落实以下条目：

### 6.1 命名规约
- 类名使用 `UpperCamelCase`；方法名、变量名使用 `lowerCamelCase`；常量名全大写并用下划线分隔
- 包名全小写、连续单词连写；抽象类以 `Abstract` 或 `Base` 开头，异常类以 `Exception` 结尾，测试类以被测类名 + `Test` 结尾
- DTO / VO / BO 等后缀按层级语义使用，避免 `util` / `helper` 等模糊命名

### 6.2 常量与魔法值
- 不允许出现未解释的魔法值（硬编码字面量），需抽取为常量并集中定义
- long 型字面量使用大写 `L`（如 `1L`），不使用小写 `l`

### 6.3 OOP 规约
- 对象比较使用 `equals()` 时，常量或已知非空对象放在左侧（`"abc".equals(str)`）
- POJO 类属性不使用基本数据类型，统一使用包装类型
- 构造方法禁止业务逻辑；`toString()` 方法包含全部字段；拆分 / 合并 POJO 需同时更新

### 6.4 集合处理
- 集合返回值不允许返回 `null`，使用 `Collections.emptyList()` 等空集合
- `HashMap` 初始化时指定初始容量，避免频繁扩容
- 遍历集合时使用 `forEach` / iterator，避免在 `foreach` 循环中直接 `remove` 元素（使用 `Iterator.remove()`）

### 6.5 并发处理
- 线程池不使用 `Executors` 直接创建，使用 `ThreadPoolExecutor` 显式设定核心参数
- `SimpleDateFormat` 线程不安全，使用 `DateTimeFormatter` 或线程局部变量

### 6.6 控制语句
- `if / else / for / while / do` 语句必须使用大括号 `{}`，即使只有一行代码
- 避免 `if` 多层嵌套超过 3 层，超出需重构

### 6.7 异常与日志
- 异常不捕获大范围 `Exception` / `Throwable`，按业务场景捕获具体异常类型
- 日志使用 SLF4J（`@Slf4j`），占位符使用 `{}`，禁止字符串拼接
- 异常信息需包含现场参数（两个参数以上并入 `args`）

### 6.8 MySQL 规约
- 表名、字段名使用小写 + 下划线；表名不使用复数
- 主键统一命名为 `id`；时间字段使用 `create_time` / `update_time`
- 禁止 `SELECT *`，明确列出字段
- `in` 子句控制在 1000 条以内；避免在 `where` 字段上做函数运算

### 6.9 工程结构
- 分层架构：`controller` / `service` / `mapper` / `entity` / `dto`，各层职责清晰，禁止跨层调用
- 接口与实现分离：Service 层定义接口 + `Impl` 实现类
- 通用工具类放置于 `common.util`，不允许业务层出现重复工具方法

## 7. Flyway 迁移管理

详细规范见 `flyway-migration.md`，核心要点：

- **已推送的迁移文件不可变**：所有 schema 变更通过新增迁移文件实现，禁止修改已有文件
- **命名规范**：`V{版本号}__{描述}.sql`，版本号严格递增
- **修复 bug**：创建新迁移 `V{n}__fix_xxx.sql`，不修改原文件
- **本地冲突处理**：运行 `scripts/reset-and-build.bat` 一键重置数据库 + 重建项目
- **团队成员**：clone 后运行 `scripts/install-git-hooks.bat` 安装 pre-commit hook
