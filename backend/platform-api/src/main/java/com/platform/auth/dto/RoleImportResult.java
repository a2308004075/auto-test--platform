/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 角色导入结果 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色 Excel 导入结果
 */
@Data
public class RoleImportResult {

    private int successCount;
    private int failCount;
    private List<String> errors = new ArrayList<>();
}
