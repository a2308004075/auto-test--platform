/**
 * @author HXN
 * @date 2026-08-30
 * @description 测试代码库模块 API
 */
import request from './request'

/**
 * 测试代码库模块 API
 */

export function getRepositories(projectId: number) {
  return request.get(`/v1/projects/${projectId}/repositories`)
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
