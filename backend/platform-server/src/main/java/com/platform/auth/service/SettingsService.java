package com.platform.auth.service;

import com.platform.auth.dto.GlobalConfigResponse;
import com.platform.auth.dto.GlobalConfigUpdateRequest;
import com.platform.auth.dto.TestSendRequest;
import com.platform.auth.entity.GlobalSettings;
import com.platform.auth.mapper.GlobalSettingsMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    /**
     * 读取配置值（不存在时返回空字符串）
     */
    private String getConfigValue(String configKey) {
        GlobalSettings settings = globalSettingsMapper.selectByConfigKey(configKey);
        return settings != null && settings.getConfigValue() != null ? settings.getConfigValue() : "";
    }

    /**
     * 测试 SMTP 邮件发送
     *
     * <p>模拟实现：校验 SMTP 配置完整性，不实际发送邮件。
     * 后续可接入 Spring Boot Mail 真实发送。
     */
    public String testSmtpSend(TestSendRequest request) {
        String host = getConfigValue("notification.smtp.host");
        String port = getConfigValue("notification.smtp.port");
        String username = getConfigValue("notification.smtp.username");

        if (host.isEmpty() || port.isEmpty() || username.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "SMTP 配置不完整，请先填写服务器地址、端口和账号");
        }
        if (request.getRecipient() == null || request.getRecipient().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "收件人邮箱不能为空");
        }

        log.info("测试邮件发送模拟: host={}, port={}, user={}, recipient={}",
                host, port, username, request.getRecipient());
        return "测试邮件已发送至 " + request.getRecipient();
    }

    /**
     * 测试 Webhook 通知发送
     *
     * <p>模拟实现：校验 Webhook URL 配置，不实际发送 HTTP 请求。
     * 后续可接入 OkHttp 真实发送。
     */
    public String testWebhookSend(TestSendRequest request) {
        String webhookUrl = getConfigValue("notification.webhook.url");
        String overrideUrl = request.getRecipient();

        if (webhookUrl.isEmpty() && (overrideUrl == null || overrideUrl.trim().isEmpty())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "Webhook URL 未配置，请先填写或提供测试回调地址");
        }

        String targetUrl = (overrideUrl != null && !overrideUrl.trim().isEmpty()) ? overrideUrl : webhookUrl;
        log.info("测试 Webhook 发送模拟: url={}", targetUrl);
        return "Webhook 通知已发送至 " + targetUrl;
    }
}
