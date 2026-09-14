/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷状态流转请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 缺陷状态流转请求
 */
@Data
public class DefectStatusTransitionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    private String remark;
}
