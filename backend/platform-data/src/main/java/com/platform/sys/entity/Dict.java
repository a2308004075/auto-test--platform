/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 字典实体类
 */
package com.platform.sys.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class Dict extends BaseEntity {

    /**
     * 字典类型编码
     */
    private String dictType;

    /**
     * 字典类型名称
     */
    private String dictTypeName;

    /**
     * 字典值
     */
    private String dictValue;

    /**
     * 字典值名称
     */
    private String dictValueName;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否启用（1=启用 0=停用）
     */
    @TableLogic
    private Integer isActive;
}
