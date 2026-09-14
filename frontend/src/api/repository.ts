/**
 * @author HXN
 * @date 2026-08-30
 * @description 测试代码库模块 API
 */
import request from './request'

/**
 * 测试代码库模块 API
 */

/**
 * 查询项目下仓库列表（groupId 不传=全部；正数=指定分组含子孙分组）
 */
export function getRepositories(projectId: number, groupId?: number) {
  return request.get(`/v1/projects/${projectId}/repositories`, {
    params: groupId ? { groupId } : undefined,
  })
}

/**
 * 批量删除仓库
 */
export function batchDeleteRepositories(projectId: number, repoIds: number[]) {
  return request.post(`/v1/projects/${projectId}/repositories/batch-delete`, repoIds)
}

/**
 * 批量移动仓库到指定分组
 */
export function batchMoveRepositories(projectId: number, targetGroupId: number, repoIds: number[]) {
  return request.post(`/v1/projects/${projectId}/repositories/batch-move`, repoIds, {
    params: { targetGroupId },
  })
}

// ===== 仓库分组 =====

export function getRepositoryGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/repository-groups`)
}

export function createRepositoryGroup(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/repository-groups`, data)
}

export function updateRepositoryGroup(projectId: number, groupId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/repository-groups/${groupId}`, data)
}

export function deleteRepositoryGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/repository-groups/${groupId}/delete`)
}

export function createRepository(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/repositories`, data)
}

/**
 * 获取远程仓库分支列表（lsRemote 查询，不克隆代码），供新建/编辑时选择分支
 */
export function getRepositoryBranches(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/repositories/branches`, data, {
    timeout: 60000,
  })
}

export function updateRepository(projectId: number, repoId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/repositories/${repoId}`, data)
}

export function deleteRepository(projectId: number, repoId: number) {
  return request.post(`/v1/projects/${projectId}/repositories/${repoId}/delete`)
}

/**
 * 复制仓库（一步生成副本，名称自动追加「（副本）」后缀，重名时追加序号）
 */
export function copyRepository(projectId: number, repoId: number) {
  return request.post(`/v1/projects/${projectId}/repositories/${repoId}/copy`)
}

/**
 * 拉取仓库代码（克隆/增量更新），大仓库耗时较长，单独放宽超时时间
 */
export function pullRepository(projectId: number, repoId: number) {
  return request.post(`/v1/projects/${projectId}/repositories/${repoId}/pull`, undefined, {
    timeout: 600000,
  })
}

export function getPullLogs(projectId: number, repoId: number) {
  return request.get(`/v1/projects/${projectId}/repositories/${repoId}/pull-logs`)
}
