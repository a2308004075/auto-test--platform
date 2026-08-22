/**
 * @author HXN
 * @date 2026-08-21 15:30
 * @description 端口自增配置类
 */
package com.platform.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * 端口自动递增配置
 * <p>
 * 默认使用配置文件中指定的端口（如 8080），若该端口被占用，
 * 则自动尝试 8081、8082……直到找到可用端口，避免启动失败。
 */
@Slf4j
@Configuration
public class AutoPortIncrementConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private static final int MAX_PORT_RETRIES = 100;

    private final ServerProperties serverProperties;

    public AutoPortIncrementConfig(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        Integer configuredPort = serverProperties.getPort();

        // server.port 为 0 或 null 时由 Spring Boot 自行处理，不再干预
        if (configuredPort == null || configuredPort == 0) {
            return;
        }

        int port = configuredPort;
        int maxPort = configuredPort + MAX_PORT_RETRIES;

        while (port <= maxPort) {
            if (isPortAvailable(port)) {
                if (port != configuredPort) {
                    log.warn("端口 {} 已被占用，应用自动切换至可用端口 {}", configuredPort, port);
                } else {
                    log.info("应用将监听端口 {}", port);
                }
                factory.setPort(port);
                return;
            }
            port++;
        }

        throw new IllegalStateException(
                String.format("无法找到可用端口，初始端口：%d，已尝试 %d 次", configuredPort, MAX_PORT_RETRIES)
        );
    }

    /**
     * 检查指定端口是否可用
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
