# Flyway 迁移管理规范

## 核心原则

**已推送到远程仓库的迁移文件视为不可变（immutable）**。任何 schema 变更必须通过新增迁移文件实现，禁止修改已有文件。

---

## 1. 迁移文件命名

```
V{版本号}__{描述}.sql
```

- 版本号：纯数字，严格递增，不留间隔（如 V19, V20, V21...）
- 描述：小写字母 + 下划线，简要说明本次变更（如 `add_user_avatar_column`）
- 示例：`V19__add_user_avatar_column.sql`

## 2. 开发阶段工作流

### 2.1 日常开发（本地未推送）

当你在编写**尚未 push 的新迁移文件**时：

- 可以自由修改、删除、重编号
- 本地数据库可随时重置：运行 `scripts/reset-and-build.bat`
- 这是唯一允许修改迁移文件的阶段

### 2.2 推送前检查（push 前）

推送代码前必须执行：

```bash
# 1. 重置数据库，验证全部迁移从零开始可以通过
scripts/reset-and-build.bat

# 2. 确认服务启动成功、无 Flyway 报错后再 push
```

### 2.3 推送后（已 push 的迁移）

一旦迁移文件被 push 到远程仓库：

- **禁止修改**：不得编辑文件内容、不得重命名、不得删除
- **只能新增**：所有 schema 变更通过创建更高版本号的迁移文件实现
- **修复 bug**：如果已推送的迁移有 bug，创建新迁移来修复，而非修改原文件

## 3. 多人协作规则

| 场景 | 正确做法 | 禁止做法 |
|------|---------|---------|
| 需要新表 | 创建 V{n}__create_xxx_table.sql | 修改 V2 添加建表语句 |
| 迁移有 bug | 创建 V{n}__fix_xxx.sql 修正数据/结构 | 直接修改有 bug 的旧文件 |
| 需要修改列类型 | 创建 V{n}__alter_xxx_column.sql | 修改原始建表迁移 |
| 本地启动失败（checksum 不匹配） | 运行 `reset-and-build.bat` 重置 | 手动删除 flyway_schema_history 记录 |
| 需要插入新初始数据 | 创建 V{n}__add_xxx_data.sql | 修改 V3 或 V11 的 INSERT 语句 |

## 4. 修复已推送迁移的 bug 示例

假设 V11 插入的 JSON 值有问题：

```
❌ 错误：直接修改 V11__add_notification_settings.sql
✅ 正确：创建 V19__fix_notification_settings_values.sql
```

V19 内容示例：
```sql
-- V19 修复 V11 中 notification 配置项的值格式
UPDATE `global_settings`
SET `config_value` = 'tls'
WHERE `config_key` = 'notification.smtp.encryption';
```

## 5. 重置数据库操作

当需要重置本地数据库时（以下场景任一触发）：

- 拉取代码后本地启动报 checksum 不匹配
- 修改了未推送的迁移文件需要重新验证
- 数据库状态与迁移历史不一致

**操作方式**：运行 `backend/scripts/reset-and-build.bat`

该脚本会：
1. 重置 MySQL 数据库（DROP + CREATE）
2. 执行 `mvn clean install -DskipTests`（清理并重建所有模块）
3. 输出操作结果

## 6. Pre-commit Hook 自动检查

项目已配置 Git pre-commit hook（`backend/scripts/hooks/pre-commit`），团队成员 clone 后运行 `backend/scripts/install-git-hooks.bat` 安装。Hook 提供三层检查：

### 6.1 迁移文件修改检测（阻断）

检测暂存区中是否有已存在的迁移文件被修改（`--diff-filter=M`）。如果发现修改，会显示警告并要求确认。如果文件已存在于远程分支，会额外提示风险。

### 6.2 实体类变更缺迁移检测（提醒）

检测 `platform-data` 模块中的实体类（`*/entity/*.java`）是否被修改/新增/删除，但暂存区中没有新增的迁移文件（`V*__*.sql`）。如果触发，会提示开发者确认是否需要创建迁移文件。

**需要迁移的场景**：
- 实体类新增/删除字段 → 对应 `ADD COLUMN` / `DROP COLUMN`
- 修改字段类型 → 对应 `MODIFY COLUMN`
- 新建实体类（映射新表）→ 对应 `CREATE TABLE`
- 新增表注解、索引等

**不需要迁移的场景**：
- 添加 `@TableField(exist = false)` 非数据库字段
- 修改 Java 属性名但不改 `@TableField` 列名
- 添加 transient 字段
- 纯逻辑方法变更

### 6.3 新增迁移提示（信息）

当检测到新增迁移文件时，显示文件列表供开发者确认。

## 7. FlywayRepairPostProcessor

项目已配置 `FlywayRepairPostProcessor`（仅 dev 环境生效），它会在启动时自动执行 `flyway.repair()` 同步 checksum。

**注意**：此机制仅作为安全网，不应依赖它来绕过迁移不可变规则。如果频繁触发 repair，说明流程存在问题。

## 8. 发布前冻结

发布新版本前：

1. 确认所有迁移文件内容正确且已 push
2. 在干净的数据库上执行一次完整迁移验证
3. 发布后，所有已包含在发布版本中的迁移文件**永久冻结**
