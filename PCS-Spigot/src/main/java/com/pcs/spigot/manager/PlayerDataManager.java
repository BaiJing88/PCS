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

package com.pcs.spigot.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.VoteHistory;
import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据管理器
 * 管理玩家信用数据、评分记录等
 */
public class PlayerDataManager {
    
    private final PCSSpigotPlugin plugin;
    private final Gson gson;
    private final File dataFile;
    
    // 玩家信用数据缓存
    private final Map<UUID, PlayerCredit> playerCredits;
    
    // 今日投票计数
    private final Map<UUID, Integer> dailyVoteCount;
    
    // 今日评分计数
    private final Map<UUID, Integer> dailyRatingCount;
    
    // 最后一次投票时间
    private final Map<UUID, Long> lastVoteTime;
    
    // 最后一次评分时间
    private final Map<UUID, Long> lastRatingTime;
    
    // 被禁言的玩家
    private final Set<UUID> mutedPlayers;
    private final Map<UUID, Long> muteExpiry;
    
    // 数据保存日期（用于跨天重置每日计数）
    private String lastSaveDate;
    
    public PlayerDataManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.json");
        
        this.playerCredits = new ConcurrentHashMap<>();
        this.dailyVoteCount = new ConcurrentHashMap<>();
        this.dailyRatingCount = new ConcurrentHashMap<>();
        this.lastVoteTime = new ConcurrentHashMap<>();
        this.lastRatingTime = new ConcurrentHashMap<>();
        this.mutedPlayers = ConcurrentHashMap.newKeySet();
        this.muteExpiry = new ConcurrentHashMap<>();
        
        // 加载已有数据
        loadData();
        
        // 启动定时任务清理过期数据
        startCleanupTask();
        
        // 启动自动保存任务（每5分钟保存一次）
        startAutoSaveTask();
    }
    
    /**
     * 获取或创建玩家信用数据
     */
    public PlayerCredit getPlayerCredit(UUID uuid) {
        return playerCredits.computeIfAbsent(uuid, id -> {
            PlayerCredit credit = new PlayerCredit();
            credit.setPlayerUuid(id);
            return credit;
        });
    }
    
    /**
     * 获取或创建玩家信用数据（带名称）
     */
    public PlayerCredit getPlayerCredit(UUID uuid, String playerName) {
        PlayerCredit credit = getPlayerCredit(uuid);
        if (playerName != null) {
            credit.setPlayerName(playerName);
        }
        return credit;
    }
    
    /**
     * 更新玩家信用数据
     */
    public void updatePlayerCredit(PlayerCredit credit) {
        playerCredits.put(credit.getPlayerUuid(), credit);
    }
    
    /**
     * 获取玩家信用数据缓存映射
     */
    public Map<UUID, PlayerCredit> getPlayerCredits() {
        return playerCredits;
    }
    
    /**
     * 从服务器获取玩家信用数据
     */
    public void fetchPlayerCreditFromServer(UUID uuid, String playerName) {
        // 通过WebSocket请求中控服务器获取数据
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
            plugin.getWebSocketClient().requestPlayerData(uuid);
        }
    }
    
    /**
     * 检查玩家是否可以发起投票
     */
    public boolean canStartVote(Player player) {
        // 检查冷却时间
        Long lastVote = lastVoteTime.get(player.getUniqueId());
        if (lastVote != null) {
            long cooldownMillis = plugin.getConfigManager().getVoteCooldownMinutes() * 60 * 1000;
            if (System.currentTimeMillis() - lastVote < cooldownMillis) {
                return false;
            }
        }
        
        // 检查每日限制
        int votesToday = dailyVoteCount.getOrDefault(player.getUniqueId(), 0);
        return votesToday < plugin.getConfigManager().getMaxDailyVotes();
    }
    
    /**
     * 获取投票冷却剩余时间（秒）
     */
    public long getVoteCooldownRemaining(Player player) {
        Long lastVote = lastVoteTime.get(player.getUniqueId());
        if (lastVote == null) return 0;
        
        long cooldownMillis = plugin.getConfigManager().getVoteCooldownMinutes() * 60 * 1000;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastVote);
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * 获取今日剩余投票次数
     */
    public int getRemainingDailyVotes(Player player) {
        int votesToday = dailyVoteCount.getOrDefault(player.getUniqueId(), 0);
        return Math.max(0, plugin.getConfigManager().getMaxDailyVotes() - votesToday);
    }
    
    /**
     * 记录投票
     */
    public void recordVote(Player player) {
        lastVoteTime.put(player.getUniqueId(), System.currentTimeMillis());
        dailyVoteCount.merge(player.getUniqueId(), 1, Integer::sum);
    }
    
    /**
     * 检查玩家是否可以评分
     */
    public boolean canRate(Player player) {
        // 检查冷却时间
        Long lastRating = lastRatingTime.get(player.getUniqueId());
        if (lastRating != null) {
            long cooldownMillis = plugin.getConfigManager().getRatingCooldownMinutes() * 60 * 1000;
            if (System.currentTimeMillis() - lastRating < cooldownMillis) {
                return false;
            }
        }
        
        // 检查每日限制
        int ratingsToday = dailyRatingCount.getOrDefault(player.getUniqueId(), 0);
        return ratingsToday < plugin.getConfigManager().getMaxDailyRatings();
    }
    
    /**
     * 检查是否可以对特定玩家评分
     */
    public boolean canRatePlayer(Player rater, UUID targetUuid) {
        PlayerCredit credit = getPlayerCredit(rater.getUniqueId());
        long cooldownMillis = plugin.getConfigManager().getSamePlayerRatingCooldownDays() * 24 * 60 * 60 * 1000L;
        return credit.canRatePlayer(targetUuid, cooldownMillis);
    }
    
    /**
     * 记录评分
     */
    public void recordRating(Player rater, UUID targetUuid) {
        lastRatingTime.put(rater.getUniqueId(), System.currentTimeMillis());
        dailyRatingCount.merge(rater.getUniqueId(), 1, Integer::sum);
        
        PlayerCredit credit = getPlayerCredit(rater.getUniqueId());
        credit.recordRating(targetUuid);
    }
    
    /**
     * 禁言玩家
     */
    public void mutePlayer(UUID uuid, long durationMillis) {
        mutedPlayers.add(uuid);
        muteExpiry.put(uuid, System.currentTimeMillis() + durationMillis);
    }
    
    /**
     * 解禁玩家
     */
    public void unmutePlayer(UUID uuid) {
        mutedPlayers.remove(uuid);
        muteExpiry.remove(uuid);
    }
    
    /**
     * 检查玩家是否被禁言
     */
    public boolean isMuted(UUID uuid) {
        if (!mutedPlayers.contains(uuid)) {
            return false;
        }
        
        Long expiry = muteExpiry.get(uuid);
        if (expiry == null || expiry < System.currentTimeMillis()) {
            // 禁言已过期
            unmutePlayer(uuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取禁言剩余时间（毫秒）
     */
    public long getMuteRemaining(UUID uuid) {
        if (!isMuted(uuid)) return 0;
        
        Long expiry = muteExpiry.get(uuid);
        if (expiry == null) return 0;
        
        return Math.max(0, expiry - System.currentTimeMillis());
    }
    
    /**
     * 封禁玩家
     */
    public void banPlayer(UUID uuid, String reason) {
        PlayerCredit credit = getPlayerCredit(uuid);
        credit.incrementBanCount();
        credit.setCurrentlyBanned(true);
        
        // 执行封禁
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer != null) {
            String banMessage = "§c你已被封禁！\n§7原因: " + reason + "\n§7请申诉请到 Discord";
            plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(
                offlinePlayer.getName(), 
                banMessage, 
                null, 
                "PCS-System"
            );
            
            // 如果在线则踢出
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.kickPlayer(banMessage);
            }
        }
    }
    
    /**
     * 解封玩家
     */
    public void unbanPlayer(UUID uuid) {
        PlayerCredit credit = getPlayerCredit(uuid);
        credit.unban();
        
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer != null && offlinePlayer.getName() != null) {
            plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).pardon(offlinePlayer.getName());
        }
    }
    
    /**
     * 踢出玩家
     */
    public void kickPlayer(UUID uuid, String reason) {
        PlayerCredit credit = getPlayerCredit(uuid);
        credit.incrementKickCount();
        
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.kickPlayer("§c你已被投票踢出！\n§7原因: " + reason);
        }
    }
    
    /**
     * 添加投票历史
     */
    public void addVoteHistory(UUID uuid, VoteHistory history) {
        PlayerCredit credit = getPlayerCredit(uuid);
        credit.addVoteHistory(history);
    }
    
    /**
     * 获取所有在线玩家的UUID列表
     */
    public List<UUID> getOnlinePlayerUuids() {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }
    
    /**
     * 获取所有在线玩家（排除特定玩家）
     */
    public List<Player> getOnlinePlayersExcluding(Player exclude) {
        List<Player> players = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.equals(exclude) && player.hasPermission("pcs.vote.participate")) {
                players.add(player);
            }
        }
        return players;
    }
    
    /**
     * 保存所有数据到文件
     */
    public void saveAll() {
        try {
            // 确保数据文件夹存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // 构建保存的数据结构
            Map<String, Object> saveData = new HashMap<>();
            saveData.put("lastSaveDate", getCurrentDate());
            saveData.put("dailyVoteCount", new HashMap<>(dailyVoteCount));
            saveData.put("dailyRatingCount", new HashMap<>(dailyRatingCount));
            saveData.put("lastVoteTime", new HashMap<>(lastVoteTime));
            saveData.put("lastRatingTime", new HashMap<>(lastRatingTime));
            saveData.put("muteExpiry", new HashMap<>(muteExpiry));
            
            // 保存玩家信用数据（只保存有实际数据的）
            Map<String, PlayerCredit> creditsToSave = new HashMap<>();
            playerCredits.forEach((uuid, credit) -> {
                if (credit.getTotalBans() > 0 || credit.getTotalKicks() > 0 || 
                    credit.isCurrentlyBanned() || credit.isCheaterTag() ||
                    credit.getCreditScore() != 5.0) {
                    creditsToSave.put(uuid.toString(), credit);
                }
            });
            saveData.put("playerCredits", creditsToSave);
            
            // 写入文件
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(saveData, writer);
            }
            
            plugin.getLogger().fine("玩家数据已保存到 " + dataFile.getName() + 
                " (" + creditsToSave.size() + " 个玩家, " + 
                dailyVoteCount.size() + " 个投票记录, " +
                dailyRatingCount.size() + " 个评分记录)");
                
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "保存玩家数据失败", e);
        }
    }
    
    /**
     * 从文件加载数据
     */
    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("玩家数据文件不存在，将创建新文件");
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> saveData = gson.fromJson(reader, type);
            
            if (saveData == null) {
                plugin.getLogger().warning("玩家数据文件为空");
                return;
            }
            
            String savedDate = (String) saveData.get("lastSaveDate");
            String currentDate = getCurrentDate();
            
            // 如果不是同一天，重置每日计数
            if (!currentDate.equals(savedDate)) {
                plugin.getLogger().info("检测到跨天，重置每日投票/评分计数");
                dailyVoteCount.clear();
                dailyRatingCount.clear();
            } else {
                // 加载每日计数
                loadMapFromJson(saveData.get("dailyVoteCount"), dailyVoteCount, Integer.class);
                loadMapFromJson(saveData.get("dailyRatingCount"), dailyRatingCount, Integer.class);
            }
            
            // 加载时间记录
            loadMapFromJson(saveData.get("lastVoteTime"), lastVoteTime, Long.class);
            loadMapFromJson(saveData.get("lastRatingTime"), lastRatingTime, Long.class);
            loadMapFromJson(saveData.get("muteExpiry"), muteExpiry, Long.class);
            
            // 恢复禁言玩家集合
            muteExpiry.keySet().forEach(mutedPlayers::add);
            
            // 加载玩家信用数据
            Object creditsObj = saveData.get("playerCredits");
            if (creditsObj instanceof Map) {
                Map<String, Object> creditsMap = (Map<String, Object>) creditsObj;
                creditsMap.forEach((uuidStr, creditObj) -> {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        String creditJson = gson.toJson(creditObj);
                        PlayerCredit credit = gson.fromJson(creditJson, PlayerCredit.class);
                        if (credit != null) {
                            credit.setPlayerUuid(uuid);
                            playerCredits.put(uuid, credit);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载玩家数据失败: " + uuidStr);
                    }
                });
            }
            
            plugin.getLogger().info("玩家数据已加载: " + playerCredits.size() + " 个玩家记录");
            
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "加载玩家数据失败", e);
        }
    }
    
    /**
     * 辅助方法：从JSON加载Map
     */
    @SuppressWarnings("unchecked")
    private <T> void loadMapFromJson(Object jsonObj, Map<UUID, T> targetMap, Class<T> valueClass) {
        if (jsonObj instanceof Map) {
            Map<String, Object> sourceMap = (Map<String, Object>) jsonObj;
            sourceMap.forEach((key, value) -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (valueClass == Integer.class && value instanceof Number) {
                        targetMap.put(uuid, (T) Integer.valueOf(((Number) value).intValue()));
                    } else if (valueClass == Long.class && value instanceof Number) {
                        targetMap.put(uuid, (T) Long.valueOf(((Number) value).longValue()));
                    }
                } catch (Exception e) {
                    // 忽略无效的UUID
                }
            });
        }
    }
    
    /**
     * 获取当前日期字符串
     */
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }
    
    /**
     * 启动自动保存任务
     */
    private void startAutoSaveTask() {
        // 每5分钟保存一次（静默模式，不输出日志）
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAll();
        }, 20 * 60 * 5, 20 * 60 * 5); // 5分钟
    }
    
    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        // 每小时清理过期数据
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cleanupExpiredData();
        }, 20 * 60 * 60, 20 * 60 * 60);
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupExpiredData() {
        long now = System.currentTimeMillis();
        
        // 清理已过期的禁言
        Iterator<Map.Entry<UUID, Long>> muteIterator = muteExpiry.entrySet().iterator();
        while (muteIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = muteIterator.next();
            if (entry.getValue() < now) {
                mutedPlayers.remove(entry.getKey());
                muteIterator.remove();
            }
        }
    }
}
