/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description MenuImportResult
 */
package com.platform.sys.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单 Excel 导入结果
 */
@Data
public class MenuImportResult {

    private int successCount;
    private int failCount;
    private List<String> errors = new ArrayList<>();
}
