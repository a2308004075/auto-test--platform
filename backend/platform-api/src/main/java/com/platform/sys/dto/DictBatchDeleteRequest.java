/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description DictBatchDeleteRequest
 */
package com.platform.sys.dto;

import lombok.Data;

import java.util.List;

/**
 * 字典批量删除请求
 */
@Data
public class DictBatchDeleteRequest {

    /**
     * 要删除的字典 ID 列表
     */
    private List<Long> ids;
}
