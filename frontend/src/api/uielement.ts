/**
 * @author HXN
 * @date 2026-08-30
 * @description 界面元素模块 API
 */
import request from './request'

/**
 * 界面元素模块 API
 */

/** 获取界面元素文件树（仓库 → 目录 → 文件，文件节点带元素数） */
export function getUiElementFiles(projectId: number) {
  return request.get(`/v1/projects/${projectId}/ui-elements/files`)
}

/** 获取指定文件的界面元素列表 */
export function getUiElements(projectId: number, repositoryId: number, filePath: string) {
  return request.get(`/v1/projects/${projectId}/ui-elements`, {
    params: { repositoryId, filePath },
  })
}

/**
 * 导入界面元素（从已拉取仓库解析前端源码，覆盖式重建）
 * 大仓库解析耗时较长，单独放宽超时时间
 */
export function importUiElements(projectId: number, repositoryId: number) {
  return request.post(`/v1/projects/${projectId}/ui-elements/import`, { repositoryId }, {
    timeout: 300000,
  })
}

/** 删除指定文件的界面元素 */
export function deleteUiElementFile(projectId: number, repositoryId: number, filePath: string) {
  return request.post(`/v1/projects/${projectId}/ui-elements/file/delete`, { repositoryId, filePath })
}

/** 删除仓库的全部界面元素 */
export function deleteUiElementRepository(projectId: number, repositoryId: number) {
  return request.post(`/v1/projects/${projectId}/ui-elements/repository/${repositoryId}/delete`)
}
