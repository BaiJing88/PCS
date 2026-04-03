/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */

package com.pcs.central.websocket;

import com.google.gson.Gson;
import com.pcs.central.service.PlayerService;
import com.pcs.central.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理面板WebSocket处理器
 * 处理前端admin.html的实时推送连接
 */
@Component
public class AdminWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminWebSocketHandler.class);
    private static final Gson GSON = new Gson();

    private final WebSocketSessionManager sessionManager;
    private final PlayerService playerService;
    private final VoteService voteService;

    // 活跃的管理面板连接
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();

    public AdminWebSocketHandler(WebSocketSessionManager sessionManager,
                                  PlayerService playerService,
                                  VoteService voteService) {
        this.sessionManager = sessionManager;
        this.playerService = playerService;
        this.voteService = voteService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        adminSessions.put(sessionId, session);
        logger.info("管理面板WebSocket连接已建立: {}", sessionId);

        // 发送连接成功消息
        sendToSession(session, Map.of(
            "type", "connected",
            "sessionId", sessionId,
            "message", "已连接到实时推送服务"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            // 解析简单的JSON消息
            Map<String, Object> data = GSON.fromJson(payload, Map.class);
            String action = (String) data.get("action");

            switch (action != null ? action : "ping") {
                case "ping":
                    sendToSession(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
                    break;
                case "subscribe":
                    // 管理员订阅特定类型的事件
                    String eventType = (String) data.get("eventType");
                    logger.info("管理员订阅事件: {}", eventType);
                    break;
                case "requestUpdate":
                    // 管理员请求立即刷新数据
                    broadcastServerUpdate();
                    broadcastPlayerUpdate();
                    break;
                default:
                    logger.debug("未知WebSocket消息类型: {}", action);
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        adminSessions.remove(sessionId);
        logger.info("管理面板WebSocket连接已关闭: {}, 状态: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("管理面板WebSocket传输错误: {}", sessionId, exception);
        adminSessions.remove(sessionId);
    }

    /**
     * 广播服务器状态更新到所有管理面板连接
     */
    public void broadcastServerUpdate() {
        adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    sendToSession(session, Map.of(
                        "type", "serverUpdate",
                        "timestamp", System.currentTimeMillis(),
                        "onlineServers", sessionManager.getOnlineServerCount()
                    ));
                } catch (Exception e) {
                    logger.error("广播服务器更新失败", e);
                }
            });
    }

    /**
     * 广播玩家列表更新到所有管理面板连接
     */
    public void broadcastPlayerUpdate() {
        adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    sendToSession(session, Map.of(
                        "type", "playerUpdate",
                        "timestamp", System.currentTimeMillis(),
                        "onlinePlayers", sessionManager.getAllOnlinePlayers().size()
                    ));
                } catch (Exception e) {
                    logger.error("广播玩家更新失败", e);
                }
            });
    }

    /**
     * 广播投票更新
     */
    public void broadcastVoteUpdate() {
        adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    sendToSession(session, Map.of(
                        "type", "voteUpdate",
                        "timestamp", System.currentTimeMillis(),
                        "activeVotes", voteService.getAllActiveVotes().size()
                    ));
                } catch (Exception e) {
                    logger.error("广播投票更新失败", e);
                }
            });
    }

    /**
     * 广播封禁/解禁通知
     */
    public void broadcastBanUpdate(String playerName, String action, String reason) {
        adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    sendToSession(session, Map.of(
                        "type", "banUpdate",
                        "timestamp", System.currentTimeMillis(),
                        "playerName", playerName,
                        "action", action,
                        "reason", reason
                    ));
                } catch (Exception e) {
                    logger.error("广播封禁更新失败", e);
                }
            });
    }

    /**
     * 定时发送心跳保活
     */
    @Scheduled(fixedRate = 30000) // 每30秒
    public void sendHeartbeat() {
        adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    sendToSession(session, Map.of(
                        "type", "heartbeat",
                        "timestamp", System.currentTimeMillis()
                    ));
                } catch (Exception e) {
                    logger.error("发送心跳失败", e);
                }
            });
    }

    /**
     * 发送消息到指定会话
     */
    private void sendToSession(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (session.isOpen()) {
            String json = GSON.toJson(data);
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * 获取当前活跃的管理面板连接数
     */
    public int getActiveConnectionCount() {
        return (int) adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .count();
    }
}
