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
import com.pcs.api.model.PCSConfig;
import com.pcs.api.protocol.PacketType;
import com.pcs.api.protocol.PlayerEventPacket;
import com.pcs.api.protocol.ProtocolPacket;
import com.pcs.api.protocol.StatusPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);
    private static final Gson GSON = new Gson();
    
    // 服务器ID -> WebSocket会话
    private final Map<String, WebSocketSession> serverSessions = new ConcurrentHashMap<>();
    
    // 会话ID -> 服务器ID
    private final Map<String, String> sessionToServer = new ConcurrentHashMap<>();

    // 命令响应等待 Map<commandId, CompletableFuture>
    private final Map<String, CompletableFuture<String>> commandFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 玩家超时时间（毫秒）：5分钟没有更新视为离线
    private static final long PLAYER_TIMEOUT_MS = 5 * 60 * 1000;
    
    public WebSocketSessionManager() {
        // 启动定期清理任务
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            this::cleanupStalePlayers, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * 清理过期的玩家记录（超过5分钟没有更新的视为离线）
     */
    private void cleanupStalePlayers() {
        long now = System.currentTimeMillis();
        serverSessions.forEach((serverId, session) -> {
            if (session.isOpen()) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> players = (Map<String, Map<String, Object>>) session.getAttributes().get("players");
                if (players != null) {
                    int removed = 0;
                    Iterator<Map.Entry<String, Map<String, Object>>> it = players.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Map<String, Object>> entry = it.next();
                        Map<String, Object> playerInfo = entry.getValue();
                        Long lastUpdate = (Long) playerInfo.get("lastUpdate");
                        if (lastUpdate != null && (now - lastUpdate) > PLAYER_TIMEOUT_MS) {
                            it.remove();
                            removed++;
                        }
                    }
                    if (removed > 0) {
                        logger.info("清理服务器 {} 的 {} 个过期玩家记录", serverId, removed);
                        session.getAttributes().put("playerCount", players.size());
                    }
                }
            }
        });
    }
    
    /**
     * 注册服务器会话
     */
    public void registerSession(String serverId, WebSocketSession session) {
        // 断开旧连接
        WebSocketSession oldSession = serverSessions.get(serverId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException e) {
                // 忽略
            }
        }
        
        serverSessions.put(serverId, session);
        sessionToServer.put(session.getId(), serverId);
    }
    
    /**
     * 注销会话
     */
    public void unregisterSession(String sessionId) {
        String serverId = sessionToServer.remove(sessionId);
        if (serverId != null) {
            WebSocketSession session = serverSessions.remove(serverId);
            // 清理该服务器的所有玩家数据
            if (session != null) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> players = (Map<String, Map<String, Object>>) session.getAttributes().remove("players");
                if (players != null && !players.isEmpty()) {
                    logger.info("服务器 {} 断开连接，清理 {} 个在线玩家记录", serverId, players.size());
                }
            }
        }
    }
    
    /**
     * 发送消息到指定服务器
     * @return 是否发送成功
     */
    public boolean sendToServer(String serverId, ProtocolPacket packet) {
        WebSocketSession session = serverSessions.get(serverId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(packet.toJson()));
                return true;
            } catch (IOException e) {
                // 发送失败，移除会话
                logger.warn("发送消息到服务器 {} 失败: {}", serverId, e.getMessage());
                serverSessions.remove(serverId);
                sessionToServer.remove(session.getId());
                return false;
            }
        }
        logger.warn("无法发送消息: 服务器 {} 不在线或未连接", serverId);
        return false;
    }
    
    /**
     * 广播到所有服务器
     */
    public void broadcast(ProtocolPacket packet) {
        String json = packet.toJson();
        int sessionCount = serverSessions.size();
        logger.info("Broadcasting message to {} servers, type: {}", sessionCount, packet.getType());
        
        if (sessionCount == 0) {
            logger.warn("No server sessions available for broadcast!");
            return;
        }
        
        AtomicInteger successCount = new AtomicInteger(0);
        serverSessions.forEach((serverId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    successCount.incrementAndGet();
                    logger.debug("Broadcast sent to server: {}", serverId);
                } catch (IOException e) {
                    logger.error("Failed to broadcast to server {}: {}", serverId, e.getMessage());
                }
            } else {
                logger.warn("Server session {} is not open, removing", serverId);
                serverSessions.remove(serverId);
                sessionToServer.remove(session.getId());
            }
        });
        
        logger.info("Broadcast complete: {}/{} servers received message", successCount.get(), sessionCount);
    }
    
    /**
     * 广播封禁同步
     */
    public void broadcastBanSync(UUID targetUuid, String action, String reason, int durationDays) {
        Map<String, Object> banData = new ConcurrentHashMap<>();
        banData.put("targetUuid", targetUuid.toString());
        banData.put("action", action);
        banData.put("reason", reason);
        banData.put("durationDays", durationDays);
        banData.put("timestamp", System.currentTimeMillis());

        ProtocolPacket packet = ProtocolPacket.request(PacketType.BAN_SYNC, banData);
        broadcast(packet);
    }

    /**
     * 广播解封通知
     */
    public void broadcastUnban(UUID targetUuid) {
        Map<String, Object> unbanData = new ConcurrentHashMap<>();
        unbanData.put("targetUuid", targetUuid.toString());
        unbanData.put("timestamp", System.currentTimeMillis());

        ProtocolPacket packet = ProtocolPacket.request(PacketType.UNBAN_NOTIFY, unbanData);
        broadcast(packet);
    }

    /**
     * 广播解封通知（带玩家名和源服务器）
     */
    public void broadcastUnban(UUID targetUuid, String targetName, String sourceServerId) {
        Map<String, Object> unbanData = new ConcurrentHashMap<>();
        unbanData.put("targetUuid", targetUuid.toString());
        unbanData.put("targetName", targetName);
        unbanData.put("sourceServerId", sourceServerId);
        unbanData.put("timestamp", System.currentTimeMillis());

        ProtocolPacket packet = ProtocolPacket.request(PacketType.UNBAN_NOTIFY, unbanData);
        broadcast(packet);
    }

    /**
     * 广播禁言命令
     */
    public void broadcastMute(UUID targetUuid, String reason, int days) {
        Map<String, Object> muteData = new ConcurrentHashMap<>();
        muteData.put("targetUuid", targetUuid.toString());
        muteData.put("reason", reason);
        muteData.put("durationDays", days);
        muteData.put("timestamp", System.currentTimeMillis());

        ProtocolPacket packet = ProtocolPacket.request(PacketType.MUTE_PLAYER, muteData);
        broadcast(packet);
    }

    /**
     * 广播解除禁言命令
     */
    public void broadcastUnmute(UUID targetUuid) {
        Map<String, Object> unmuteData = new ConcurrentHashMap<>();
        unmuteData.put("targetUuid", targetUuid.toString());
        unmuteData.put("timestamp", System.currentTimeMillis());

        ProtocolPacket packet = ProtocolPacket.request(PacketType.UNMUTE_PLAYER, unmuteData);
        broadcast(packet);
    }
    
    /**
     * 广播到所有服务器，排除指定的服务器
     */
    public void broadcastToAllExcept(ProtocolPacket packet, String excludeServerId) {
        String json = packet.toJson();
        AtomicInteger successCount = new AtomicInteger(0);
        int totalCount = serverSessions.size();
        
        serverSessions.forEach((serverId, session) -> {
            if (serverId.equals(excludeServerId)) {
                return; // 跳过排除的服务器
            }
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    successCount.incrementAndGet();
                    logger.debug("Broadcast sent to server: {}", serverId);
                } catch (IOException e) {
                    logger.error("Failed to broadcast to server {}: {}", serverId, e.getMessage());
                }
            } else {
                logger.warn("Server session {} is not open, removing", serverId);
                serverSessions.remove(serverId);
                sessionToServer.remove(session.getId());
            }
        });
        
        logger.info("Broadcast (excluding {}) complete: {}/{} servers received message", 
            excludeServerId, successCount.get(), totalCount - 1);
    }

    // ==================== 离线账号支持（通过玩家名广播） ====================

    /**
     * 广播封禁同步（通过玩家名，支持离线账号）
     */
    public void broadcastBanSyncByName(String targetName, String action, String reason, int durationDays) {
        Map<String, Object> banData = new ConcurrentHashMap<>();
        banData.put("targetName", targetName);
        banData.put("targetUuid", "OFFLINE:" + targetName.toLowerCase());
        banData.put("action", action);
        banData.put("reason", reason);
        banData.put("durationDays", durationDays);
        banData.put("timestamp", System.currentTimeMillis());
        banData.put("offlineMode", true);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.BAN_SYNC, banData);
        broadcast(packet);
    }

    /**
     * 广播解封通知（通过玩家名，支持离线账号）
     */
    public void broadcastUnbanByName(String targetName) {
        Map<String, Object> unbanData = new ConcurrentHashMap<>();
        unbanData.put("targetName", targetName);
        unbanData.put("targetUuid", "OFFLINE:" + targetName.toLowerCase());
        unbanData.put("timestamp", System.currentTimeMillis());
        unbanData.put("offlineMode", true);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.UNBAN_NOTIFY, unbanData);
        broadcast(packet);
    }

    /**
     * 广播禁言命令（通过玩家名，支持离线账号）
     */
    public void broadcastMuteByName(String targetName, String reason, int days) {
        Map<String, Object> muteData = new ConcurrentHashMap<>();
        muteData.put("targetName", targetName);
        muteData.put("targetUuid", "OFFLINE:" + targetName.toLowerCase());
        muteData.put("reason", reason);
        muteData.put("durationDays", days);
        muteData.put("timestamp", System.currentTimeMillis());
        muteData.put("offlineMode", true);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.MUTE_PLAYER, muteData);
        broadcast(packet);
    }

    /**
     * 广播解除禁言命令（通过玩家名，支持离线账号）
     */
    public void broadcastUnmuteByName(String targetName) {
        Map<String, Object> unmuteData = new ConcurrentHashMap<>();
        unmuteData.put("targetName", targetName);
        unmuteData.put("targetUuid", "OFFLINE:" + targetName.toLowerCase());
        unmuteData.put("timestamp", System.currentTimeMillis());
        unmuteData.put("offlineMode", true);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.UNMUTE_PLAYER, unmuteData);
        broadcast(packet);
    }

    /**
     * 踢出玩家（通过玩家名，支持离线账号）
     */
    public void kickPlayerByName(String playerName, String reason, String serverId) {
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("playerName", playerName);
        data.put("playerUuid", "OFFLINE:" + playerName.toLowerCase());
        data.put("reason", reason);
        data.put("offlineMode", true);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.KICK_PLAYER, data);

        if ("ALL".equals(serverId)) {
            broadcast(packet);
        } else {
            sendToServer(serverId, packet);
        }
    }

    /**
     * 广播消息
     */
    public void broadcastMessage(String message, String targetServerId, boolean useTitle,
                                  int titleFadeIn, int titleStay, int titleFadeOut) {
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("message", message);
        data.put("targetServerId", targetServerId);
        data.put("useTitle", useTitle);
        data.put("titleFadeIn", titleFadeIn);
        data.put("titleStay", titleStay);
        data.put("titleFadeOut", titleFadeOut);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.BROADCAST, data);

        if ("ALL".equals(targetServerId)) {
            broadcast(packet);
        } else {
            sendToServer(targetServerId, packet);
        }
    }

    /**
     * 广播配置更新到所有服务器
     */
    public void broadcastConfigUpdate(PCSConfig config) {
        ProtocolPacket packet = ProtocolPacket.request(PacketType.CONFIG_UPDATE, config);
        broadcast(packet);
    }
    
    /**
     * 发送远程命令
     * @return 是否发送成功
     */
    public boolean sendRemoteCommand(String serverId, String command, String commandId, String executedBy) {
        logger.info("准备发送远程命令 - serverId: {}, command: {}, commandId: {}, executedBy: {}", 
                   serverId, command, commandId, executedBy);
        
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("commandId", commandId);
        data.put("command", command);
        data.put("executedBy", executedBy);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.REMOTE_COMMAND, data);
        
        logger.info("创建远程命令数据包 - packetType: {}, requestId: {}", packet.getType(), packet.getRequestId());

        if ("ALL".equals(serverId)) {
            logger.info("广播命令到所有服务器");
            broadcast(packet);
            return true;
        } else {
            logger.info("发送命令到指定服务器: {}", serverId);
            boolean sent = sendToServer(serverId, packet);
            logger.info("命令发送结果: {}", sent ? "成功" : "失败");
            return sent;
        }
    }

    /**
     * 等待命令响应
     */
    public CompletableFuture<String> waitForCommandResponse(String commandId, long timeoutMs) {
        CompletableFuture<String> future = new CompletableFuture<>();
        commandFutures.put(commandId, future);

        // 设置超时
        timeoutExecutor.schedule(() -> {
            CompletableFuture<String> removed = commandFutures.remove(commandId);
            if (removed != null && !removed.isDone()) {
                removed.completeExceptionally(new TimeoutException("Command timed out"));
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * 处理命令响应
     */
    public void handleCommandResponse(String commandId, String output) {
        CompletableFuture<String> future = commandFutures.remove(commandId);
        if (future != null) {
            future.complete(output);
        }
    }

    /**
     * 获取在线服务器数量
     */
    public int getOnlineServerCount() {
        return (int) serverSessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

    /**
     * 获取服务器会话
     */
    public WebSocketSession getServerSession(String serverId) {
        return serverSessions.get(serverId);
    }

    /**
     * 踢出玩家
     */
    public void kickPlayer(UUID playerUuid, String reason, String serverId) {
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("playerUuid", playerUuid.toString());
        data.put("reason", reason);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.KICK_PLAYER, data);

        if ("ALL".equals(serverId)) {
            broadcast(packet);
        } else {
            sendToServer(serverId, packet);
        }
    }

    /**
     * 发送私信给玩家
     */
    public void sendPrivateMessage(UUID playerUuid, String message, String serverId) {
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("playerUuid", playerUuid.toString());
        data.put("message", message);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.PRIVATE_MESSAGE, data);

        if ("ALL".equals(serverId)) {
            broadcast(packet);
        } else {
            sendToServer(serverId, packet);
        }
    }

    /**
     * 获取所有在线服务器信息
     */
    public Map<String, Map<String, Object>> getOnlineServers() {
        Map<String, Map<String, Object>> servers = new ConcurrentHashMap<>();
        serverSessions.forEach((serverId, session) -> {
            if (session != null && session.isOpen() && session.getAttributes() != null) {
                Map<String, Object> attrs = session.getAttributes();
                Map<String, Object> info = new ConcurrentHashMap<>();
                info.put("id", serverId);
                info.put("online", true);
                info.put("name", attrs.getOrDefault("serverName", serverId));
                info.put("type", attrs.getOrDefault("serverType", "SPIGOT"));
                info.put("version", attrs.getOrDefault("serverVersion", "Unknown"));
                // 从session属性获取更多信息
                info.put("playerCount", attrs.getOrDefault("playerCount", 0));
                info.put("maxPlayers", attrs.getOrDefault("maxPlayers", 20));
                info.put("tps", attrs.getOrDefault("tps", "20.0"));
                info.put("memoryUsed", attrs.getOrDefault("memoryUsed", 0));
                info.put("memoryMax", attrs.getOrDefault("memoryMax", 0));
                info.put("uptime", attrs.getOrDefault("uptime", "-"));
                Object lastHeartbeat = attrs.get("lastHeartbeat");
                info.put("lastHeartbeat", lastHeartbeat != null ? lastHeartbeat : System.currentTimeMillis());
                servers.put(serverId, info);
            }
        });
        return servers;
    }

    /**
     * 更新服务器玩家信息
     */
    public void updateServerPlayers(String serverId, PlayerEventPacket event) {
        // 检查参数是否为null
        if (serverId == null || event == null) {
            logger.warn("updateServerPlayers called with null serverId or event");
            return;
        }

        // 检查玩家UUID是否为null
        String playerUuid = event.getPlayerUuid();
        if (playerUuid == null) {
            logger.warn("PlayerEventPacket has null playerUuid, eventType: {}", event.getEventType());
            return;
        }

        WebSocketSession session = serverSessions.get(serverId);
        if (session != null && session.isOpen()) {
            // 更新玩家列表
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> players = (Map<String, Map<String, Object>>) session.getAttributes()
                    .computeIfAbsent("players", k -> new ConcurrentHashMap<String, Map<String, Object>>());

            String eventType = event.getEventType();

            if (PlayerEventPacket.QUIT.equals(eventType) || PlayerEventPacket.KICK.equals(eventType)) {
                players.remove(playerUuid);
            } else {
                Map<String, Object> playerInfo = new ConcurrentHashMap<>();
                playerInfo.put("uuid", playerUuid);
                playerInfo.put("name", event.getPlayerName());
                playerInfo.put("server", serverId);
                playerInfo.put("status", "online");
                playerInfo.put("lastEvent", eventType);
                playerInfo.put("lastUpdate", System.currentTimeMillis());

                // 添加额外数据
                if (event.getData() != null) {
                    playerInfo.putAll(event.getData());
                }

                players.put(playerUuid, playerInfo);
            }

            // 更新玩家计数
            session.getAttributes().put("playerCount", players.size());
        }
    }

    /**
     * 更新服务器状态
     */
    public void updateServerStatus(String serverId, StatusPacket.StatusReport report) {
        WebSocketSession session = serverSessions.get(serverId);
        if (session != null && session.isOpen()) {
            session.getAttributes().put("tps", String.format("%.1f", report.getTps()));
            session.getAttributes().put("playerCount", report.getOnlinePlayers());
            session.getAttributes().put("maxPlayers", report.getMaxPlayers());
            session.getAttributes().put("memoryUsed", report.getMemoryUsed());
            session.getAttributes().put("memoryMax", report.getMemoryMax());
            session.getAttributes().put("uptime", report.getUptimeMinutes() + "分钟");
            session.getAttributes().put("lastHeartbeat", System.currentTimeMillis());

            // 存储世界信息
            if (report.getWorlds() != null) {
                session.getAttributes().put("worlds", report.getWorlds());
            }
        }
    }

    /**
     * 获取服务器上的所有玩家
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getServerPlayers(String serverId) {
        WebSocketSession session = serverSessions.get(serverId);
        if (session != null && session.isOpen()) {
            Map<String, Map<String, Object>> players = (Map<String, Map<String, Object>>) session.getAttributes().get("players");
            if (players != null) {
                return new ArrayList<>(players.values());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 获取所有在线玩家
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllOnlinePlayers() {
        List<Map<String, Object>> allPlayers = new ArrayList<>();
        serverSessions.forEach((serverId, session) -> {
            if (session.isOpen()) {
                Map<String, Map<String, Object>> players = (Map<String, Map<String, Object>>) session.getAttributes().get("players");
                if (players != null) {
                    players.forEach((uuid, info) -> {
                        Map<String, Object> playerCopy = new ConcurrentHashMap<>(info);
                        playerCopy.put("serverId", serverId);
                        allPlayers.add(playerCopy);
                    });
                }
            }
        });
        return allPlayers;
    }
}
