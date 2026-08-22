/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 字典管理控制器
 */
package com.platform.sys.controller;

import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.sys.dto.DictBatchDeleteRequest;
import com.platform.sys.dto.DictCreateRequest;
import com.platform.sys.dto.DictImportResult;
import com.platform.sys.dto.DictListItem;
import com.platform.sys.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * 字典管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/v1/sys/dicts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DictController {

    private final DictService dictService;

    /**
     * 分页查询字典列表
     */
    @GetMapping
    public ApiResponse<PageResponse<DictListItem>> page(
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictTypeName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(dictService.page(dictType, dictTypeName, page, pageSize));
    }

    /**
     * 获取单个字典
     */
    @GetMapping("/{id}")
    public ApiResponse<DictListItem> get(@PathVariable Long id) {
        return ApiResponse.ok(dictService.get(id));
    }

    /**
     * 新增字典
     */
    @PostMapping
    public ApiResponse<DictListItem> add(@Valid @RequestBody DictCreateRequest request) {
        return ApiResponse.ok(dictService.addOrUpdate(null, request));
    }

    /**
     * 更新字典
     */
    @PostMapping("/{id}")
    public ApiResponse<DictListItem> update(@PathVariable Long id,
                                            @Valid @RequestBody DictCreateRequest request) {
        return ApiResponse.ok(dictService.addOrUpdate(id, request));
    }

    /**
     * 批量删除字典
     */
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody DictBatchDeleteRequest request) {
        dictService.batchDelete(request);
        return ApiResponse.ok(null);
    }

    /**
     * 根据字典类型查询字典值列表（公开接口，无需登录）
     */
    @GetMapping("/type/{dictType}")
    public ApiResponse<List<DictListItem>> getByType(@PathVariable String dictType) {
        return ApiResponse.ok(dictService.getByType(dictType));
    }

    /**
     * 导出字典列表到 Excel
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        dictService.exportDicts(response);
    }

    /**
     * 从 Excel 导入字典
     */
    @PostMapping("/import")
    public ApiResponse<DictImportResult> importDicts(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(dictService.importDicts(file));
    }
}
