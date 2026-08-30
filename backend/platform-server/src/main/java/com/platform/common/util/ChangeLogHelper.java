/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录采集工具
 */
package com.platform.common.util;

import com.platform.common.service.ChangeLogService;
import com.platform.common.service.ChangeLogService.FieldChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 变更记录采集工具
 *
 * <p>用于在业务 Service 中比较对象字段变更，生成变更记录。
 */
public final class ChangeLogHelper {

    private ChangeLogHelper() {
    }

    /**
     * 创建一个新的变更采集器
     *
     * @param bizType 业务类型
     * @param bizId   业务对象 ID
     * @param service 变更记录服务
     */
    public static Collector collect(String bizType, Long bizId, ChangeLogService service) {
        return new Collector(bizType, bizId, service);
    }

    /**
     * 变更采集器
     */
    public static class Collector {
        private final String bizType;
        private final Long bizId;
        private final ChangeLogService service;
        private final List<FieldChange> changes = new ArrayList<>();

        private Collector(String bizType, Long bizId, ChangeLogService service) {
            this.bizType = bizType;
            this.bizId = bizId;
            this.service = service;
        }

        /**
         * 比较字段值是否发生变化
         *
         * @param fieldName 字段名
         * @param oldValue  旧值
         * @param newValue  新值
         */
        public Collector compare(String fieldName, Object oldValue, Object newValue) {
            String oldStr = toString(oldValue);
            String newStr = toString(newValue);
            if (!Objects.equals(oldStr, newStr)) {
                changes.add(new FieldChange(fieldName, oldStr, newStr));
            }
            return this;
        }

        /**
         * 比较字段值，使用 Supplier 延迟获取新值（用于更新前对象已被修改的场景）
         *
         * @param fieldName  字段名
         * @param oldValue   旧值
         * @param newValueFn 新值提供者
         */
        public Collector compareLazy(String fieldName, Object oldValue, Supplier<Object> newValueFn) {
            return compare(fieldName, oldValue, newValueFn.get());
        }

        /**
         * 保存变更记录
         */
        public void save() {
            service.batchRecord(bizType, bizId, changes);
        }

        private String toString(Object value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }
    }
}
