/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Swagger 导入结果 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

/**
 * Swagger 导入结果
 */
@Data
public class SwaggerImportResult {

    private Integer total;
    private Integer created;
    private Integer updated;
    private Integer skipped;

    public static SwaggerImportResult of(int total, int created, int updated, int skipped) {
        SwaggerImportResult r = new SwaggerImportResult();
        r.setTotal(total);
        r.setCreated(created);
        r.setUpdated(updated);
        r.setSkipped(skipped);
        return r;
    }
}
