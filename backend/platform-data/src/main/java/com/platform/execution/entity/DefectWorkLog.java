/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷工时记录实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 缺陷工时记录实体
 *
 * <p>对应数据库 defect_work_log 表。</p>
 */
@Data
@TableName("defect_work_log")
public class DefectWorkLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 缺陷 ID
     */
    private Long defectId;

    /**
     * 记录人 ID
     */
    private Long userId;

    /**
     * 工作日期
     */
    private LocalDate logDate;

    /**
     * 工时（小时）
     */
    private BigDecimal hours;

    /**
     * 工时类型
     */
    private String workType;

    /**
     * 工作说明
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
