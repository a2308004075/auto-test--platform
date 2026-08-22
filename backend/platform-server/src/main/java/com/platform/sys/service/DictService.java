/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 字典管理服务
 */
package com.platform.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.sys.dto.DictBatchDeleteRequest;
import com.platform.sys.dto.DictCreateRequest;
import com.platform.sys.dto.DictListItem;
import com.platform.sys.entity.Dict;
import com.platform.sys.mapper.DictMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
        wrapper.orderByAsc(Dict::getSortNo)
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
}
