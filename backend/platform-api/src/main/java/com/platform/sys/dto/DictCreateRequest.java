/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 字典创建请求 DTO
 */
package com.platform.sys.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 字典创建/更新请求
 */
@Data
public class DictCreateRequest {

    /**
     * 字典类型编码
     */
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    /**
     * 字典类型名称
     */
    @NotBlank(message = "字典类型名称不能为空")
    private String dictTypeName;

    /**
     * 字典值
     */
    @NotBlank(message = "字典值不能为空")
    private String dictValue;

    /**
     * 字典值名称
     */
    @NotBlank(message = "字典值名称不能为空")
    private String dictValueName;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 备注
     */
    private String remark;
}
