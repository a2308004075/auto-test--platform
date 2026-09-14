# Flyway 迁移管理规范

## 核心原则

**已推送到远程仓库的迁移文件视为不可变（immutable）**。任何 schema 变更必须通过新增迁移文件实现，禁止修改已有文件。

**开发数据优先（data-first）**：开发过程中禁止重置开发数据库 `auto_test_platform`。任何表结构或数据变更一律通过新增 Flyway 迁移文件实现，由服务启动时的 Flyway 增量应用，已有业务数据必须保持完好。`backend/scripts/reset-and-build.bat` 仅在用户明确提出时才允许运行，AI 不得主动执行、也不得主动建议执行。

**迁移自动执行（auto-apply）**：AI 可自主创建并执行迁移，表结构 DDL 与数据修正 UPDATE/INSERT 均无需用户逐条审批，涉及的数据修正由 AI 主动完成。唯一硬红线：**禁止删除数据**，任何丢失数据的操作必须先获用户批准。详见 2.1 自动执行流程。

---

## 1. 迁移文件命名

```
V{版本号}__{描述}.sql
```

- 版本号：纯数字，严格递增，不留间隔（如 V19, V20, V21...）
- 描述：小写字母 + 下划线，简要说明本次变更（如 `add_user_avatar_column`）
- 示例：`V19__add_user_avatar_column.sql`

## 2. 开发阶段工作流

### 2.1 迁移自动执行流程（所有结构/数据变更）

AI 自主完成迁移的创建与执行，无需用户逐条审批 SQL：

1. **编写**：创建 `V{n}__xxx.sql`，包含表结构变更及配套数据修正（新增列带默认值、旧数据格式清洗、存量数据回填等），保证已有数据兼容
2. **验证**：运行 `backend/scripts/verify-migrations.bat`，确认迁移链在临时库从零可完整跑通
3. **执行**：直接在开发库执行该迁移文件中的同一 SQL（涉数据改写时先备份，见下方红线），随后向 `flyway_schema_history` 登记该版本记录（installed_rank 取当前最大值 +1，type 为 SQL，success 为 1）；checksum 由 dev 环境启动时的 `FlywayRepairPostProcessor` 自动对齐，服务重启不会重复执行
4. **报告**：在对话中说明本次执行的迁移文件、涉及的表、结构变更与数据处理方式

多处变更汇总为一个迁移一次执行，执行后在对话中统一报告。

红线（自动执行也不得违反）：

- **禁止删除数据**：不得自主执行 DELETE、TRUNCATE、DROP TABLE、DROP COLUMN 等任何导致数据丢失的操作；确有需要（如废弃表/列）时，必须先向用户说明并获明确批准
- **改写数据前备份**：涉及 UPDATE 改写存量数据的迁移，先用 mysqldump 备份受影响的表至 `backend/scripts/backup/`（文件名含时间戳），再执行
- 重命名表/列、修改列类型（值保留）等不丢失数据的结构操作可自主执行

### 2.2 日常开发（本地未推送）

当你在编写**尚未 push 的新迁移文件**时：

- 可以自由修改、删除、重编号
- 修改已在本地执行过的迁移文件后：checksum 差异由 dev 环境的 `FlywayRepairPostProcessor` 在启动时自动同步；但本地库结构仍是旧内容执行的结果，如需让改动在本地生效，由 AI 直接执行对应的补偿 SQL（或按 2.1 流程重新执行差异部分），或将改动拆为新的更高版本迁移
- 验证修改后的迁移链：运行 `backend/scripts/verify-migrations.bat`（在临时库从零执行全部迁移，不影响开发库）
- 这是唯一允许修改迁移文件的阶段

### 2.3 推送前检查（push 前）

推送代码前必须执行：

```bash
# 在临时库 auto_test_platform_verify 上从零执行全部迁移，
# 验证迁移链完整可用；开发库与其数据不受任何影响
backend/scripts/verify-migrations.bat

# 确认输出 [PASS] 后再 push
```

### 2.4 推送后（已 push 的迁移）

一旦迁移文件被 push 到远程仓库：

- **禁止修改**：不得编辑文件内容、不得重命名、不得删除
- **只能新增**：所有 schema 变更通过创建更高版本号的迁移文件实现
- **修复 bug**：如果已推送的迁移有 bug，创建新迁移来修复，而非修改原文件

## 3. 多人协作规则

下表所有场景均按 2.1 自动执行流程处理：AI 自主创建迁移并执行，无需用户逐条审批；但任何丢失数据的操作仍须先获用户批准。

| 场景 | 正确做法 | 禁止做法 |
|------|---------|---------|
| 需要新表 | 创建 V{n}__create_xxx_table.sql | 修改 V2 添加建表语句 |
| 迁移有 bug | 创建 V{n}__fix_xxx.sql 修正数据/结构 | 直接修改有 bug 的旧文件 |
| 需要修改列类型 | 创建 V{n}__alter_xxx_column.sql | 修改原始建表迁移 |
| 本地启动失败（checksum 不匹配） | 重启服务，dev 环境自动 repair 同步 checksum | 运行 `reset-and-build.bat` 重置、手动删除 flyway_schema_history 记录 |
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

## 5. 数据库操作红线（数据优先）

**开发过程中禁止重置开发数据库**。`backend/scripts/reset-and-build.bat`（DROP + 重建 + 导入备份）仅当用户明确提出时才允许运行，AI 不得主动执行，也不得主动建议执行。

**禁止删除数据**。除 2.1 红线所述（DELETE / TRUNCATE / DROP TABLE / DROP COLUMN 须用户批准）外，AI 也不得以任何其他方式清空或丢弃开发库数据。

历史触发场景的现行替代做法：

| 历史场景 | 现行正确做法 |
|---------|------------|
| 拉取代码后本地启动报 checksum 不匹配 | 重启服务，dev 环境 `FlywayRepairPostProcessor` 自动同步 checksum |
| 修改了未推送的迁移文件需要重新验证 | 运行 `backend/scripts/verify-migrations.bat`（临时库验证，不碰开发库） |
| 数据库状态与迁移历史不一致 | 编写新的迁移文件对齐结构/数据；无法通过迁移修复时，先向用户说明情况并确认后续方案 |
| 需要验证迁移链能否从零跑通 | 运行 `backend/scripts/verify-migrations.bat` |

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

**注意**：此机制是「修改未推送迁移文件后本地继续开发」的依赖手段，但不能用来绕过迁移不可变规则——已推送的迁移文件仍然禁止修改；且 repair 只同步 checksum，不会把迁移文件的新改动应用到本地库。

## 8. 发布前冻结

发布新版本前：

1. 确认所有迁移文件内容正确且已 push
2. 运行 `backend/scripts/verify-migrations.bat`，在临时库上完成一次从零的完整迁移验证
3. 发布后，所有已包含在发布版本中的迁移文件**永久冻结**
