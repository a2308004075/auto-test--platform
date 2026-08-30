/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求文档管理服务
 */
package com.platform.requirement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.project.service.ProjectService;
import com.platform.requirement.dto.RequirementItemCreateRequest;
import com.platform.requirement.dto.RequirementItemResponse;
import com.platform.requirement.dto.RequirementVersionCreateRequest;
import com.platform.requirement.dto.RequirementVersionResponse;
import com.platform.requirement.entity.RequirementItem;
import com.platform.requirement.entity.RequirementVersion;
import com.platform.requirement.mapper.RequirementItemMapper;
import com.platform.requirement.mapper.RequirementVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 需求文档管理服务
 *
 * <p>提供需求版本和需求条目的 CRUD 操作。
 * 版本归属于项目，条目归属于版本；版本删除时级联删除其下所有条目（由 FK CASCADE 保证）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementService {

    private final RequirementVersionMapper versionMapper;
    private final RequirementItemMapper itemMapper;
    private final ProjectService projectService;

    // ===== 版本管理 =====

    /**
     * 查询项目下的版本列表（按创建时间倒序），每个版本附带条目计数
     */
    public List<RequirementVersionResponse> listVersions(Long projectId) {
        LambdaQueryWrapper<RequirementVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementVersion::getProjectId, projectId)
                .orderByDesc(RequirementVersion::getCreatedAt);
        List<RequirementVersion> versions = versionMapper.selectList(wrapper);

        List<RequirementVersionResponse> result = new ArrayList<>();
        for (RequirementVersion v : versions) {
            RequirementVersionResponse resp = toVersionResponse(v);
            // 统计该版本下的条目数量
            LambdaQueryWrapper<RequirementItem> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(RequirementItem::getVersionId, v.getId());
            resp.setItemCount(Math.toIntExact(itemMapper.selectCount(countWrapper)));
            result.add(resp);
        }
        return result;
    }

    /**
     * 创建版本
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementVersionResponse createVersion(RequirementVersionCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        RequirementVersion version = new RequirementVersion();
        version.setProjectId(request.getProjectId());
        version.setVersionName(request.getVersionName());
        version.setDescription(request.getDescription());
        version.setStatus(request.getStatus() != null ? request.getStatus() : "PLANNING");
        version.setStartDate(request.getStartDate());
        version.setEndDate(request.getEndDate());

        versionMapper.insert(version);
        RequirementVersionResponse resp = toVersionResponse(version);
        resp.setItemCount(0);
        return resp;
    }

    /**
     * 更新版本
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementVersionResponse updateVersion(Long versionId, RequirementVersionCreateRequest request) {
        RequirementVersion version = findVersionById(versionId);

        version.setVersionName(request.getVersionName());
        version.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            version.setStatus(request.getStatus());
        }
        version.setStartDate(request.getStartDate());
        version.setEndDate(request.getEndDate());

        versionMapper.updateById(version);

        RequirementVersionResponse resp = toVersionResponse(version);
        LambdaQueryWrapper<RequirementItem> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(RequirementItem::getVersionId, versionId);
        resp.setItemCount(Math.toIntExact(itemMapper.selectCount(countWrapper)));
        return resp;
    }

    /**
     * 删除版本（FK CASCADE 自动删除其下所有条目）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(Long versionId) {
        findVersionById(versionId);
        versionMapper.deleteById(versionId);
    }

    // ===== 需求条目管理 =====

    /**
     * 查询版本下的需求条目列表（按排序号升序，创建时间升序）
     */
    public List<RequirementItemResponse> listItems(Long versionId) {
        findVersionById(versionId);

        LambdaQueryWrapper<RequirementItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementItem::getVersionId, versionId)
                .orderByAsc(RequirementItem::getSortOrder)
                .orderByAsc(RequirementItem::getCreatedAt);
        List<RequirementItem> items = itemMapper.selectList(wrapper);

        List<RequirementItemResponse> result = new ArrayList<>();
        for (RequirementItem item : items) {
            result.add(toItemResponse(item));
        }
        return result;
    }

    /**
     * 创建需求条目
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementItemResponse createItem(RequirementItemCreateRequest request) {
        findVersionById(request.getVersionId());

        RequirementItem item = new RequirementItem();
        item.setVersionId(request.getVersionId());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setReqType(request.getReqType() != null ? request.getReqType() : "FEATURE");
        item.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        item.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        item.setAssignee(request.getAssignee());
        item.setDeadline(request.getDeadline());

        // 自动计算排序号（当前版本最大 sort_order + 1）
        LambdaQueryWrapper<RequirementItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementItem::getVersionId, request.getVersionId())
                .orderByDesc(RequirementItem::getSortOrder)
                .last("LIMIT 1");
        RequirementItem last = itemMapper.selectOne(wrapper);
        item.setSortOrder(last != null && last.getSortOrder() != null ? last.getSortOrder() + 1 : 0);

        itemMapper.insert(item);
        return toItemResponse(item);
    }

    /**
     * 更新需求条目
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementItemResponse updateItem(Long itemId, RequirementItemCreateRequest request) {
        RequirementItem item = findItemById(itemId);

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        if (request.getReqType() != null) {
            item.setReqType(request.getReqType());
        }
        if (request.getPriority() != null) {
            item.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        item.setAssignee(request.getAssignee());
        item.setDeadline(request.getDeadline());

        itemMapper.updateById(item);
        return toItemResponse(item);
    }

    /**
     * 删除需求条目
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        findItemById(itemId);
        itemMapper.deleteById(itemId);
    }

    // ===== 内部方法 =====

    private RequirementVersion findVersionById(Long versionId) {
        RequirementVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "版本不存在");
        }
        return version;
    }

    private RequirementItem findItemById(Long itemId) {
        RequirementItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求条目不存在");
        }
        return item;
    }

    private RequirementVersionResponse toVersionResponse(RequirementVersion v) {
        RequirementVersionResponse resp = new RequirementVersionResponse();
        resp.setId(v.getId());
        resp.setProjectId(v.getProjectId());
        resp.setVersionName(v.getVersionName());
        resp.setDescription(v.getDescription());
        resp.setStatus(v.getStatus());
        resp.setStartDate(v.getStartDate());
        resp.setEndDate(v.getEndDate());
        resp.setCreatedAt(v.getCreatedAt());
        resp.setUpdatedAt(v.getUpdatedAt());
        return resp;
    }

    private RequirementItemResponse toItemResponse(RequirementItem item) {
        RequirementItemResponse resp = new RequirementItemResponse();
        resp.setId(item.getId());
        resp.setVersionId(item.getVersionId());
        resp.setTitle(item.getTitle());
        resp.setDescription(item.getDescription());
        resp.setReqType(item.getReqType());
        resp.setPriority(item.getPriority());
        resp.setStatus(item.getStatus());
        resp.setAssignee(item.getAssignee());
        resp.setDeadline(item.getDeadline());
        resp.setSortOrder(item.getSortOrder());
        resp.setCreatedAt(item.getCreatedAt());
        resp.setUpdatedAt(item.getUpdatedAt());
        return resp;
    }
}
