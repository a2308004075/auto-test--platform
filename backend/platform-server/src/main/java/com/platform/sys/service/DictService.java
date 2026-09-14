/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 字典管理服务
 */
package com.platform.sys.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.sys.dto.DictBatchDeleteRequest;
import com.platform.sys.dto.DictCreateRequest;
import com.platform.sys.dto.DictImportResult;
import com.platform.sys.dto.DictListItem;
import com.platform.sys.entity.Dict;
import com.platform.sys.mapper.DictMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictService {

    private final DictMapper dictMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询字典列表
     */
    public PageResponse<DictListItem> page(String dictType, String dictTypeName,
                                           int page, int pageSize) {
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        if (dictType != null && !dictType.isEmpty()) {
            wrapper.like(Dict::getDictType, dictType);
        }
        if (dictTypeName != null && !dictTypeName.isEmpty()) {
            wrapper.like(Dict::getDictTypeName, dictTypeName);
        }
        wrapper.orderByAsc(Dict::getDictType)
                .orderByAsc(Dict::getSortNo)
                .orderByAsc(Dict::getId);

        Page<Dict> pageParam = new Page<>(page, pageSize);
        Page<Dict> result = dictMapper.selectPage(pageParam, wrapper);

        List<DictListItem> items = result.getRecords().stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        return PageResponse.of(items, result.getTotal(), page, pageSize);
    }

    /**
     * 获取单个字典
     */
    public DictListItem get(Long id) {
        Dict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ErrorCode.DICT_NOT_FOUND, "字典不存在");
        }
        return toListItem(dict);
    }

    /**
     * 新增或更新字典
     */
    @Transactional(rollbackFor = Exception.class)
    public DictListItem addOrUpdate(Long id, DictCreateRequest request) {
        Dict dict;
        if (id != null) {
            dict = dictMapper.selectById(id);
            if (dict == null) {
                throw new BusinessException(ErrorCode.DICT_NOT_FOUND, "字典不存在");
            }
            BeanUtils.copyProperties(request, dict);
            dictMapper.updateById(dict);
        } else {
            dict = new Dict();
            BeanUtils.copyProperties(request, dict);
            if (dict.getSortNo() == null) {
                dict.setSortNo(0);
            }
            dict.setIsActive(1);
            dictMapper.insert(dict);
        }
        return toListItem(dict);
    }

    /**
     * 批量删除字典（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(DictBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "请选择要删除的字典");
        }
        dictMapper.deleteBatchIds(request.getIds());
    }

    /**
     * 根据字典类型查询字典值列表
     */
    public List<DictListItem> getByType(String dictType) {
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dict::getDictType, dictType)
                .eq(Dict::getIsActive, 1)
                .orderByAsc(Dict::getSortNo)
                .orderByAsc(Dict::getId);
        List<Dict> dicts = dictMapper.selectList(wrapper);
        return dicts.stream().map(this::toListItem).collect(Collectors.toList());
    }

    // ===== Excel 导入导出 =====

    /**
     * 导出字典列表到 Excel
     */
    public void exportDicts(HttpServletResponse response) {
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dict::getIsActive, 1)
                .orderByAsc(Dict::getDictType)
                .orderByAsc(Dict::getSortNo)
                .orderByAsc(Dict::getId);
        List<Dict> dicts = dictMapper.selectList(wrapper);

        List<List<Object>> rows = new ArrayList<>();
        for (Dict dict : dicts) {
            List<Object> row = new ArrayList<>();
            row.add(dict.getDictType() != null ? dict.getDictType() : "");
            row.add(dict.getDictTypeName() != null ? dict.getDictTypeName() : "");
            row.add(dict.getDictValue() != null ? dict.getDictValue() : "");
            row.add(dict.getDictValueName() != null ? dict.getDictValueName() : "");
            row.add(dict.getSortNo() != null ? dict.getSortNo() : 0);
            row.add(dict.getRemark() != null ? dict.getRemark() : "");
            rows.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode("字典列表.xlsx", "UTF-8"));
            OutputStream out = response.getOutputStream();
            ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(true);
            writer.writeCellValue(0, 0, "字典名称");
            writer.writeCellValue(1, 0, "字典描述");
            writer.writeCellValue(2, 0, "字典键值");
            writer.writeCellValue(3, 0, "字典键值描述");
            writer.writeCellValue(4, 0, "排序号");
            writer.writeCellValue(5, 0, "备注");
            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                for (int j = 0; j < row.size(); j++) {
                    writer.writeCellValue(j, i + 1, row.get(j));
                }
            }
            writer.flush(out, true);
            writer.close();
        } catch (IOException e) {
            log.error("导出字典 Excel 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 从 Excel 导入字典
     */
    @Transactional(rollbackFor = Exception.class)
    public DictImportResult importDicts(MultipartFile file) {
        DictImportResult result = new DictImportResult();
        if (file == null || file.isEmpty()) {
            result.getErrors().add("文件为空");
            return result;
        }

        try {
            ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(file.getInputStream());
            List<List<Object>> rows = reader.read();
            reader.close();

            // 跳过表头行
            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                if (row.isEmpty()) {
                    continue;
                }
                try {
                    String dictType = row.size() > 0 ? String.valueOf(row.get(0)).trim() : "";
                    String dictTypeName = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                    String dictValue = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";
                    String dictValueName = row.size() > 3 ? String.valueOf(row.get(3)).trim() : "";
                    int sortNo = row.size() > 4 ? parseIntSafe(row.get(4)) : 0;
                    String remark = row.size() > 5 ? String.valueOf(row.get(5)).trim() : "";

                    if (dictType.isEmpty() || dictTypeName.isEmpty() || dictValue.isEmpty() || dictValueName.isEmpty()) {
                        result.getErrors().add("第 " + (i + 1) + " 行: 字典名称、字典描述、字典键值、字典键值描述不能为空");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }

                    // 按 dictType + dictValue 查找是否已存在
                    LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Dict::getDictType, dictType)
                            .eq(Dict::getDictValue, dictValue);
                    Dict existing = dictMapper.selectOne(wrapper);

                    if (existing != null) {
                        existing.setDictTypeName(dictTypeName);
                        existing.setDictValueName(dictValueName);
                        existing.setSortNo(sortNo);
                        existing.setRemark(remark);
                        dictMapper.updateById(existing);
                    } else {
                        Dict dict = new Dict();
                        dict.setDictType(dictType);
                        dict.setDictTypeName(dictTypeName);
                        dict.setDictValue(dictValue);
                        dict.setDictValueName(dictValueName);
                        dict.setSortNo(sortNo);
                        dict.setRemark(remark);
                        dict.setIsActive(1);
                        dictMapper.insert(dict);
                    }
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception e) {
                    result.getErrors().add("第 " + (i + 1) + " 行: " + e.getMessage());
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
        } catch (IOException e) {
            log.error("导入字典 Excel 失败", e);
            throw new BusinessException(ErrorCode.EXCEL_IMPORT_FAILED, "文件读取失败: " + e.getMessage());
        }

        log.info("字典导入完成: 成功={}, 失败={}", result.getSuccessCount(), result.getFailCount());
        return result;
    }

    // ===== 私有方法 =====

    private DictListItem toListItem(Dict dict) {
        DictListItem item = new DictListItem();
        item.setId(dict.getId());
        item.setDictType(dict.getDictType());
        item.setDictTypeName(dict.getDictTypeName());
        item.setDictValue(dict.getDictValue());
        item.setDictValueName(dict.getDictValueName());
        item.setSortNo(dict.getSortNo());
        item.setRemark(dict.getRemark());
        if (dict.getCreatedAt() != null) {
            item.setCreatedAt(dict.getCreatedAt().format(DT_FMT));
        }
        if (dict.getUpdatedAt() != null) {
            item.setUpdatedAt(dict.getUpdatedAt().format(DT_FMT));
        }
        return item;
    }

    private int parseIntSafe(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
