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

package com.pcs.central.controller;

import com.pcs.api.model.PCSConfig;
import com.pcs.central.database.RatingRepository;
import com.pcs.central.model.entity.VoteHistoryEntity;
import com.pcs.central.service.*;
import com.pcs.central.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final ConfigService configService;
    private final PlayerService playerService;
    private final VoteService voteService;
    private final WebSocketSessionManager sessionManager;
    private final KickBanService kickBanService;
    private final RatingRepository ratingRepository;

    public AdminController(ConfigService configService,
                          PlayerService playerService,
                          VoteService voteService,
                          WebSocketSessionManager sessionManager,
                          KickBanService kickBanService,
                          RatingRepository ratingRepository) {
        this.configService = configService;
        this.playerService = playerService;
        this.voteService = voteService;
        this.sessionManager = sessionManager;
        this.kickBanService = kickBanService;
        this.ratingRepository = ratingRepository;
    }
    
    @GetMapping("/config")
    public ResponseEntity<PCSConfig> getConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }
    
    @PostMapping("/config")
    public ResponseEntity<Void> updateConfig(@RequestBody PCSConfig config) {
        configService.saveConfig(config);
        // 广播配置更新到所有服务器
        sessionManager.broadcastConfigUpdate(config);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/config/action")
    public ResponseEntity<Void> addAction(@RequestBody PCSConfig.VoteAction action) {
        configService.updateConfig(config -> config.getAvailableActions().add(action));
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/config/action/{actionId}")
    public ResponseEntity<Void> removeAction(@PathVariable String actionId) {
        configService.updateConfig(config -> 
            config.getAvailableActions().removeIf(a -> a.getId().equals(actionId)));
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/config/reason")
    public ResponseEntity<Void> addReason(@RequestBody String reason) {
        configService.updateConfig(config -> config.getAvailableReasons().add(reason));
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/config/reason")
    public ResponseEntity<Void> removeReason(@RequestBody String reason) {
        configService.updateConfig(config -> config.getAvailableReasons().remove(reason));
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/player/{uuid}/ban")
    public ResponseEntity<Void> banPlayer(@PathVariable UUID uuid, @RequestParam String reason) {
        playerService.banPlayer(uuid, reason);
        // 广播封禁同步到所有服务器，踢出在线玩家
        sessionManager.broadcastBanSync(uuid, "BAN", reason, 30);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/player/{uuid}/unban")
    public ResponseEntity<Void> unbanPlayer(@PathVariable UUID uuid) {
        playerService.unbanPlayer(uuid);
        // 广播解封通知到所有服务器
        sessionManager.broadcastUnban(uuid);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 禁言玩家
     */
    @PostMapping("/player/{uuid}/mute")
    public ResponseEntity<Map<String, Object>> mutePlayer(
            @PathVariable UUID uuid,
            @RequestParam String reason,
            @RequestParam(defaultValue = "7") int days) {
        // 广播禁言命令到所有服务器
        sessionManager.broadcastMute(uuid, reason, days);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Player muted for " + days + " days",
            "uuid", uuid,
            "days", days,
            "reason", reason
        ));
    }
    
    /**
     * 解除禁言
     */
    @PostMapping("/player/{uuid}/unmute")
    public ResponseEntity<Map<String, Object>> unmutePlayer(@PathVariable UUID uuid) {
        // 广播解除禁言命令到所有服务器
        sessionManager.broadcastUnmute(uuid);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Player unmuted",
            "uuid", uuid
        ));
    }
    
    @PostMapping("/player/{uuid}/cheater-tag")
    public ResponseEntity<Void> setCheaterTag(@PathVariable UUID uuid, 
                                               @RequestParam boolean tag,
                                               @RequestParam String reason) {
        playerService.setCheaterTag(uuid, tag, reason);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 在服务器执行命令
     */
    @PostMapping("/server/exec")
    public ResponseEntity<Map<String, Object>> executeServerCommand(
            @RequestBody Map<String, String> request,
            @RequestAttribute("username") String executedBy) {

        String serverId = request.get("serverId");
        String command = request.get("command");

        logger.info("收到远程命令请求 - serverId: {}, command: {}, executedBy: {}", serverId, command, executedBy);

        if (serverId == null || command == null) {
            logger.error("远程命令请求缺少必要参数 - serverId: {}, command: {}", serverId, command);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "缺少必要参数: serverId=" + serverId + ", command=" + command
            ));
        }

        // 生成命令ID
        String commandId = UUID.randomUUID().toString();
        logger.info("生成命令ID: {}，准备发送远程命令", commandId);

        // 发送远程命令
        boolean sent = sessionManager.sendRemoteCommand(serverId, command, commandId, executedBy);
        
        if (!sent) {
            logger.error("发送远程命令失败 - serverId: {}, command: {}", serverId, command);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "commandId", commandId,
                "message", "无法发送命令到服务器，请检查服务器是否在线",
                "error", "Server not reachable"
            ));
        }

        logger.info("远程命令已发送，等待响应 - commandId: {}", commandId);

        // 等待响应（带超时）
        try {
            CompletableFuture<String> future = sessionManager.waitForCommandResponse(commandId, 15000); // 增加到15秒
            String output = future.get(15, TimeUnit.SECONDS);

            logger.info("收到命令响应 - commandId: {}, output: {}", commandId, output);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "commandId", commandId,
                "output", output
            ));
        } catch (Exception e) {
            // 超时或错误
            logger.warn("命令执行超时或错误 - commandId: {}, error: {}", commandId, e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "commandId", commandId,
                "message", "命令已发送，但未在15秒内收到响应。命令可能仍在执行或已超时。",
                "warning", "服务器可能正在处理命令，请检查服务器日志确认执行结果。",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取服务器列表
     */
    @GetMapping("/servers")
    public ResponseEntity<Map<String, Object>> getServerList() {
        int onlineCount = sessionManager.getOnlineServerCount();
        Map<String, Map<String, Object>> servers = sessionManager.getOnlineServers();

        // 转换服务器信息为更友好的格式（与前端字段名匹配）
        List<Map<String, Object>> serverList = servers.values().stream().map(info -> {
            Map<String, Object> server = new HashMap<>();
            server.put("id", info.get("id"));
            server.put("name", info.get("name"));
            server.put("type", info.get("type"));
            server.put("online", info.get("online"));
            server.put("version", info.get("version"));
            server.put("playerCount", info.get("playerCount"));
            server.put("maxPlayers", info.get("maxPlayers"));
            server.put("tps", info.get("tps"));
            server.put("memoryUsed", info.get("memoryUsed"));
            server.put("memoryMax", info.get("memoryMax"));
            server.put("lastHeartbeat", info.get("lastHeartbeat"));
            return server;
        }).toList();

        return ResponseEntity.ok(Map.of(
            "servers", serverList,
            "onlineCount", onlineCount
        ));
    }

    /**
     * 踢出玩家（带12小时临时封禁）
     */
    @PostMapping("/player/kick")
    public ResponseEntity<Map<String, Object>> kickPlayer(
            @RequestBody Map<String, String> request,
            @RequestAttribute("username") String executedBy) {
        String uuid = request.get("uuid");
        String playerName = request.get("playerName");
        String reason = request.get("reason");
        String serverId = request.getOrDefault("serverId", "ALL");

        if (uuid == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing player UUID"
            ));
        }

        // 如果是踢出特定服务器，添加12小时临时封禁
        if (!"ALL".equals(serverId)) {
            kickBanService.kickAndBanPlayer(
                UUID.fromString(uuid),
                playerName != null ? playerName : "Unknown",
                serverId,
                reason != null ? reason : "管理员踢出",
                executedBy
            );
        }

        // 执行踢出操作
        sessionManager.kickPlayer(UUID.fromString(uuid), reason != null ? reason : "管理员踢出", serverId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Player kicked and banned for 12 hours on server " + serverId,
            "serverId", serverId,
            "banDuration", "12 hours"
        ));
    }

    /**
     * 解除玩家在特定服务器的临时封禁
     */
    @PostMapping("/player/unban-server")
    public ResponseEntity<Map<String, Object>> unbanPlayerFromServer(@RequestBody Map<String, String> request) {
        String uuid = request.get("uuid");
        String serverId = request.get("serverId");

        if (uuid == null || serverId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing uuid or serverId"
            ));
        }

        boolean success = kickBanService.unbanPlayerFromServer(UUID.fromString(uuid), serverId);

        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Player unbanned from server " + serverId : "No active ban found"
        ));
    }

    /**
     * 检查玩家是否在特定服务器被封禁
     */
    @GetMapping("/player/{uuid}/ban-status/{serverId}")
    public ResponseEntity<Map<String, Object>> checkPlayerBanStatus(
            @PathVariable UUID uuid,
            @PathVariable String serverId) {
        boolean isBanned = kickBanService.isPlayerBannedFromServer(uuid, serverId);
        var banOpt = kickBanService.getActiveBan(uuid, serverId);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("playerUuid", uuid);
        response.put("serverId", serverId);
        response.put("banned", isBanned);

        banOpt.ifPresent(ban -> {
            response.put("reason", ban.getReason());
            response.put("kickedAt", ban.getKickedAt());
            response.put("expireAt", ban.getExpireAt());
            response.put("kickedBy", ban.getKickedBy());
        });

        return ResponseEntity.ok(response);
    }

    /**
     * 获取服务器的临时封禁列表
     */
    @GetMapping("/server/{serverId}/kick-bans")
    public ResponseEntity<Map<String, Object>> getServerKickBans(@PathVariable String serverId) {
        var bans = kickBanService.getActiveBansForServer(serverId);
        return ResponseEntity.ok(Map.of(
            "serverId", serverId,
            "bans", bans,
            "count", bans.size()
        ));
    }

    /**
     * 发送私信给玩家
     */
    @PostMapping("/player/message")
    public ResponseEntity<Map<String, Object>> sendPrivateMessage(@RequestBody Map<String, String> request) {
        String uuid = request.get("uuid");
        String message = request.get("message");
        String serverId = request.getOrDefault("serverId", "ALL");

        if (uuid == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing uuid or message"
            ));
        }

        sessionManager.sendPrivateMessage(UUID.fromString(uuid), message, serverId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Private message sent"
        ));
    }

    // ==================== 离线账号支持（通过玩家名操作） ====================

    /**
     * 通过玩家名封禁玩家（支持离线账号）
     */
    @PostMapping("/player/name/{playerName}/ban")
    public ResponseEntity<Map<String, Object>> banPlayerByName(
            @PathVariable String playerName,
            @RequestParam String reason) {
        
        boolean success = playerService.banPlayerByName(playerName, reason);
        
        // 广播封禁同步到所有服务器
        sessionManager.broadcastBanSyncByName(playerName, "BAN", reason, 30);
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", "Player " + playerName + " has been banned",
            "playerName", playerName,
            "reason", reason
        ));
    }

    /**
     * 通过玩家名解封玩家（支持离线账号）
     */
    @PostMapping("/player/name/{playerName}/unban")
    public ResponseEntity<Map<String, Object>> unbanPlayerByName(@PathVariable String playerName) {
        boolean success = playerService.unbanPlayerByName(playerName);
        
        if (success) {
            // 广播解封通知到所有服务器
            sessionManager.broadcastUnbanByName(playerName);
        }
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Player " + playerName + " has been unbanned" : "Player not found",
            "playerName", playerName
        ));
    }

    /**
     * 通过玩家名禁言玩家（支持离线账号）
     */
    @PostMapping("/player/name/{playerName}/mute")
    public ResponseEntity<Map<String, Object>> mutePlayerByName(
            @PathVariable String playerName,
            @RequestParam String reason,
            @RequestParam(defaultValue = "7") int days) {
        
        // 广播禁言命令到所有服务器（通过名称）
        sessionManager.broadcastMuteByName(playerName, reason, days);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Player " + playerName + " has been muted for " + days + " days",
            "playerName", playerName,
            "days", days,
            "reason", reason
        ));
    }

    /**
     * 通过玩家名解除禁言（支持离线账号）
     */
    @PostMapping("/player/name/{playerName}/unmute")
    public ResponseEntity<Map<String, Object>> unmutePlayerByName(@PathVariable String playerName) {
        // 广播解除禁言命令到所有服务器（通过名称）
        sessionManager.broadcastUnmuteByName(playerName);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Player " + playerName + " has been unmuted",
            "playerName", playerName
        ));
    }

    /**
     * 通过玩家名踢出玩家（支持离线账号）
     */
    @PostMapping("/player/name/kick")
    public ResponseEntity<Map<String, Object>> kickPlayerByName(
            @RequestBody Map<String, String> request,
            @RequestAttribute("username") String executedBy) {
        
        String playerName = request.get("playerName");
        String reason = request.get("reason");
        String serverId = request.getOrDefault("serverId", "ALL");

        if (playerName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing player name"
            ));
        }

        // 通过名称踢出玩家
        sessionManager.kickPlayerByName(playerName, reason != null ? reason : "管理员踢出", serverId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Player " + playerName + " has been kicked",
            "playerName", playerName,
            "serverId", serverId
        ));
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        // 查询今日评分数量
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int todayRatings = ratingRepository.countTodayRatings(startOfToday);
        
        return ResponseEntity.ok(Map.of(
            "onlineCount", sessionManager.getOnlineServerCount(),
            "activeVotes", voteService.getAllActiveVotes().size(),
            "todayVotes", todayRatings, // 从数据库查询今日评分数量
            "bannedCount", kickBanService.getAllActiveBans().size()
        ));
    }

    /**
     * 获取所有玩家（从数据库查询，包括离线和在线玩家）
     */
    @GetMapping("/players")
    public ResponseEntity<Map<String, Object>> getPlayers() {
        // 从数据库查询所有玩家
        var allPlayers = playerService.getAllPlayers();
        
        // 获取在线玩家信息（用于补充实时数据）
        var onlinePlayers = sessionManager.getAllOnlinePlayers();
        Map<String, Map<String, Object>> onlinePlayerMap = onlinePlayers.stream()
            .filter(p -> p.get("uuid") != null)
            .collect(Collectors.toMap(
                p -> p.get("uuid").toString(),
                p -> p,
                (a, b) -> a
            ));
        
        // 合并数据：数据库信息 + 在线状态
        var playersWithStatus = allPlayers.stream()
            .map(p -> {
                Map<String, Object> playerMap = new HashMap<>();
                String uuid = p.getPlayerUuid() != null ? p.getPlayerUuid().toString() : null;
                String name = p.getPlayerName();
                
                playerMap.put("uuid", uuid);
                playerMap.put("name", name);
                playerMap.put("creditScore", p.getCreditScore());
                playerMap.put("currentlyBanned", p.isCurrentlyBanned());
                playerMap.put("cheaterTag", p.isCheaterTag());
                playerMap.put("totalBans", p.getTotalBans());
                playerMap.put("totalKicks", p.getTotalKicks());
                
                // 如果在线，添加在线信息和实时数据
                boolean isOnline = onlinePlayerMap.containsKey(uuid);
                playerMap.put("online", isOnline);
                
                if (isOnline) {
                    Map<String, Object> onlineInfo = onlinePlayerMap.get(uuid);
                    playerMap.put("server", onlineInfo.get("server"));
                    playerMap.put("world", onlineInfo.get("world"));
                    playerMap.put("ping", onlineInfo.get("ping"));
                    playerMap.put("status", "online");
                } else {
                    playerMap.put("status", "offline");
                }
                
                return playerMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "players", playersWithStatus,
            "playerCount", playersWithStatus.size(),
            "onlineCount", onlinePlayers.size()
        ));
    }

    /**
     * 获取服务器详情（包括玩家列表）
     */
    @GetMapping("/server/{serverId}/detail")
    public ResponseEntity<Map<String, Object>> getServerDetail(@PathVariable String serverId) {
        var serverInfo = sessionManager.getOnlineServers().get(serverId);
        if (serverInfo == null) {
            return ResponseEntity.notFound().build();
        }

        var players = sessionManager.getServerPlayers(serverId);

        return ResponseEntity.ok(Map.of(
            "server", serverInfo,
            "players", players,
            "playerCount", players.size()
        ));
    }

    /**
     * 获取投票历史记录
     */
    @GetMapping("/votes/history")
    public ResponseEntity<Map<String, Object>> getVoteHistory() {
        var historyEntities = voteService.getRecentVoteHistory(50);

        List<Map<String, Object>> historyList = historyEntities.stream()
            .map(entity -> {
                Map<String, Object> vote = new HashMap<>();
                vote.put("voteId", entity.getVoteId());
                vote.put("targetPlayerUuid", entity.getTargetPlayerUuid());
                vote.put("targetPlayerName", entity.getTargetPlayerName());
                vote.put("initiatorUuid", entity.getInitiatorUuid());
                vote.put("initiatorName", entity.getInitiatorName());
                vote.put("action", entity.getAction());
                vote.put("reason", entity.getReason());
                vote.put("serverId", entity.getServerId());
                vote.put("serverName", entity.getServerName());
                vote.put("voteTime", entity.getVoteTime());
                vote.put("passed", entity.isPassed());
                vote.put("yesVotes", entity.getYesVotes());
                vote.put("noVotes", entity.getNoVotes());
                vote.put("status", entity.isPassed() ? "PASSED" : "REJECTED");
                return vote;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "votes", historyList,
            "count", historyList.size()
        ));
    }
}
