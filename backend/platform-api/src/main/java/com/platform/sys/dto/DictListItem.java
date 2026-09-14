/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description DictListItem
 */
package com.platform.sys.dto;

import lombok.Data;

/**
 * 字典列表项响应
 */
@Data
public class DictListItem {

    private Long id;
    private String dictType;
    private String dictTypeName;
    private String dictValue;
    private String dictValueName;
    private Integer sortNo;
    private String remark;
    private String createdAt;
    private String updatedAt;
}
