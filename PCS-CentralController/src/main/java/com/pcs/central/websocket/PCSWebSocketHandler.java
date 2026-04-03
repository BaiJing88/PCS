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
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.RatingInfo;
import com.pcs.api.protocol.*;
import com.pcs.api.protocol.UnbanPacket;
import com.pcs.api.security.EncryptionManager;
import com.pcs.central.database.RatingRepository;
import com.pcs.central.model.entity.RatingEntity;
import com.pcs.central.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class PCSWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PCSWebSocketHandler.class);
    private static final Gson GSON = new Gson();
    
    @Autowired
    private com.pcs.central.config.YamlConfigReader configReader;
    
    private String configuredApiKey;
    
    private final WebSocketSessionManager sessionManager;
    private final PlayerService playerService;
    private final ConfigService configService;
    private final RatingService ratingService;
    private final KickBanService kickBanService;
    private final ServerRegistryService serverRegistryService;
    private final RatingRepository ratingRepository;

    // 加密管理器
    private final EncryptionManager encryptionManager = new EncryptionManager();

    public PCSWebSocketHandler(WebSocketSessionManager sessionManager,
                               PlayerService playerService,
                               ConfigService configService,
                               RatingService ratingService,
                               KickBanService kickBanService,
                               ServerRegistryService serverRegistryService,
                               RatingRepository ratingRepository) {
        this.sessionManager = sessionManager;
        this.playerService = playerService;
        this.configService = configService;
        this.ratingService = ratingService;
        this.kickBanService = kickBanService;
        this.serverRegistryService = serverRegistryService;
        this.ratingRepository = ratingRepository;
    }
    
    @PostConstruct
    public void init() {
        this.configuredApiKey = configReader.getApiKey();
        
        logger.info("=== PCS WebSocket Handler Initialized ===");
        logger.info("API Key configured: {}", configuredApiKey != null && !configuredApiKey.isEmpty());
        logger.info("==========================================");
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            // 解析协议数据包
            
            // 尝试解析为ProtocolPacket
            ProtocolPacket packet = null;
            PacketType type = null;
            
            try {
                packet = ProtocolPacket.fromJson(payload);
                type = PacketType.fromString(packet.getType());
            } catch (Exception e) {
                logger.debug("ProtocolPacket parse failed");
            }
            
            // 如果解析失败
            if (type == null) {
                sendError(session, null, "Unknown packet type");
                return;
            }

            // Handle encrypted packets
            if (type == PacketType.ENCRYPTED) {
                if (!encryptionManager.isEncryptionEnabled(session.getId())) {
                    sendError(session, packet.getRequestId(), "Encryption not enabled for this session");
                    return;
                }
                // Decrypt the packet
                packet = encryptionManager.decryptPacket(session.getId(), packet);
                type = PacketType.fromString(packet.getType());
            }

            switch (type) {
                case HANDSHAKE:
                    handleHandshake(session, packet);
                    break;
                case AUTH_REQUEST:
                    handleAuth(session, packet);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(session, packet);
                    break;
                case CONFIG_REQUEST:
                    handleConfigRequest(session, packet);
                    break;
                case PLAYER_DATA_REQUEST:
                    handlePlayerDataRequest(session, packet);
                    break;
                case REMOTE_COMMAND_RESPONSE:
                    handleCommandResponse(packet);
                    break;
                case RATING_SUBMIT:
                    handleRatingSubmit(session, packet);
                    break;
                case KEY_EXCHANGE:
                    handleKeyExchange(session, packet);
                    break;
                case PLAYER_EVENT:
                    handlePlayerEvent(session, packet);
                    break;
                case STATUS_REPORT:
                    handleStatusReport(session, packet);
                    break;
                case UNBAN_REQUEST:
                    handleUnbanRequest(session, packet);
                    break;
                case LEGACY_BAN_SYNC:
                    handleLegacyBanSync(session, packet);
                    break;
                case BAN_SYNC:
                    handleBanSync(session, packet);
                    break;
                case MUTE_PLAYER:
                    handleMutePlayer(session, packet);
                    break;
                case KICK_PLAYER:
                    handleKickPlayer(session, packet);
                    break;
                case DISCONNECT:
                    session.close();
                    break;
                default:
                    sendError(session, packet.getRequestId(), "Unhandled packet type: " + type);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message", e);
            sendError(session, null, "Internal error: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
        sessionManager.unregisterSession(session.getId());
        encryptionManager.removeSession(session.getId());
    }

    private void handleKeyExchange(WebSocketSession session, ProtocolPacket packet) throws IOException {
        Map<String, Object> payload = packet.getPayload(Map.class);
        String step = (String) payload.get("step");

        if ("request".equals(step)) {
            // Client sent public key, initialize server session
            encryptionManager.initializeSession(session.getId());

            String clientPublicKey = (String) payload.get("publicKey");
            encryptionManager.completeServerKeyExchange(session.getId(), clientPublicKey);

            // Send server public key back
            ProtocolPacket response = encryptionManager.createKeyExchangeResponse(session.getId(), packet.getRequestId());
            sendPacket(session, response);
            logger.info("Key exchange completed for session: {}", session.getId());
        }
    }

    private void sendPacket(WebSocketSession session, ProtocolPacket packet) throws IOException {
        // Encrypt if encryption is enabled
        if (encryptionManager.isEncryptionEnabled(session.getId())) {
            packet = encryptionManager.encryptPacket(session.getId(), packet);
        }
        session.sendMessage(new TextMessage(packet.toJson()));
    }
    
    private void handleHandshake(WebSocketSession session, ProtocolPacket packet) throws IOException {
        AuthPacket handshake = packet.getPayload(AuthPacket.class);
        
        // 发送挑战码
        String challenge = generateChallenge();
        session.getAttributes().put("challenge", challenge);
        session.getAttributes().put("serverId", handshake.getServerId());
        
        AuthPacket response = new AuthPacket();
        response.setChallenge(challenge);

        ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                PacketType.HANDSHAKE, response);
        sendPacket(session, responsePacket);
    }

    private void handleAuth(WebSocketSession session, ProtocolPacket packet) throws IOException {
        AuthPacket auth = packet.getPayload(AuthPacket.class);
        String requestedServerId = auth.getServerId(); // 客户端请求的服务器ID（可能为null或临时值）
        String apiKey = auth.getApiKey();
        String serverName = auth.getServerName();
        String serverType = auth.getServerType() != null ? auth.getServerType() : "SPIGOT";

        // 验证API密钥（优先使用环境变量，其次使用配置文件，最后使用默认值）
        String expectedApiKey = System.getenv("PCS_API_KEY");
        if (expectedApiKey == null || expectedApiKey.isEmpty()) {
            expectedApiKey = configuredApiKey; // 从配置文件注入
        }

        if (apiKey != null && apiKey.equals(expectedApiKey)) {
            // 【核心】由中控分配或确认服务器ID
            String assignedServerId = serverRegistryService.registerOrGetServerId(
                    requestedServerId, apiKey, serverName, serverType);
            
            // 更新服务器信息（包括版本号）
            serverRegistryService.updateServerInfo(assignedServerId, serverName, serverType, auth.getServerVersion());
            
            // 使用中控分配的ID
            session.getAttributes().put("authenticated", true);
            session.getAttributes().put("serverId", assignedServerId);
            session.getAttributes().put("serverName", serverName != null ? serverName : assignedServerId);
            session.getAttributes().put("serverType", serverType);
            session.getAttributes().put("serverVersion", auth.getServerVersion() != null ? auth.getServerVersion() : "Unknown");
            sessionManager.registerSession(assignedServerId, session);

            // 构建响应，包含中控分配的服务器ID
            AuthPacket response = new AuthPacket();
            response.setSuccess(true);
            response.setMessage("Authenticated successfully");
            response.setToken(generateToken());
            response.setServerId(assignedServerId); // 返回分配的服务器ID
            response.setAssignedId(!assignedServerId.equals(requestedServerId)); // 标记是否是新分配的ID

            ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                    PacketType.AUTH_RESPONSE, response);
            sendPacket(session, responsePacket);

            logger.info("Server authenticated via API key: {} (requested: {}, assigned: {})", 
                       assignedServerId, requestedServerId, assignedServerId);
        } else {
            // 尝试 challenge-response 认证（如果之前有过 HANDSHAKE）
            String challenge = (String) session.getAttributes().get("challenge");
            if (challenge != null && challenge.equals(auth.getResponse())) {
                // challenge-response认证也通过API Key验证，同样分配服务器ID
                String assignedServerId = serverRegistryService.registerOrGetServerId(
                        requestedServerId, apiKey, serverName, serverType);
                
                // 更新服务器信息
                serverRegistryService.updateServerInfo(assignedServerId, serverName, serverType, auth.getServerVersion());
                
                session.getAttributes().put("authenticated", true);
                session.getAttributes().put("serverId", assignedServerId);
                sessionManager.registerSession(assignedServerId, session);

                AuthPacket response = new AuthPacket();
                response.setSuccess(true);
                response.setMessage("Authenticated successfully");
                response.setToken(generateToken());
                response.setServerId(assignedServerId);

                ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                        PacketType.AUTH_RESPONSE, response);
                sendPacket(session, responsePacket);

                logger.info("Server authenticated via challenge: {}", assignedServerId);
            } else {
                AuthPacket response = new AuthPacket();
                response.setSuccess(false);
                response.setMessage("Authentication failed: invalid API key or challenge response");

                ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                        PacketType.AUTH_RESPONSE, response);
                sendPacket(session, responsePacket);
                session.close();
            }
        }
    }
    
    private void handleHeartbeat(WebSocketSession session, ProtocolPacket packet) throws IOException {
        Map<String, Object> data = packet.getPayload(Map.class);

        ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                PacketType.HEARTBEAT, Map.of("timestamp", System.currentTimeMillis()));
        sendPacket(session, response);
    }
    
    private void handleConfigRequest(WebSocketSession session, ProtocolPacket packet) throws IOException {
        var config = configService.getConfig();

        ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                PacketType.CONFIG_RESPONSE, config);
        sendPacket(session, response);
    }

    private void handlePlayerDataRequest(WebSocketSession session, ProtocolPacket packet) throws IOException {
        Map<String, Object> data = packet.getPayload(Map.class);
        String playerUuidStr = (String) data.get("playerUuid");
        String playerName = (String) data.get("playerName");

        PlayerCredit credit = null;

        if (playerUuidStr != null && !playerUuidStr.isEmpty()) {
            // 通过UUID查询
            UUID playerUuid = UUID.fromString(playerUuidStr);
            credit = playerService.getOrCreatePlayerCredit(playerUuid, 
                    playerName != null ? playerName : "Unknown");
            
            // 填充评分历史（最近20条）
            try {
                List<RatingEntity> ratingEntities = ratingRepository.findByTargetUuidOrderByRatedAtDesc(playerUuid);
                if (ratingEntities != null && !ratingEntities.isEmpty()) {
                    List<RatingInfo> ratingHistory = ratingEntities.stream()
                        .limit(20)
                        .map(entity -> {
                            RatingInfo info = new RatingInfo();
                            info.setRatingId(entity.getRatingId());
                            try {
                                info.setRaterUuid(UUID.fromString(entity.getRaterUuid().toString()));
                            } catch (Exception ignored) {}
                            info.setRaterName(entity.getRaterName());
                            try {
                                info.setTargetUuid(UUID.fromString(entity.getTargetUuid().toString()));
                            } catch (Exception ignored) {}
                            info.setTargetName(entity.getTargetName());
                            info.setScore(entity.getScore());
                            info.setComment(entity.getComment());
                            info.setServerId(entity.getServerId());
                            if (entity.getRatedAt() != null) {
                                info.setRatedAt(java.sql.Timestamp.valueOf(entity.getRatedAt()));
                            }
                            return info;
                        })
                        .collect(Collectors.toList());
                    credit.setRatingHistory(ratingHistory);
                    logger.debug("[PLAYER_DATA] 玩家 {} 评分历史: {} 条", playerUuidStr, ratingHistory.size());
                }
            } catch (Exception e) {
                logger.warn("查询玩家 {} 评分历史失败: {}", playerUuidStr, e.getMessage());
            }
        } else if (playerName != null && !playerName.isEmpty()) {
            // 通过玩家名查询
            var creditOpt = playerService.getPlayerCreditByName(playerName);
            if (creditOpt.isPresent()) {
                credit = creditOpt.get();
            } else {
                // 玩家不存在，返回一个默认的信用数据
                credit = new PlayerCredit();
                credit.setPlayerName(playerName);
                credit.setCreditScore(5.0); // 默认中等信用分
            }
        }

        if (credit != null) {
            ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                    PacketType.PLAYER_DATA_RESPONSE, credit);
            sendPacket(session, response);
        }
    }
    
    private void handleCommandResponse(ProtocolPacket packet) {
        CommandPacket.CommandResponse response = packet.getPayload(CommandPacket.CommandResponse.class);
        String output = response.getOutput() != null ? response.getOutput() : "";
        logger.info("收到命令响应 - commandId: {}, success: {}, output: {}", 
                    response.getCommandId(), response.isSuccess(), output);
        sessionManager.handleCommandResponse(response.getCommandId(), output);
    }

    private void handleRatingSubmit(WebSocketSession session, ProtocolPacket packet) throws IOException {
        try {
            RatingInfo ratingInfo = packet.getPayload(RatingInfo.class);

            // 使用评分服务处理评分（带权重计算）
            ratingService.submitRating(ratingInfo);

            // 获取评分者的权重用于响应
            double weight = ratingService.getRaterWeight(ratingInfo.getRaterUuid());

            // 发送成功响应
            ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                PacketType.RATING_RESPONSE,
                Map.of(
                    "success", true,
                    "message", "Rating submitted successfully",
                    "weight", weight,
                    "raterCreditScore", ratingService.getWeightedAverageRating(ratingInfo.getRaterUuid())
                ));
            sendPacket(session, response);

            logger.info("Rating submitted by {} for {} with score {}, weight: {}",
                ratingInfo.getRaterName(),
                ratingInfo.getTargetName(),
                ratingInfo.getScore(),
                weight);

        } catch (Exception e) {
            logger.error("Error handling rating submit", e);
            ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                PacketType.ERROR,
                Map.of(
                    "success", false,
                    "message", "Failed to submit rating: " + e.getMessage()
                ));
            sendPacket(session, response);
        }
    }

    /**
     * 处理玩家事件
     */
    private void handlePlayerEvent(WebSocketSession session, ProtocolPacket packet) throws IOException {
        try {
            PlayerEventPacket event = packet.getPayload(PlayerEventPacket.class);
            String serverId = (String) session.getAttributes().get("serverId");
            String playerUuidStr = event.getPlayerUuid();
            String playerName = event.getPlayerName();
            String eventType = event.getEventType();

            // 检查serverId是否为null
            if (serverId == null) {
                logger.warn("Received player event from session without serverId: {}", eventType);
                return;
            }
            
            // 检查playerUuid是否为null
            if (playerUuidStr == null) {
                logger.warn("Received player event without playerUuid: {} from {}", eventType, serverId);
                return;
            }

            // 直接在session中更新玩家列表（避免通过serverId查找session可能不匹配的问题）
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> players = (Map<String, Map<String, Object>>) session.getAttributes()
                    .computeIfAbsent("players", k -> new ConcurrentHashMap<String, Map<String, Object>>());

            // 处理退出/踢出事件
            if (PlayerEventPacket.QUIT.equals(eventType) || PlayerEventPacket.KICK.equals(eventType)) {
                players.remove(playerUuidStr);
                logger.info("玩家 {} 离开服务器 {}", playerName, serverId);
            } else {
                // 处理其他事件（加入、移动、聊天等）
                Map<String, Object> playerInfo = new ConcurrentHashMap<>();
                playerInfo.put("uuid", playerUuidStr);
                playerInfo.put("name", playerName);
                playerInfo.put("server", serverId);
                playerInfo.put("status", "online");
                playerInfo.put("lastEvent", eventType);
                playerInfo.put("lastUpdate", System.currentTimeMillis());
                
                // 添加额外数据（如位置、世界等）
                if (event.getData() != null) {
                    playerInfo.putAll(event.getData());
                }
                
                players.put(playerUuidStr, playerInfo);
            }
            
            // 更新玩家计数
            session.getAttributes().put("playerCount", players.size());

            // 检查是否是玩家加入事件
            if (PlayerEventPacket.JOIN.equals(eventType)) {
                try {
                    UUID playerUuid = UUID.fromString(playerUuidStr);
                    
                    // 1. 检查并创建数据库记录（如果不存在）
                    playerService.getOrCreatePlayerCredit(playerUuid, playerName);
                    logger.info("玩家 {} ({}) 加入服务器 {}，已更新数据库", playerName, playerUuidStr, serverId);

                    // 2. 检查玩家是否被数据库封禁（永久封禁）
                    var creditOpt = playerService.getPlayerCreditByName(playerName);
                    if (creditOpt.isPresent() && creditOpt.get().isCurrentlyBanned()) {
                        String kickMessage = "\u00a7c你已被PCS系统封禁！\n\u00a77原因: 被投票封禁\n\u00a77如有疑问请联系管理员";
                        sessionManager.kickPlayer(playerUuid, kickMessage, serverId);
                        logger.info("玩家 {} ({}) 尝试加入服务器 {} 但被数据库封禁阻止", playerName, playerUuidStr, serverId);
                        
                        ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                            PacketType.PLAYER_EVENT, Map.of("success", false, "denied", true, "reason", "BANNED"));
                        sendPacket(session, response);
                        return;
                    }

                    // 3. 检查玩家是否被临时封禁
                    var banOpt = kickBanService.getActiveBan(playerUuid, serverId);
                    if (banOpt.isPresent()) {
                        var ban = banOpt.get();
                        String kickMessage = String.format(
                            "\u00a7c你已被踢出该服务器！\n\u00a77原因: %s\n\u00a77封禁至: %s\n\u00a77操作员: %s",
                            ban.getReason(),
                            ban.getExpireAt().toString().replace("T", " "),
                            ban.getKickedBy() != null ? ban.getKickedBy() : "系统"
                        );
                        sessionManager.kickPlayer(playerUuid, kickMessage, serverId);
                        logger.info("玩家 {} ({}) 尝试加入服务器 {} 但被临时封禁阻止", playerName, playerUuidStr, serverId);
                        
                        ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                            PacketType.PLAYER_EVENT, Map.of("success", false, "denied", true, "reason", "KICK_BANNED"));
                        sendPacket(session, response);
                        return;
                    }
                    
                } catch (IllegalArgumentException e) {
                    // 可能是离线玩家，使用离线玩家UUID格式
                    logger.warn("玩家 {} 的UUID格式不正确，尝试作为离线玩家处理: {}", playerName, playerUuidStr);
                    playerService.getOrCreateOfflinePlayerCredit(playerName);
                }
            }

            logger.debug("Player event received: {} - {} ({}) from {}", eventType, playerName, playerUuidStr, serverId);

            // 发送确认响应
            ProtocolPacket response = ProtocolPacket.response(packet.getRequestId(),
                PacketType.PLAYER_EVENT, Map.of("success", true, "received", true));
            sendPacket(session, response);

        } catch (Exception e) {
            logger.error("Error handling player event", e);
        }
    }

    /**
     * 处理状态报告
     */
    private void handleStatusReport(WebSocketSession session, ProtocolPacket packet) throws IOException {
        try {
            StatusPacket.StatusReport report = packet.getPayload(StatusPacket.StatusReport.class);
            String serverId = (String) session.getAttributes().get("serverId");

            // 更新服务器状态
            sessionManager.updateServerStatus(serverId, report);

            // 发送响应
            StatusPacket.StatusResponse response = new StatusPacket.StatusResponse();
            response.setSuccess(true);
            response.setMessage("Status received");

            ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                PacketType.STATUS_RESPONSE, response);
            sendPacket(session, responsePacket);

            logger.debug("Status report received from {}: TPS={}, Players={}",
                serverId, report.getTps(), report.getOnlinePlayers());

        } catch (Exception e) {
            logger.error("Error handling status report", e);
        }
    }

    private void sendError(WebSocketSession session, String requestId, String message) throws IOException {
        ProtocolPacket error = ProtocolPacket.response(requestId, PacketType.ERROR,
                Map.of("error", message));
        sendPacket(session, error);
    }

    /**
     * 处理解除封禁请求
     */
    private void handleUnbanRequest(WebSocketSession session, ProtocolPacket packet) throws IOException {
        try {
            UnbanPacket.UnbanRequest request = packet.getPayload(UnbanPacket.UnbanRequest.class);
            String serverId = (String) session.getAttributes().get("serverId");
            
            if (request.getTargetUuid() == null || request.getTargetName() == null) {
                UnbanPacket.UnbanResponse response = new UnbanPacket.UnbanResponse(
                    false, "缺少必要参数", request.getTargetUuid(), request.getTargetName());
                ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                        PacketType.UNBAN_RESPONSE, response);
                sendPacket(session, responsePacket);
                return;
            }

            UUID targetUuid = UUID.fromString(request.getTargetUuid());
            
            // 调用服务解除封禁
            boolean success = kickBanService.unbanPlayerFromServer(targetUuid, serverId);
            
            // 同时解除数据库中的永久封禁
            playerService.unbanPlayer(targetUuid);
            
            String message = success 
                ? "玩家 " + request.getTargetName() + " 的封禁已解除" 
                : "玩家 " + request.getTargetName() + " 在该服务器上没有有效的封禁记录";
            
            UnbanPacket.UnbanResponse response = new UnbanPacket.UnbanResponse(
                success, message, request.getTargetUuid(), request.getTargetName());
            
            ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                    PacketType.UNBAN_RESPONSE, response);
            sendPacket(session, responsePacket);
            
            // 广播解封通知给所有服务器
            if (success) {
                sessionManager.broadcastUnban(targetUuid, request.getTargetName(), serverId);
            }
            
            logger.info("玩家 {} ({}) 的封禁已被 {} 在服务器 {} 解除", 
                request.getTargetName(), targetUuid, request.getOperator(), serverId);
                
        } catch (Exception e) {
            logger.error("处理解除封禁请求时出错", e);
            UnbanPacket.UnbanResponse response = new UnbanPacket.UnbanResponse(
                false, "处理请求时出错: " + e.getMessage(), null, null);
            ProtocolPacket responsePacket = ProtocolPacket.response(packet.getRequestId(),
                    PacketType.UNBAN_RESPONSE, response);
            sendPacket(session, responsePacket);
        }
    }
    
    /**
     * 处理旧封禁同步请求
     * 当服务器装载PCS后，同步之前已有的封禁列表
     */
    private void handleLegacyBanSync(WebSocketSession session, ProtocolPacket packet) {
        try {
            Map<String, Object> payload = packet.getPayload(Map.class);
            String serverId = (String) session.getAttributes().get("serverId");
            
            String playerUuidStr = (String) payload.get("playerUuid");
            String playerName = (String) payload.get("playerName");
            String reason = (String) payload.get("reason");
            Number expirationNum = (Number) payload.get("expiration");
            
            if (playerUuidStr == null || playerName == null) {
                logger.warn("收到无效的旧封禁同步请求: 缺少玩家信息");
                return;
            }
            
            UUID playerUuid = UUID.fromString(playerUuidStr);
            long expirationTime = expirationNum != null ? expirationNum.longValue() : 0;
            
            // 将旧封禁记录到数据库
            playerService.recordLegacyBan(playerUuid, playerName, reason, 
                expirationTime > 0 ? new java.util.Date(expirationTime) : null, serverId);
            
            logger.info("已同步旧封禁: {} ({}) 来自服务器 {}", playerName, playerUuidStr, serverId);
            
        } catch (Exception e) {
            logger.error("处理旧封禁同步请求时出错", e);
        }
    }
    
    /**
     * 处理封禁同步 - 广播到所有服务器
     */
    private void handleBanSync(WebSocketSession session, ProtocolPacket packet) {
        try {
            Map<String, Object> payload = packet.getPayload(Map.class);
            String senderServerId = (String) session.getAttributes().get("serverId");
            
            // 检查是否需要广播到所有服务器
            Boolean broadcastAll = (Boolean) payload.get("broadcastAll");
            if (Boolean.TRUE.equals(broadcastAll)) {
                // 广播到所有服务器（除了发送者）
                sessionManager.broadcastToAllExcept(packet, senderServerId);
                logger.info("封禁同步已广播到所有服务器 (来自: {})", senderServerId);
            }
        } catch (Exception e) {
            logger.error("处理封禁同步广播时出错", e);
        }
    }
    
    /**
     * 处理禁言 - 广播到所有服务器
     */
    private void handleMutePlayer(WebSocketSession session, ProtocolPacket packet) {
        try {
            Map<String, Object> payload = packet.getPayload(Map.class);
            String senderServerId = (String) session.getAttributes().get("serverId");
            
            // 检查是否需要广播到所有服务器
            Boolean broadcastAll = (Boolean) payload.get("broadcastAll");
            if (Boolean.TRUE.equals(broadcastAll)) {
                // 广播到所有服务器（除了发送者）
                sessionManager.broadcastToAllExcept(packet, senderServerId);
                logger.info("禁言已广播到所有服务器 (来自: {})", senderServerId);
            }
        } catch (Exception e) {
            logger.error("处理禁言广播时出错", e);
        }
    }
    
    /**
     * 处理踢出玩家 - 广播到所有服务器
     */
    private void handleKickPlayer(WebSocketSession session, ProtocolPacket packet) {
        try {
            Map<String, Object> payload = packet.getPayload(Map.class);
            String senderServerId = (String) session.getAttributes().get("serverId");
            
            // 检查是否需要广播到所有服务器
            Boolean broadcastAll = (Boolean) payload.get("broadcastAll");
            if (Boolean.TRUE.equals(broadcastAll)) {
                // 广播到所有服务器（除了发送者）
                sessionManager.broadcastToAllExcept(packet, senderServerId);
                logger.info("踢出已广播到所有服务器 (来自: {})", senderServerId);
            }
        } catch (Exception e) {
            logger.error("处理踢出广播时出错", e);
        }
    }
    
    private String generateChallenge() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}
