/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录服务
 */
package com.platform.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.auth.mapper.UserMapper;
import com.platform.common.dto.ChangeLogResponse;
import com.platform.common.entity.ChangeLog;
import com.platform.common.mapper.ChangeLogMapper;
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
 * 变更记录服务
 *
 * <p>支持按业务类型 + 业务对象 ID 记录和查询字段变更历史。
 */
@Service
@RequiredArgsConstructor
public class ChangeLogService {

    private final ChangeLogMapper changeLogMapper;
    private final UserMapper userMapper;

    /**
     * 查询业务对象下的变更记录列表
     *
     * @param fieldName 字段名（传 null 表示查询全部）
     */
    public List<ChangeLogResponse> listByBiz(String bizType, Long bizId, String fieldName) {
        LambdaQueryWrapper<ChangeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChangeLog::getBizType, bizType)
                .eq(ChangeLog::getBizId, bizId);
        if (fieldName != null && !fieldName.isEmpty()) {
            wrapper.eq(ChangeLog::getFieldName, fieldName);
        }
        wrapper.orderByDesc(ChangeLog::getCreatedAt);
        List<ChangeLog> logs = changeLogMapper.selectList(wrapper);

        Map<Long, User> userCache = new HashMap<>();
        List<ChangeLogResponse> result = new ArrayList<>();
        for (ChangeLog log : logs) {
            result.add(toResponse(log, userCache));
        }
        return result;
    }

    /**
     * 批量记录字段变更
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchRecord(String bizType, Long bizId, List<FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        Long userId = getCurrentUserId();
        for (FieldChange change : changes) {
            ChangeLog log = new ChangeLog();
            log.setBizType(bizType);
            log.setBizId(bizId);
            log.setFieldName(change.getFieldName());
            log.setOldValue(change.getOldValue());
            log.setNewValue(change.getNewValue());
            log.setCreatedBy(userId);
            changeLogMapper.insert(log);
        }
    }

    /**
     * 按业务对象删除所有变更记录（用于业务对象删除时级联清理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBiz(String bizType, Long bizId) {
        LambdaQueryWrapper<ChangeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChangeLog::getBizType, bizType).eq(ChangeLog::getBizId, bizId);
        changeLogMapper.delete(wrapper);
    }

    private ChangeLogResponse toResponse(ChangeLog log, Map<Long, User> userCache) {
        ChangeLogResponse resp = new ChangeLogResponse();
        resp.setId(log.getId());
        resp.setBizType(log.getBizType());
        resp.setBizId(log.getBizId());
        resp.setFieldName(log.getFieldName());
        resp.setOldValue(log.getOldValue());
        resp.setNewValue(log.getNewValue());
        resp.setCreatedBy(log.getCreatedBy());
        resp.setCreatedAt(log.getCreatedAt());

        if (log.getCreatedBy() != null) {
            User user = userCache.computeIfAbsent(log.getCreatedBy(), userMapper::selectById);
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

    /**
     * 字段变更项
     */
    public static class FieldChange {
        private final String fieldName;
        private final String oldValue;
        private final String newValue;

        public FieldChange(String fieldName, String oldValue, String newValue) {
            this.fieldName = fieldName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }
    }
}
