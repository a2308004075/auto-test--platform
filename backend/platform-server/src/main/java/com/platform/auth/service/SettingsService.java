package com.platform.auth.service;

import com.platform.auth.dto.GlobalConfigResponse;
import com.platform.auth.dto.GlobalConfigUpdateRequest;
import com.platform.auth.entity.GlobalSettings;
import com.platform.auth.mapper.GlobalSettingsMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局配置服务
 *
 * <p>管理系统级配置项（登录安全、会话超时、密码策略等），仅 ADMIN 可操作。
 */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final GlobalSettingsMapper globalSettingsMapper;

    /**
     * 查询全部全局配置
     */
    public List<GlobalConfigResponse> listAll() {
        List<GlobalSettings> list = globalSettingsMapper.selectList(null);
        List<GlobalConfigResponse> result = new ArrayList<>(list.size());
        for (GlobalSettings g : list) {
            result.add(toResponse(g));
        }
        return result;
    }

    /**
     * 根据配置键查询配置
     */
    public GlobalConfigResponse getByKey(String configKey) {
        GlobalSettings settings = globalSettingsMapper.selectByConfigKey(configKey);
        if (settings == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "配置项不存在：" + configKey);
        }
        return toResponse(settings);
    }

    /**
     * 更新配置项
     */
    @Transactional(rollbackFor = Exception.class)
    public GlobalConfigResponse update(String configKey, GlobalConfigUpdateRequest request) {
        GlobalSettings settings = globalSettingsMapper.selectByConfigKey(configKey);
        if (settings == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "配置项不存在：" + configKey);
        }
        settings.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) {
            settings.setDescription(request.getDescription());
        }
        globalSettingsMapper.updateById(settings);
        return toResponse(settings);
    }

    private GlobalConfigResponse toResponse(GlobalSettings g) {
        GlobalConfigResponse r = new GlobalConfigResponse();
        BeanUtils.copyProperties(g, r);
        return r;
    }
}
