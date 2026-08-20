package com.platform.execution.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 执行进度 WebSocket 处理器
 *
 * <p>客户端连接 /ws/execution/{executionId} 后，
 * 服务端通过 sendProgress() 推送执行进度。
 */
@Component
@Slf4j
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String executionId = extractExecutionId(session);
        if (executionId != null) {
            sessions.put(executionId, session);
            log.info("WebSocket 连接建立: executionId={}", executionId);
        } else {
            log.warn("WebSocket 连接缺少 executionId: {}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String executionId = extractExecutionId(session);
        if (executionId != null) {
            sessions.remove(executionId);
            log.info("WebSocket 连接关闭: executionId={}, status={}", executionId, status);
        }
    }

    /**
     * 推送执行进度
     *
     * @param executionId 执行记录 ID
     * @param message     JSON 消息
     */
    public void sendProgress(String executionId, String message) {
        WebSocketSession session = sessions.get(executionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("WebSocket 推送失败: executionId={}", executionId, e);
            }
        }
    }

    /**
     * 从 URL 提取 executionId
     * URL 格式: /ws/execution/{executionId}
     */
    private String extractExecutionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        String[] parts = path.split("/");
        // /ws/execution/{executionId} → ["", "ws", "execution", "executionId"]
        return parts.length > 3 ? parts[3] : null;
    }
}
