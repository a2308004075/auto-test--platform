# 需求变更文档同步规则

## 触发条件

当项目需求发生变更时（包括用户口头描述、功能调整、需求迭代等），必须主动检查 `docs/` 目录下所有相关文档是否需要同步更新，并在需要时立即执行更新，确保文档间的一致性。

## docs/ 目录文档清单

- `docs/SRS.md` — 软件需求规格说明书（需求源头）
- `docs/PRD.md` — 产品需求文档（含接口定义、界面描述、流程图引用）
- `docs/ui/` — 28 个 HTML 原型页面，按模块分布：
  - `ui/auth/` (login)
  - `ui/project/` (dashboard, list, env-config)
  - `ui/api/` (list, edit, debug, swagger-import)
  - `ui/cases/` (case-list, case-edit, suite-list)
  - `ui/execution/` (plan-list, plan-edit, execution-list, execution-detail)
  - `ui/keywords/` (keyword-list/edit/create, action-list/debug/editor, tool-list/create)
  - `ui/analytics/` (trend-analysis, history-compare)
  - `ui/environment/` (environment-config)
  - `ui/settings/` (global-config, user-management, profile)
  - `ui/common/` (nav.js, shared-styles.css 公共组件)

## 执行流程

1. 需求变更发生时，先识别变更影响的模块和功能点
2. 检查 `SRS.md` 是否需要更新（需求规格层面）
3. 检查 `PRD.md` 是否需要更新（产品需求、接口、界面描述层面）
4. 检查 `docs/ui/` 下对应的 HTML 原型页面是否需要更新（界面交互层面）
5. 若涉及公共组件变更，同步更新 `ui/common/` 下的 `nav.js` 和 `shared-styles.css`
6. 更新完成后，验证各文档间的交叉引用仍然有效
