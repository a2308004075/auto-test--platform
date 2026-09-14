/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论模块 API
 */
import request from './request'

export interface CommentItem {
  id: number
  bizType: string
  bizId: number
  content: string
  parentId: number | null
  createdBy: number | null
  createdByName: string | null
  createdAt: string
  children?: CommentItem[]
}

export interface CommentCreateRequest {
  bizType: string
  bizId: number
  content: string
  parentId?: number | null
}

/** 查询业务对象下的评论列表 */
export function getComments(bizType: string, bizId: number) {
  return request.get('/v1/comments', { params: { bizType, bizId } })
}

/** 发表评论/回复 */
export function createComment(data: CommentCreateRequest) {
  return request.post('/v1/comments', data)
}

/** 删除评论 */
export function deleteComment(commentId: number) {
  return request.post(`/v1/comments/${commentId}/delete`)
}
