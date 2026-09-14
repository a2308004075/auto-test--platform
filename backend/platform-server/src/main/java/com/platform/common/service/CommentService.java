/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论服务
 */
package com.platform.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.auth.mapper.UserMapper;
import com.platform.common.dto.CommentCreateRequest;
import com.platform.common.dto.CommentResponse;
import com.platform.common.entity.Comment;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论服务
 *
 * <p>支持按业务类型 + 业务对象 ID 进行通用评论管理，包括发表评论、回复评论、删除评论和查询评论列表。
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    /**
     * 查询业务对象下的评论列表（按创建时间升序，一级评论在前，子评论嵌套在父评论下）
     */
    public List<CommentResponse> listByBiz(String bizType, Long bizId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBizType, bizType)
                .eq(Comment::getBizId, bizId)
                .orderByAsc(Comment::getCreatedAt);
        List<Comment> comments = commentMapper.selectList(wrapper);

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, CommentResponse> responseMap = new HashMap<>();
        List<CommentResponse> topList = new ArrayList<>();

        for (Comment c : comments) {
            CommentResponse resp = toResponse(c, userCache);
            responseMap.put(c.getId(), resp);
            if (c.getParentId() == null) {
                topList.add(resp);
            } else {
                CommentResponse parent = responseMap.get(c.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(resp);
                }
            }
        }
        return topList;
    }

    /**
     * 发表评论（parentId 为空表示一级评论，否则为回复）
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentResponse create(CommentCreateRequest request) {
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "父评论不存在");
            }
            if (!request.getBizType().equals(parent.getBizType()) || !request.getBizId().equals(parent.getBizId())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "父评论与当前业务对象不匹配");
            }
        }

        Comment comment = new Comment();
        comment.setBizType(request.getBizType());
        comment.setBizId(request.getBizId());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setCreatedBy(getCurrentUserId());
        commentMapper.insert(comment);

        Map<Long, User> userCache = new HashMap<>();
        return toResponse(comment, userCache);
    }

    /**
     * 删除评论（仅评论作者或 superAdmin 可删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "评论不存在");
        }
        Long currentUserId = getCurrentUserId();
        if (!isSuperAdmin() && (currentUserId == null || !currentUserId.equals(comment.getCreatedBy()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
        }

        // 级联删除所有子评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, commentId);
        commentMapper.delete(wrapper);
        commentMapper.deleteById(commentId);
    }

    /**
     * 按业务对象删除所有评论（用于业务对象删除时级联清理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBiz(String bizType, Long bizId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBizType, bizType).eq(Comment::getBizId, bizId);
        commentMapper.delete(wrapper);
    }

    private CommentResponse toResponse(Comment comment, Map<Long, User> userCache) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setBizType(comment.getBizType());
        resp.setBizId(comment.getBizId());
        resp.setContent(comment.getContent());
        resp.setParentId(comment.getParentId());
        resp.setCreatedBy(comment.getCreatedBy());
        resp.setCreatedAt(comment.getCreatedAt());

        if (comment.getCreatedBy() != null) {
            User user = userCache.computeIfAbsent(comment.getCreatedBy(), userMapper::selectById);
            if (user != null) {
                resp.setCreatedByName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            }
        }
        return resp;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
