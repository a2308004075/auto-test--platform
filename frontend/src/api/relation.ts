/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联模块 API
 */
import request from './request'

// ===== 需求-用例关联 API =====

/** 查询需求条目下关联的用例列表 */
export function getRequirementCaseRelations(itemId: number) {
  return request.get(`/v1/requirement-items/${itemId}/case-relations`)
}

/** 添加需求条目与用例的关联 */
export function addRequirementCaseRelation(itemId: number, data: { caseType: string; caseId: number }) {
  return request.post(`/v1/requirement-items/${itemId}/case-relations`, data)
}

/** 删除需求-用例关联 */
export function deleteRequirementCaseRelation(relationId: number) {
  return request.post(`/v1/requirement-case-relations/${relationId}/delete`)
}

/** 查询用例关联的需求条目列表（反查：用例视角） */
export function getCaseRequirementRelations(projectId: number, caseType: string, caseId: number) {
  return request.get(`/v1/projects/${projectId}/case-requirement-relations`, { params: { caseType, caseId } })
}

/** 按目标反查缺陷关联（用例视角：该用例被哪些缺陷关联） */
export function getDefectRelationsByTarget(projectId: number, targetType: string, targetId: number) {
  return request.get(`/v1/projects/${projectId}/defects/by-target`, { params: { targetType, targetId } })
}
