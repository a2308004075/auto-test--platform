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

export function updateRepository(projectId: number, repoId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/repositories/${repoId}`, data)
}

export function deleteRepository(projectId: number, repoId: number) {
  return request.post(`/v1/projects/${projectId}/repositories/${repoId}/delete`)
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
