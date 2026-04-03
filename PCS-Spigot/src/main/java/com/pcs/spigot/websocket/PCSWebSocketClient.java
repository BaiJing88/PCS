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

package com.pcs.spigot.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pcs.api.model.PCSConfig;
import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.VoteSession;
import com.pcs.api.protocol.AuthPacket;
import com.pcs.api.protocol.CommandPacket;
import com.pcs.api.protocol.PacketType;
import com.pcs.api.protocol.ProtocolPacket;
import com.pcs.api.protocol.ServerControlPacket;
import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.manager.ReconnectManager;
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * WebSocket客户端
 * 用于与中央控制器通信
 */
public class PCSWebSocketClient extends WebSocketClient {
    
    private final PCSSpigotPlugin plugin;
    private final Gson gson;
    private final ScheduledExecutorService heartbeatExecutor;
    private final ReconnectManager reconnectManager;

    private boolean authenticated = false;
    private String authToken;
    
    public PCSWebSocketClient(PCSSpigotPlugin plugin) {
        super(createURI(plugin));
        this.plugin = plugin;
        this.gson = new Gson();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        this.reconnectManager = new ReconnectManager(plugin);

        // 设置连接超时
        setConnectionLostTimeout(60);
    }
    
    private static URI createURI(PCSSpigotPlugin plugin) {
        try {
            return new URI(plugin.getConfigManager().getWebSocketUrl());
        } catch (URISyntaxException e) {
            plugin.getLogger().log(Level.SEVERE, "WebSocket URL格式错误!", e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        plugin.getLogger().info("WebSocket连接已建立，正在认证...");
        authenticated = false;

        // 重置重连计数器
        reconnectManager.resetAttempts();

        // 发送认证请求
        sendAuthentication();

        // 启动心跳
        startHeartbeat();
    }
    
    @Override
    public void onMessage(String message) {
        try {
            ProtocolPacket packet = ProtocolPacket.fromJson(message);
            PacketType type = PacketType.fromString(packet.getType());
            
            if (type == null) {
                plugin.getLogger().warning("收到未知类型的数据包: " + packet.getType());
                return;
            }
            
            switch (type) {
                case AUTH_RESPONSE:
                    handleAuthResponse(packet);
                    break;
                    
                case CONFIG_RESPONSE:
                case CONFIG_UPDATE:
                    handleConfigUpdate(packet);
                    break;
                    
                case PLAYER_DATA_RESPONSE:
                    handlePlayerDataResponse(packet);
                    break;
                    
                case BAN_NOTIFY:
                    handleBanNotify(packet);
                    break;
                    
                case UNBAN_NOTIFY:
                    handleUnbanNotify(packet);
                    break;

                case BAN_SYNC:
                    handleBanSync(packet);
                    break;

                case BROADCAST:
                    handleBroadcast(packet);
                    break;

                case REMOTE_COMMAND:
                    handleRemoteCommand(packet);
                    break;

                case SERVER_CONTROL:
                    handleServerControl(packet);
                    break;

                case KICK_PLAYER:
                    handleKickPlayer(packet);
                    break;

                case MUTE_PLAYER:
                    handleMutePlayer(packet);
                    break;

                case UNMUTE_PLAYER:
                    handleUnmutePlayer(packet);
                    break;

                case PRIVATE_MESSAGE:
                    handlePrivateMessage(packet);
                    break;

                case ERROR:
                    handleError(packet);
                    break;
                    
                case RATING_UPDATE:
                    handleRatingUpdate(packet);
                    break;

                default:
                    plugin.getLogger().fine("收到未处理的数据包类型: " + type);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "处理WebSocket消息时出错", e);
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        plugin.getLogger().info("WebSocket连接已关闭: " + reason + " (code: " + code + ")");
        authenticated = false;
        
        // 停止心跳
        stopHeartbeat();
        
        // 自动重连（只在是活跃实例且被远程关闭时）
        if (remote && plugin.getWebSocketClient() == this) {
            reconnectManager.recordFailedReconnect();
            scheduleReconnect();
        }
    }
    
    @Override
    public void onError(Exception ex) {
        plugin.getLogger().log(Level.WARNING, "WebSocket错误", ex);
    }
    
    /**
     * 发送认证请求
     */
    private void sendAuthentication() {
        AuthPacket auth = new AuthPacket();
        auth.setServerId(plugin.getConfigManager().getServerId());
        auth.setServerName(plugin.getConfigManager().getServerName());
        auth.setServerType(plugin.getConfigManager().getServerType());
        auth.setServerVersion(Bukkit.getVersion());
        auth.setApiKey(plugin.getConfigManager().getApiKey());
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.AUTH_REQUEST, auth);
        send(packet.toJson());
    }
    
    /**
     * 处理认证响应
     */
    private void handleAuthResponse(ProtocolPacket packet) {
        AuthPacket response = packet.getPayload(AuthPacket.class);
        
        if (response.isSuccess()) {
            authenticated = true;
            authToken = response.getToken();
            
            // 【重要】保存中控分配的服务器ID
            String assignedServerId = response.getServerId();
            String currentServerId = plugin.getConfigManager().getServerId();
            
            if (assignedServerId != null && !assignedServerId.equals(currentServerId)) {
                // 中控分配了新的ID，保存到配置文件
                plugin.getConfig().set("server.id", assignedServerId);
                plugin.saveConfig();
                plugin.getLogger().info("==============================================");
                plugin.getLogger().info("中控分配了新的服务器ID: " + assignedServerId);
                plugin.getLogger().info("已保存到配置文件 plugins/PCS-Spigot/config.yml");
                plugin.getLogger().info("==============================================");
            } else {
                plugin.getLogger().info("认证成功！服务器ID: " + currentServerId);
            }
            
            // 请求配置
            requestConfig();
        } else {
            plugin.getLogger().warning("认证失败: " + response.getMessage());
        }
    }
    
    /**
     * 处理配置更新
     */
    private void handleConfigUpdate(ProtocolPacket packet) {
        PCSConfig config = packet.getPayload(PCSConfig.class);
        plugin.getConfigManager().updateRemoteConfig(config);
        plugin.getLogger().info("配置已更新");
    }
    
    /**
     * 处理玩家数据响应
     */
    private void handlePlayerDataResponse(ProtocolPacket packet) {
        PlayerCredit credit = packet.getPayload(PlayerCredit.class);
        if (credit != null && credit.getPlayerUuid() != null) {
            plugin.getPlayerDataManager().updatePlayerCredit(credit);
            plugin.getLogger().fine("玩家数据已更新: " + credit.getPlayerName());
        }
    }
    
    /**
     * 处理封禁通知
     */
    private void handleBanNotify(ProtocolPacket packet) {
        if (!plugin.getConfigManager().isBanSyncEnabled()) {
            return;
        }
        
        JsonObject payload = packet.getPayload(JsonObject.class);
        String playerName = payload.get("playerName").getAsString();
        String reason = payload.get("reason").getAsString();
        String sourceServer = payload.has("sourceServer") ? 
            payload.get("sourceServer").getAsString() : "unknown";
        
        // 执行封禁
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer != null) {
            plugin.getPlayerDataManager().banPlayer(offlinePlayer.getUniqueId(), reason);
            
            MessageUtil.broadcast("§c玩家 §e" + playerName + " §c已被从 §e" + sourceServer + " §c同步封禁！");
        }
    }
    
    /**
     * 处理解封通知
     */
    private void handleUnbanNotify(ProtocolPacket packet) {
        if (!plugin.getConfigManager().isBanSyncEnabled()) {
            return;
        }

        JsonObject payload = packet.getPayload(JsonObject.class);
        String targetUuid = payload.has("targetUuid") ? payload.get("targetUuid").getAsString() : null;
        String targetName = payload.has("targetName") ? payload.get("targetName").getAsString() : null;

        if (targetUuid == null) {
            return;
        }

        // 处理OFFLINE前缀（通过玩家名发送的指令）
        final UUID uuid;
        final String playerName;
        
        if (targetUuid.startsWith("OFFLINE:")) {
            // 对于离线账号，尝试查找在线玩家获取真实UUID
            String name = targetUuid.substring(8);
            Player onlinePlayer = Bukkit.getPlayerExact(name);
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
                playerName = onlinePlayer.getName();
            } else {
                // 玩家不在线，无法通过UUID查找，跳过
                plugin.getLogger().fine("UNBAN_NOTIFY目标玩家不在线，跳过: " + name);
                return;
            }
        } else {
            try {
                uuid = UUID.fromString(targetUuid);
                playerName = targetName != null ? targetName : targetUuid.substring(0, 8);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UNBAN_NOTIFY无效的UUID格式: " + targetUuid);
                return;
            }
        }

        // 在主线程执行解封操作
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getPlayerDataManager().unbanPlayer(uuid);
            MessageUtil.broadcast("§a玩家 §e" + playerName + " §a已被解封！");
        });
    }

    /**
     * 处理封禁同步（执行KICK/BAN/MUTE）
     */
    private void handleBanSync(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);

        String targetUuid = payload.has("targetUuid") ? payload.get("targetUuid").getAsString() : null;
        String targetName = payload.has("targetName") ? payload.get("targetName").getAsString() : null;
        String action = payload.has("action") ? payload.get("action").getAsString() : "KICK";
        String reason = payload.has("reason") ? payload.get("reason").getAsString() : "违反服务器规则";
        int durationDays = payload.has("durationDays") ? payload.get("durationDays").getAsInt() : 7;

        if (targetUuid == null) {
            return;
        }

        // 处理OFFLINE前缀（通过玩家名发送的指令）
        final UUID uuid;
        final String playerName;
        Player onlinePlayer = null;
        
        if (targetUuid.startsWith("OFFLINE:")) {
            String name = targetUuid.substring(8);
            onlinePlayer = Bukkit.getPlayerExact(name);
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
                playerName = onlinePlayer.getName();
            } else {
                plugin.getLogger().warning("BAN_SYNC目标玩家不在线: " + name);
                return;
            }
        } else {
            try {
                uuid = UUID.fromString(targetUuid);
                onlinePlayer = Bukkit.getPlayer(uuid);
                playerName = onlinePlayer != null ? onlinePlayer.getName() : targetName;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("BAN_SYNC无效的UUID格式: " + targetUuid);
                return;
            }
        }

        final Player player = onlinePlayer;

        // 在主线程执行Bukkit操作
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (action.toUpperCase()) {
                case "KICK":
                    if (player != null && player.isOnline()) {
                        player.kickPlayer("§c你已被投票踢出！\n§e原因: " + reason);
                    }
                    break;
                case "BAN":
                    plugin.getPlayerDataManager().banPlayer(uuid, reason);
                    if (player != null && player.isOnline()) {
                        player.kickPlayer("§c你已被封禁！\n§e原因: " + reason);
                    }
                    break;
                case "MUTE":
                    // 计算禁言结束时间
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_YEAR, durationDays);
                    Date muteEndTime = calendar.getTime();

                    // 添加到禁言管理器
                    plugin.getMuteManager().mutePlayer(uuid, reason, muteEndTime);

                    if (player != null && player.isOnline()) {
                        player.sendMessage("§c§l[PCS] §e你已被投票禁言 " + durationDays + " 天！");
                        player.sendMessage("§7原因: " + reason);
                    }

                    String displayName = player != null ? player.getName() : (playerName != null ? playerName : uuid.toString().substring(0, 8));
                    MessageUtil.broadcast("§6[PCS] §c玩家 " + displayName + " 已被禁言 " + durationDays + " 天");
                    break;
            }
        });
    }

    /**
     * 处理广播消息
     */
    private void handleBroadcast(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);
        String message = payload.get("message").getAsString();
        boolean useTitle = payload.has("useTitle") && payload.get("useTitle").getAsBoolean();

        if (useTitle && payload.has("titleFadeIn")) {
            int fadeIn = payload.get("titleFadeIn").getAsInt();
            int stay = payload.get("titleStay").getAsInt();
            int fadeOut = payload.get("titleFadeOut").getAsInt();

            for (Player player : Bukkit.getOnlinePlayers()) {
                MessageUtil.sendTitle(player, message, "", fadeIn, stay, fadeOut);
            }
        } else {
            MessageUtil.broadcast(message);
        }
    }

    /**
     * 处理远程命令
     * 通过读日志文件增量来捕获命令输出，兼容Paper（Log4j2）和Spigot（JUL）
     */
    private void handleRemoteCommand(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);

        String commandId = payload.has("commandId") ? payload.get("commandId").getAsString() : null;
        String command = payload.has("command") ? payload.get("command").getAsString() : null;
        String executedBy = payload.has("executedBy") ? payload.get("executedBy").getAsString() : "Unknown";

        if (commandId == null || command == null) {
            return;
        }

        // 在主线程上执行命令
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                String cmdToExecute = command;
                if (cmdToExecute.startsWith("/")) {
                    cmdToExecute = cmdToExecute.substring(1);
                }
                
                plugin.getLogger().info("[RemoteCommand] Executing: " + cmdToExecute + " (by " + executedBy + ")");
                
                ConsoleCommandSender console = Bukkit.getConsoleSender();
                
                // 记录日志文件当前读取位置
                java.io.RandomAccessFile logFile = null;
                long logPos = -1;
                try {
                    logFile = new java.io.RandomAccessFile(
                        new java.io.File("logs", "latest.log"), "r");
                    logPos = logFile.length();
                } catch (Exception e) {
                    // 日志文件不可读，忽略
                }
                
                boolean success = Bukkit.dispatchCommand(console, cmdToExecute);
                
                // 读取增量日志作为输出
                StringBuilder output = new StringBuilder();
                if (logFile != null && logPos >= 0) {
                    try {
                        logFile.seek(logPos);
                        String line;
                        while ((line = logFile.readLine()) != null) {
                            // 去掉时间戳前缀 [HH:mm:ss] [thread/LEVEL]:
                            String trimmed = line.trim();
                            // 过滤掉PCS自身的日志
                            if (trimmed.contains("[PCS-Spigot]")) continue;
                            // 去掉日志格式前缀，提取实际内容
                            // 格式: [HH:mm:ss] [Thread/LEVEL]: message
                            int msgStart = trimmed.indexOf("]: ");
                            if (msgStart >= 0) {
                                trimmed = trimmed.substring(msgStart + 3).trim();
                            }
                            if (!trimmed.isEmpty()) {
                                output.append(trimmed).append("\n");
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        try { logFile.close(); } catch (Exception ignored) {}
                    }
                }
                
                String error = null;
                if (!success && output.length() == 0) {
                    error = "命令执行失败或命令不存在";
                    plugin.getLogger().warning("[RemoteCommand] Command failed: " + cmdToExecute);
                } else {
                    plugin.getLogger().info("[RemoteCommand] Success: " + cmdToExecute);
                }

                sendCommandResponse(commandId, success, output.toString().trim(), error);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[RemoteCommand] Error executing command", e);
                sendCommandResponse(commandId, false, "", e.getMessage());
            }
        });
    }

    /**
     * 通过添加临时Handler捕获Bukkit Logger输出的辅助类
     */
    private class LoggerCapturer {
        private final java.util.logging.Handler handler;
        private final StringBuilder captured = new StringBuilder();
        
        LoggerCapturer() {
            handler = new java.util.logging.Handler() {
                @Override
                public void publish(java.util.logging.LogRecord record) {
                    String msg = record.getMessage();
                    if (msg != null) {
                        captured.append(msg).append("\n");
                    }
                }
                
                @Override
                public void flush() {}
                
                @Override
                public void close() throws SecurityException {}
            };
            handler.setLevel(java.util.logging.Level.ALL);
            Bukkit.getLogger().addHandler(handler);
        }
        
        String stop() {
            Bukkit.getLogger().removeHandler(handler);
            return captured.toString();
        }
    }

    /**
     * 发送命令响应
     */
    private void sendCommandResponse(String commandId, boolean success, String output, String error) {
        if (!isOpen() || !authenticated) {
            plugin.getLogger().warning("无法发送命令响应: WebSocket未连接或未认证");
            return;
        }

        CommandPacket.CommandResponse response = new CommandPacket.CommandResponse();
        response.setCommandId(commandId);
        response.setServerId(plugin.getConfigManager().getServerId());
        response.setSuccess(success);
        response.setOutput(output != null ? output : "");
        response.setError(error);
        response.setExecutionTime(System.currentTimeMillis());

        ProtocolPacket packet = ProtocolPacket.request(PacketType.REMOTE_COMMAND_RESPONSE, response);
        send(packet.toJson());
    }

    /**
     * 处理错误
     */
    private void handleError(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);
        String errorMessage = payload.has("message") ? payload.get("message").getAsString() : "未知错误";
        plugin.getLogger().warning("中控服务器错误: " + errorMessage);
    }
    
    /**
     * 处理评分更新推送
     * 当其他玩家给某玩家评分后，中控会推送更新
     */
    private void handleRatingUpdate(ProtocolPacket packet) {
        try {
            JsonObject payload = packet.getPayload(JsonObject.class);
            
            String playerUuidStr = payload.has("playerUuid") ? payload.get("playerUuid").getAsString() : null;
            String playerName = payload.has("playerName") ? payload.get("playerName").getAsString() : "Unknown";
            double creditScore = payload.has("creditScore") ? payload.get("creditScore").getAsDouble() : 5.0;
            int banCount = payload.has("banCount") ? payload.get("banCount").getAsInt() : 0;
            int kickCount = payload.has("kickCount") ? payload.get("kickCount").getAsInt() : 0;
            boolean currentlyBanned = payload.has("currentlyBanned") && payload.get("currentlyBanned").getAsBoolean();
            boolean cheater = payload.has("cheater") && payload.get("cheater").getAsBoolean();
            
            if (playerUuidStr == null) {
                plugin.getLogger().warning("收到评分更新但缺少玩家UUID");
                return;
            }
            
            // 处理OFFLINE前缀
            UUID playerUuid;
            if (playerUuidStr.startsWith("OFFLINE:")) {
                String name = playerUuidStr.substring(8);
                Player onlinePlayer = Bukkit.getPlayerExact(name);
                if (onlinePlayer != null) {
                    playerUuid = onlinePlayer.getUniqueId();
                    playerName = onlinePlayer.getName();
                } else {
                    plugin.getLogger().fine("评分更新目标玩家不在线，跳过: " + name);
                    return;
                }
            } else {
                try {
                    playerUuid = UUID.fromString(playerUuidStr);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("评分更新无效的UUID格式: " + playerUuidStr);
                    return;
                }
            }
            
            // 更新本地缓存的玩家信用数据
            PlayerCredit credit = plugin.getPlayerDataManager().getPlayerCredit(playerUuid, playerName);
            credit.setCreditScore(creditScore);
            credit.setTotalBans(banCount);
            credit.setTotalKicks(kickCount);
            credit.setCurrentlyBanned(currentlyBanned);
            credit.setCheaterTag(cheater);
            
            plugin.getPlayerDataManager().getPlayerCredits().put(playerUuid, credit);
            
            plugin.getLogger().fine("玩家 " + playerName + " 的信用分已更新为 " + String.format("%.2f", creditScore));
            
            // 如果该玩家在线，发送提示
            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                // 根据信用分显示不同颜色的消息
                String color;
                if (creditScore >= 8) color = "§a";
                else if (creditScore >= 5) color = "§e";
                else if (creditScore >= 3) color = "§6";
                else color = "§c";
                
                onlinePlayer.sendMessage("§6[PCS] §7你的信用分已更新: " + color + String.format("%.2f", creditScore) + "§7分");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "处理评分更新时出错", e);
        }
    }

    /**
     * 处理踢出玩家指令
     */
    private void handleKickPlayer(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);

        String targetUuid = payload.has("playerUuid") ? payload.get("playerUuid").getAsString() : null;
        String playerName = payload.has("playerName") ? payload.get("playerName").getAsString() : null;
        String reason = payload.has("reason") ? payload.get("reason").getAsString() : "管理员踢出";

        if (targetUuid == null) {
            plugin.getLogger().warning("收到踢出指令但缺少玩家UUID");
            return;
        }

        // 检查是否是通过玩家名发送的踢人指令（OFFLINE前缀）
        Player targetPlayer = null;
        if (targetUuid.startsWith("OFFLINE:")) {
            // 通过玩家名查找
            String name = targetUuid.substring(8);
            targetPlayer = Bukkit.getPlayerExact(name);
            if (targetPlayer == null) {
                plugin.getLogger().warning("踢出玩家失败: 玩家不在线 (" + name + ")");
                return;
            }
        } else {
            // 通过UUID查找
            try {
                UUID uuid = UUID.fromString(targetUuid);
                targetPlayer = Bukkit.getPlayer(uuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("踢出玩家失败: 无效的UUID格式 (" + targetUuid + ")");
                return;
            }
        }

        final Player playerToKick = targetPlayer;
        final String finalReason = reason;

        // 在主线程执行踢出操作
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (playerToKick != null && playerToKick.isOnline()) {
                playerToKick.kickPlayer("\u00a7c你已被踢出服务器！\n\u00a77原因: " + finalReason);
                plugin.getLogger().info("已踢出玩家: " + playerToKick.getName() + "), 原因: " + finalReason);
            } else {
                plugin.getLogger().warning("踢出玩家失败: 玩家已离线");
            }
        });
    }

    /**
     * 处理禁言指令
     */
    private void handleMutePlayer(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);

        String targetUuid = payload.has("targetUuid") ? payload.get("targetUuid").getAsString() : null;
        String targetName = payload.has("targetName") ? payload.get("targetName").getAsString() : null;
        String reason = payload.has("reason") ? payload.get("reason").getAsString() : "管理员禁言";
        int durationDays = payload.has("durationDays") ? payload.get("durationDays").getAsInt() : 7;

        if (targetUuid == null) {
            plugin.getLogger().warning("收到禁言指令但缺少玩家UUID");
            return;
        }

        // 检查是否是通过玩家名发送的禁言（OFFLINE前缀）
        final UUID uuid;
        final String playerNameLookup;
        if (targetUuid.startsWith("OFFLINE:")) {
            // 通过玩家名查找
            String name = targetUuid.substring(8);
            Player onlinePlayer = Bukkit.getPlayerExact(name);
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
                playerNameLookup = onlinePlayer.getName();
                plugin.getLogger().info("禁言离线账号，但在本服务器找到在线玩家: " + name + " -> " + uuid);
            } else {
                // 玩家不在线，无法禁言（可以扩展为支持离线禁言）
                plugin.getLogger().warning("禁言指令目标玩家不在线: " + name);
                return;
            }
        } else {
            uuid = UUID.fromString(targetUuid);
            playerNameLookup = targetName;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Player player = Bukkit.getPlayer(uuid);

                // 计算禁言结束时间
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, durationDays);
                Date muteEndTime = calendar.getTime();

                // 添加到禁言管理器
                plugin.getMuteManager().mutePlayer(uuid, reason, muteEndTime);

                if (player != null && player.isOnline()) {
                    player.sendMessage("§c§l[PCS] §e你已被管理员禁言 " + durationDays + " 天！");
                    player.sendMessage("§7原因: " + reason);
                }

                String playerName = player != null ? player.getName() : (playerNameLookup != null ? playerNameLookup : uuid.toString().substring(0, 8));
                MessageUtil.broadcast("§6[PCS] §c玩家 " + playerName + " 已被禁言 " + durationDays + " 天");
                plugin.getLogger().info("已禁言玩家: " + playerName + " (" + uuid + "), 天数: " + durationDays + ", 原因: " + reason);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "禁言玩家时出错", e);
            }
        });
    }

    /**
     * 处理解除禁言指令
     */
    private void handleUnmutePlayer(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);

        String targetUuid = payload.has("targetUuid") ? payload.get("targetUuid").getAsString() : null;
        String targetName = payload.has("targetName") ? payload.get("targetName").getAsString() : null;

        if (targetUuid == null) {
            plugin.getLogger().warning("收到解禁言指令但缺少玩家UUID");
            return;
        }

        // 处理OFFLINE前缀（通过玩家名发送的指令）
        final UUID uuid;
        final String playerNameLookup;
        if (targetUuid.startsWith("OFFLINE:")) {
            // 通过玩家名查找
            String name = targetUuid.substring(8);
            Player onlinePlayer = Bukkit.getPlayerExact(name);
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
                playerNameLookup = onlinePlayer.getName();
                plugin.getLogger().info("解禁言离线账号，但在本服务器找到在线玩家: " + name + " -> " + uuid);
            } else {
                // 玩家不在线，无法解禁言
                plugin.getLogger().warning("解禁言指令目标玩家不在线: " + name);
                return;
            }
        } else {
            try {
                uuid = UUID.fromString(targetUuid);
                playerNameLookup = targetName;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("解禁言指令无效的UUID格式: " + targetUuid);
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Player player = Bukkit.getPlayer(uuid);

                // 从禁言管理器移除
                plugin.getMuteManager().unmutePlayer(uuid);

                if (player != null && player.isOnline()) {
                    player.sendMessage("§a§l[PCS] §a你已被解除禁言！");
                }

                String playerName = player != null ? player.getName() : (playerNameLookup != null ? playerNameLookup : uuid.toString().substring(0, 8));
                MessageUtil.broadcast("§6[PCS] §a玩家 " + playerName + " 已被解除禁言");
                plugin.getLogger().info("已解禁言玩家: " + playerName + " (" + uuid + ")");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "解禁言玩家时出错", e);
            }
        });
    }

    /**
     * 处理私信指令
     */
    private void handlePrivateMessage(ProtocolPacket packet) {
        JsonObject payload = packet.getPayload(JsonObject.class);

        String targetUuid = payload.has("playerUuid") ? payload.get("playerUuid").getAsString() : null;
        String message = payload.has("message") ? payload.get("message").getAsString() : null;

        if (targetUuid == null || message == null) {
            plugin.getLogger().warning("收到私信指令但缺少必要参数");
            return;
        }

        // 处理OFFLINE前缀
        final UUID uuid;
        if (targetUuid.startsWith("OFFLINE:")) {
            String name = targetUuid.substring(8);
            Player onlinePlayer = Bukkit.getPlayerExact(name);
            if (onlinePlayer != null) {
                uuid = onlinePlayer.getUniqueId();
            } else {
                plugin.getLogger().warning("发送私信失败: 玩家不在线 (" + name + ")");
                return;
            }
        } else {
            try {
                uuid = UUID.fromString(targetUuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("发送私信失败: 无效的UUID格式 (" + targetUuid + ")");
                return;
            }
        }

        // 在主线程发送私信
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Player player = Bukkit.getPlayer(uuid);

                if (player != null && player.isOnline()) {
                    player.sendMessage("\u00a76[\u00a7l系统私信\u00a76] \u00a7f" + message);
                    plugin.getLogger().info("已发送私信给玩家: " + player.getName());
                } else {
                    plugin.getLogger().warning("发送私信失败: 玩家不在线 (" + uuid + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "发送私信时出错", e);
            }
        });
    }

    /**
     * 处理服务器控制命令
     */
    private void handleServerControl(ProtocolPacket packet) {
        ServerControlPacket.ControlRequest request = packet.getPayload(ServerControlPacket.ControlRequest.class);

        String commandId = request.getCommandId();
        String commandType = request.getCommandType();

        switch (commandType) {
            case ServerControlPacket.RESTART:
                sendControlResponse(commandId, true, "服务器将在5秒后重启", null);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.shutdown();
                }, 100L);
                break;

            case ServerControlPacket.RELOAD_CONFIG:
                plugin.reloadConfig();
                plugin.getConfigManager().loadConfig();
                sendControlResponse(commandId, true, "配置已重载", null);
                break;

            case ServerControlPacket.SHUTDOWN:
                sendControlResponse(commandId, true, "服务器正在关闭", null);
                Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L);
                break;

            case ServerControlPacket.BROADCAST_TITLE:
                handleBroadcastTitle(commandId, request.getParams());
                break;

            case ServerControlPacket.EXECUTE_AS_PLAYER:
                handleExecuteAsPlayer(commandId, request.getParams());
                break;

            case ServerControlPacket.GET_PLAYER_LIST:
                handleGetPlayerList(commandId);
                break;

            case ServerControlPacket.GET_WORLD_INFO:
                handleGetWorldInfo(commandId);
                break;

            default:
                sendControlResponse(commandId, false, null, "未知命令类型: " + commandType);
        }
    }

    /**
     * 处理标题广播
     */
    private void handleBroadcastTitle(String commandId, Object paramsObj) {
        try {
            ServerControlPacket.TitleParams params = gson.fromJson(gson.toJson(paramsObj), ServerControlPacket.TitleParams.class);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(
                    params.getTitle() != null ? params.getTitle() : "",
                    params.getSubtitle() != null ? params.getSubtitle() : "",
                    params.getFadeIn(),
                    params.getStay(),
                    params.getFadeOut()
                );
            }

            sendControlResponse(commandId, true, "标题已广播给 " + Bukkit.getOnlinePlayers().size() + " 名玩家", null);
        } catch (Exception e) {
            sendControlResponse(commandId, false, null, "广播标题失败: " + e.getMessage());
        }
    }

    /**
     * 处理以玩家身份执行命令
     */
    private void handleExecuteAsPlayer(String commandId, Object paramsObj) {
        try {
            ServerControlPacket.ExecuteAsPlayerParams params = gson.fromJson(gson.toJson(paramsObj), ServerControlPacket.ExecuteAsPlayerParams.class);

            String playerUuidStr = params.getPlayerUuid();
            Player targetPlayer;
            
            // 处理OFFLINE前缀
            if (playerUuidStr.startsWith("OFFLINE:")) {
                String name = playerUuidStr.substring(8);
                targetPlayer = Bukkit.getPlayerExact(name);
            } else {
                try {
                    targetPlayer = Bukkit.getPlayer(java.util.UUID.fromString(playerUuidStr));
                } catch (IllegalArgumentException e) {
                    sendControlResponse(commandId, false, null, "无效的UUID格式");
                    return;
                }
            }
            
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sendControlResponse(commandId, false, null, "玩家不在线");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean success = targetPlayer.performCommand(params.getCommand());
                sendControlResponse(commandId, success, "命令执行" + (success ? "成功" : "失败"), null);
            });
        } catch (Exception e) {
            sendControlResponse(commandId, false, null, "执行命令失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取玩家列表
     */
    private void handleGetPlayerList(String commandId) {
        java.util.List<java.util.Map<String, Object>> players = new java.util.ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("uuid", player.getUniqueId().toString());
            info.put("name", player.getName());
            info.put("displayName", player.getDisplayName());
            info.put("world", player.getWorld().getName());
            info.put("health", player.getHealth());
            info.put("foodLevel", player.getFoodLevel());
            info.put("level", player.getLevel());
            info.put("gameMode", player.getGameMode().name());
            info.put("address", player.getAddress() != null ? player.getAddress().toString() : "unknown");
            info.put("ping", player.getPing());
            players.add(info);
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("players", players);
        data.put("count", players.size());
        data.put("max", Bukkit.getMaxPlayers());

        sendControlResponse(commandId, true, data, null);
    }

    /**
     * 处理获取世界信息
     */
    private void handleGetWorldInfo(String commandId) {
        java.util.List<java.util.Map<String, Object>> worlds = new java.util.ArrayList<>();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("name", world.getName());
            info.put("type", world.getEnvironment().name());
            info.put("difficulty", world.getDifficulty().name());
            info.put("loadedChunks", world.getLoadedChunks().length);
            info.put("entities", world.getEntities().size());
            info.put("players", world.getPlayers().size());
            info.put("time", world.getTime());
            info.put("fullTime", world.getFullTime());
            info.put("weather", world.hasStorm() ? (world.isThundering() ? "thunder" : "rain") : "clear");
            worlds.add(info);
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("worlds", worlds);

        sendControlResponse(commandId, true, data, null);
    }

    /**
     * 发送控制命令响应
     */
    private void sendControlResponse(String commandId, boolean success, Object data, String error) {
        if (!isOpen() || !authenticated) {
            plugin.getLogger().warning("无法发送控制响应: WebSocket未连接或未认证");
            return;
        }

        ServerControlPacket.ControlResponse response = new ServerControlPacket.ControlResponse();
        response.setCommandId(commandId);
        response.setSuccess(success);
        response.setMessage(success ? (data instanceof String ? (String) data : "操作成功") : error);
        response.setData(data);

        ProtocolPacket packet = ProtocolPacket.request(PacketType.SERVER_CONTROL_RESPONSE, response);
        send(packet.toJson());
    }

    /**
     * 请求配置
     */
    public void requestConfig() {
        if (!isOpen() || !authenticated) return;
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.CONFIG_REQUEST, new JsonObject());
        send(packet.toJson());
    }
    
    /**
     * 请求玩家数据
     */
    public void requestPlayerData(UUID playerUuid) {
        if (!isOpen() || !authenticated) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("playerUuid", playerUuid.toString());
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.PLAYER_DATA_REQUEST, payload);
        send(packet.toJson());
    }
    
    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        int interval = plugin.getConfigManager().getHeartbeatInterval();
        
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (isOpen() && authenticated) {
                JsonObject heartbeat = new JsonObject();
                heartbeat.addProperty("onlinePlayers", Bukkit.getOnlinePlayers().size());
                heartbeat.addProperty("timestamp", System.currentTimeMillis());
                
                ProtocolPacket packet = ProtocolPacket.request(PacketType.HEARTBEAT, heartbeat);
                send(packet.toJson());
            }
        }, interval, interval, TimeUnit.SECONDS);
    }
    
    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return isOpen();
    }
    
    /**
     * 广播踢出命令到所有服务器
     * 投票在本服务器进行，但结果应用到全服
     */
    public void broadcastKickToAllServers(UUID targetUuid, String targetName, String reason) {
        if (!isOpen() || !authenticated) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("playerUuid", targetUuid.toString());
        payload.addProperty("playerName", targetName);
        payload.addProperty("reason", reason);
        payload.addProperty("broadcastAll", true); // 标记为广播到所有服务器
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.KICK_PLAYER, payload);
        send(packet.toJson());
    }
    
    /**
     * 广播封禁命令到所有服务器
     * 投票在本服务器进行，但结果应用到全服
     */
    public void broadcastBanToAllServers(UUID targetUuid, String targetName, String reason, int durationDays) {
        if (!isOpen() || !authenticated) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("targetUuid", targetUuid.toString());
        payload.addProperty("targetName", targetName);
        payload.addProperty("reason", reason);
        payload.addProperty("durationDays", durationDays);
        payload.addProperty("action", "BAN");
        payload.addProperty("broadcastAll", true); // 标记为广播到所有服务器
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.BAN_SYNC, payload);
        send(packet.toJson());
    }
    
    /**
     * 广播禁言命令到所有服务器
     * 投票在本服务器进行，但结果应用到全服
     */
    public void broadcastMuteToAllServers(UUID targetUuid, String targetName, String reason, int durationDays) {
        if (!isOpen() || !authenticated) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("targetUuid", targetUuid.toString());
        payload.addProperty("targetName", targetName);
        payload.addProperty("reason", reason);
        payload.addProperty("durationDays", durationDays);
        payload.addProperty("broadcastAll", true); // 标记为广播到所有服务器
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.MUTE_PLAYER, payload);
        send(packet.toJson());
    }
    
    /**
     * 发送评分到中控服务器
     */
    public void sendRating(UUID raterUuid, String raterName, UUID targetUuid, String targetName, int score, String comment) {
        if (!isOpen() || !authenticated) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("raterUuid", raterUuid.toString());
        payload.addProperty("raterName", raterName);
        payload.addProperty("targetUuid", targetUuid.toString());
        payload.addProperty("targetName", targetName);
        payload.addProperty("score", score);
        payload.addProperty("comment", comment);
        payload.addProperty("timestamp", System.currentTimeMillis());
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.RATING_SUBMIT, payload);
        send(packet.toJson());
        
        plugin.getLogger().info("评分已提交: " + raterName + " 给 " + targetName + " 打了 " + score + " 分");
    }
    
    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        reconnectManager.scheduleReconnect(() -> {
            reconnect();
        });
    }
    
    /**
     * 重新连接
     */
    public void reconnect() {
        try {
            // 检查当前实例是否是插件中的活跃实例，如果不是则跳过
            if (plugin.getWebSocketClient() != this) {
                plugin.getLogger().fine("当前WebSocket实例已不是活跃实例，跳过重连");
                return;
            }
            
            // 如果当前实例已关闭或正在关闭，创建新实例
            if (isClosed() || isClosing()) {
                plugin.getLogger().info("当前WebSocket已关闭，创建新实例进行重连...");
                PCSWebSocketClient newClient = new PCSWebSocketClient(plugin);
                plugin.setWebSocketClient(newClient);
                newClient.connect();
                return;
            }
            
            // 先尝试优雅关闭当前实例
            try {
                close();
            } catch (Exception ignored) {}

            // 创建新的客户端实例并替换到插件中（旧的 WebSocketClient 不可重用）
            PCSWebSocketClient newClient = new PCSWebSocketClient(plugin);
            plugin.setWebSocketClient(newClient);

            // 启动新连接
            newClient.connect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "重连失败", e);
            // 只有在当前实例仍是活跃实例时才继续重连
            if (plugin.getWebSocketClient() == this) {
                scheduleReconnect();
            }
        }
    }
    
    /**
     * 检查是否已认证
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

}
