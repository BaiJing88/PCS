package com.pcs.fabric.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pcs.api.model.PCSConfig;
import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.VoteSession;
import com.pcs.api.protocol.*;
import com.pcs.fabric.PCSFabricMod;
import net.minecraft.server.network.ServerPlayerEntity;
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
import java.util.logging.Logger;

/**
 * Fabric 版 WebSocket 客户端
 * 与 Spigot 插件体验一致
 */
public class FabricWebSocketClient extends WebSocketClient {
    
    private final PCSFabricMod mod;
    private final Gson gson;
    private final ScheduledExecutorService heartbeatExecutor;
    private final FabricReconnectManager reconnectManager;

    private boolean authenticated = false;
    private String authToken;
    private String serverId;
    private String serverName;
    private String apiKey;
    private String wsUrl;
    
    public FabricWebSocketClient(PCSFabricMod mod) {
        super(createURI(mod));
        this.mod = mod;
        this.gson = new Gson();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        this.reconnectManager = new FabricReconnectManager(mod);
        
        this.serverId = mod.getConfig().get("serverId", "fabric-server-1");
        this.serverName = mod.getConfig().get("serverName", "Fabric Server");
        this.apiKey = mod.getConfig().get("apiKey", "");
        this.wsUrl = mod.getConfig().get("wsUrl", "ws://localhost:8080/ws/pcs");
        
        setConnectionLostTimeout(60);
    }
    
    private static URI createURI(PCSFabricMod mod) {
        try {
            String wsUrl = mod.getConfig().get("wsUrl", "ws://localhost:8080/ws/pcs");
            return new URI(wsUrl);
        } catch (URISyntaxException e) {
            mod.getLogger().severe("WebSocket URL 格式错误: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        mod.getLogger().info("WebSocket 连接已建立，正在认证...");
        authenticated = false;
        reconnectManager.resetAttempts();
        sendAuthentication();
        startHeartbeat();
    }
    
    @Override
    public void onMessage(String message) {
        try {
            ProtocolPacket packet = ProtocolPacket.fromJson(message);
            PacketType type = PacketType.fromString(packet.getType());
            
            if (type == null) {
                mod.getLogger().warn("收到未知类型的数据包: " + packet.getType());
                return;
            }
            
            switch (type) {
                case AUTH_RESPONSE -> handleAuthResponse(packet);
                case CONFIG_UPDATE -> handleConfigUpdate(packet);
                case BAN_NOTIFY -> handleBanNotify(packet);
                case UNBAN_NOTIFY -> handleUnbanNotify(packet);
                case BAN_SYNC -> handleBanSync(packet);
                case BROADCAST -> handleBroadcast(packet);
                case REMOTE_COMMAND -> handleRemoteCommand(packet);
                case SERVER_CONTROL -> handleServerControl(packet);
                case KICK_PLAYER -> handleKickPlayer(packet);
                case PRIVATE_MESSAGE -> handlePrivateMessage(packet);
                case VOTE_START -> handleVoteStart(packet);
                case VOTE_END -> handleVoteEnd(packet);
                case ERROR -> handleError(packet);
                default -> mod.getLogger().debug("收到未处理的数据包: " + type);
            }
        } catch (Exception e) {
            mod.getLogger().warning("处理 WebSocket 消息时出错: " + e.getMessage());
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        mod.getLogger().info("WebSocket 连接已关闭: " + reason + " (code: " + code + ")");
        authenticated = false;
        stopHeartbeat();
        
        if (remote) {
            reconnectManager.recordFailedReconnect();
            scheduleReconnect();
        }
    }
    
    @Override
    public void onError(Exception ex) {
        mod.getLogger().warning("WebSocket 错误: " + ex.getMessage());
    }
    
    // ==================== 认证 ====================
    
    private void sendAuthentication() {
        AuthPacket auth = new AuthPacket();
        auth.setServerId(serverId);
        auth.setServerName(serverName);
        auth.setServerType("FABRIC");
        auth.setServerVersion(net.minecraft.SharedConstants.getGameVersion().getName());
        auth.setApiKey(apiKey);
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.AUTH_REQUEST, auth);
        send(packet.toJson());
    }
    
    private void handleAuthResponse(ProtocolPacket packet) {
        AuthPacket response = packet.getPayload(AuthPacket.class);
        
        if (response.isSuccess()) {
            authenticated = true;
            authToken = response.getToken();
            mod.getLogger().info("认证成功！服务器ID: " + serverId);
            requestConfig();
        } else {
            mod.getLogger().warning("认证失败: " + response.getMessage());
        }
    }
    
    // ==================== 配置 ====================
    
    private void requestConfig() {
        ProtocolPacket packet = ProtocolPacket.request(PacketType.CONFIG_REQUEST, null);
        send(packet.toJson());
    }
    
    private void handleConfigUpdate(ProtocolPacket packet) {
        PCSConfig config = packet.getPayload(PCSConfig.class);
        mod.setConfig(config);
        mod.getLogger().info("配置已更新");
    }
    
    // ==================== 封禁 ====================
    
    private void handleBanNotify(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        String targetName = ctrl.getTargetName();
        
        // 踢出玩家
        mod.getServer().getPlayerManager().getPlayerByUsername(targetName).ifPresent(player -> {
            player.networkHandler.disconnect(net.minecraft.text.Text.literal("你已被封禁: " + ctrl.getReason()));
        });
        
        mod.getLogger().info("玩家 " + targetName + " 已被封禁");
    }
    
    private void handleUnbanNotify(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        mod.getLogger().info("玩家 " + ctrl.getTargetName() + " 已解封");
    }
    
    private void handleBanSync(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        mod.getLogger().info("收到封禁同步: " + ctrl.getTargetName());
    }
    
    // ==================== 广播 ====================
    
    private void handleBroadcast(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        String message = ctrl.getMessage();
        
        mod.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(net.minecraft.text.Text.literal(message));
        });
    }
    
    // ==================== 远程命令 ====================
    
    private void handleRemoteCommand(ProtocolPacket packet) {
        CommandPacket cmd = packet.getPayload(CommandPacket.class);
        String command = cmd.getCommand();
        
        mod.getServer().getCommandManager().execute(
            mod.getServer().getCommandSource(),
            command
        );
    }
    
    // ==================== 服务器控制 ====================
    
    private void handleServerControl(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        mod.getLogger().info("收到服务器控制命令: " + ctrl.getAction());
    }
    
    // ==================== 踢出玩家 ====================
    
    private void handleKickPlayer(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        String targetName = ctrl.getTargetName();
        
        mod.getServer().getPlayerManager().getPlayerByUsername(targetName).ifPresent(player -> {
            player.networkHandler.disconnect(net.minecraft.text.Text.literal(ctrl.getReason()));
        });
    }
    
    // ==================== 私信 ====================
    
    private void handlePrivateMessage(ProtocolPacket packet) {
        ServerControlPacket ctrl = packet.getPayload(ServerControlPacket.class);
        String targetName = ctrl.getTargetName();
        
        mod.getServer().getPlayerManager().getPlayerByUsername(targetName).ifPresent(player -> {
            player.sendMessage(net.minecraft.text.Text.literal("[PCS] " + ctrl.getMessage()));
        });
    }
    
    // ==================== 投票 ====================
    
    private void handleVoteStart(ProtocolPacket packet) {
        VoteSession vote = packet.getPayload(VoteSession.class);
        mod.getLogger().info("投票开始: " + vote.getAction() + " " + vote.getTargetName());
        
        // 广播给玩家
        String msg = String.format("§e[PCS] 投票发起: %s %s - 原因: %s", 
            vote.getAction(), vote.getTargetName(), vote.getReason());
        mod.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(net.minecraft.text.Text.literal(msg));
        });
    }
    
    private void handleVoteEnd(ProtocolPacket packet) {
        VoteSession vote = packet.getPayload(VoteSession.class);
        String result = vote.isPassed() ? "§a通过" : "§c否决";
        mod.getLogger().info("投票结束: " + vote.getAction() + " " + vote.getTargetName() + " - " + result);
    }
    
    // ==================== 错误 ====================
    
    private void handleError(ProtocolPacket packet) {
        ProtocolPacket.Error err = packet.getPayload(ProtocolPacket.Error.class);
        mod.getLogger().warning("收到错误: " + err.getMessage());
    }
    
    // ==================== 心跳 ====================
    
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }
    
    private void stopHeartbeat() {
        heartbeatExecutor.shutdown();
    }
    
    private void sendHeartbeat() {
        if (!isOpen()) return;
        
        try {
            JsonObject heartbeat = new JsonObject();
            heartbeat.addProperty("type", "HEARTBEAT");
            heartbeat.addProperty("serverId", serverId);
            heartbeat.addProperty("timestamp", System.currentTimeMillis());
            
            // 添加服务器状态
            var server = mod.getServer();
            heartbeat.addProperty("onlinePlayers", server.getPlayerManager().getPlayerList().size());
            heartbeat.addProperty("maxPlayers", server.getMaxPlayerCount());
            heartbeat.addProperty("tps", 20.0); // Fabric 需要额外获取
            
            send(heartbeat.toString());
        } catch (Exception e) {
            mod.getLogger().warning("发送心跳失败: " + e.getMessage());
        }
    }
    
    // ==================== 重连 ====================
    
    private void scheduleReconnect() {
        long delay = reconnectManager.getNextReconnectDelay();
        mod.getLogger().info("将在 " + delay + " 秒后尝试重连...");
        
        heartbeatExecutor.schedule(() -> {
            try {
                reconnect();
            } catch (Exception e) {
                mod.getLogger().warning("重连失败: " + e.getMessage());
            }
        }, delay, TimeUnit.SECONDS);
    }
    
    // ==================== 公开方法 ====================
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void sendRating(UUID raterUuid, String raterName, UUID targetUuid, String targetName, int score, String comment) {
        RatingInfo rating = new RatingInfo();
        rating.setRaterUuid(raterUuid);
        rating.setRaterName(raterName);
        rating.setTargetUuid(targetUuid);
        rating.setTargetName(targetName);
        rating.setScore(score);
        rating.setComment(comment);
        rating.setServerId(serverId);
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.RATING_SUBMIT, rating);
        send(packet.toJson());
    }
    
    public void sendPlayerJoin(ServerPlayerEntity player) {
        JsonObject data = new JsonObject();
        data.addProperty("playerUuid", player.getUuid().toString());
        data.addProperty("playerName", player.getName().getString());
        data.addProperty("serverId", serverId);
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.PLAYER_JOIN, data);
        send(packet.toJson());
    }
    
    public void sendPlayerQuit(ServerPlayerEntity player) {
        JsonObject data = new JsonObject();
        data.addProperty("playerUuid", player.getUuid().toString());
        data.addProperty("playerName", player.getName().getString());
        data.addProperty("serverId", serverId);
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.PLAYER_QUIT, data);
        send(packet.toJson());
    }
    
    public void disconnect() {
        close();
        stopHeartbeat();
    }
}
