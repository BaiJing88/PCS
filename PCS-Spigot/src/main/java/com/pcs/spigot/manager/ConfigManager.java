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

import com.pcs.api.model.PCSConfig;
import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 配置管理器
 * 管理插件配置和从中控服务器获取的配置
 */
public class ConfigManager {
    
    private final PCSSpigotPlugin plugin;
    private PCSConfig remoteConfig;
    
    public ConfigManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.remoteConfig = new PCSConfig(); // 默认配置
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        plugin.reloadConfig();
        plugin.getLogger().info("配置已重新加载");
    }

    /**
     * 从中控服务器更新配置
     */
    public void updateRemoteConfig(PCSConfig config) {
        this.remoteConfig = config;
        plugin.getLogger().info("配置已从中控服务器更新");
    }
    
    /**
     * 获取本地配置
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
    
    /**
     * 获取远程配置
     */
    public PCSConfig getRemoteConfig() {
        return remoteConfig;
    }
    
    // ==================== 服务器配置 ====================
    // 注意：服务器配置（id、name、type）都由服主本地配置，不由中控下发

    /**
     * 获取服务器唯一标识（重要：每个服务器必须不同）
     * 从本地配置文件读取，如果不存在则自动生成并保存
     */
    public String getServerId() {
        String serverId = getConfig().getString("server.id");
        
        // 如果没有配置server.id，自动生成一个唯一的ID
        if (serverId == null || serverId.isEmpty() || "spigot-server-1".equals(serverId)) {
            // 生成唯一ID：类型-随机数-时间戳
            String random = java.util.UUID.randomUUID().toString().substring(0, 8);
            String type = getServerType().toLowerCase();
            serverId = type + "-" + random;
            
            // 保存到配置文件
            getConfig().set("server.id", serverId);
            plugin.saveConfig();
            
            plugin.getLogger().info("==============================================");
            plugin.getLogger().info("首次运行，已自动生成服务器唯一ID: " + serverId);
            plugin.getLogger().info("此ID已保存到 plugins/PCS-Spigot/config.yml");
            plugin.getLogger().info("请确保每个服务器的ID都是唯一的！");
            plugin.getLogger().info("==============================================");
        }
        
        return serverId;
    }

    /**
     * 获取服务器显示名称（从本地配置读取）
     */
    public String getServerName() {
        return getConfig().getString("server.name", "生存服务器");
    }

    /**
     * 获取服务器类型（从本地配置读取）
     */
    public String getServerType() {
        return getConfig().getString("server.type", "SPIGOT");
    }
    
    // ==================== 中控连接配置 ====================
    
    public String getControllerHost() {
        return getConfig().getString("controller.host", "localhost");
    }
    
    public int getControllerPort() {
        return getConfig().getInt("controller.port", 8080);
    }
    
    public boolean isUseSsl() {
        return getConfig().getBoolean("controller.use-ssl", false);
    }
    
    public String getApiKey() {
        return getConfig().getString("controller.api-key", "");
    }
    
    public int getReconnectInterval() {
        return getConfig().getInt("controller.reconnect-interval", 30);
    }
    
    public int getHeartbeatInterval() {
        return getConfig().getInt("controller.heartbeat-interval", 30);
    }
    
    /**
     * 获取WebSocket URL
     */
    public String getWebSocketUrl() {
        String protocol = isUseSsl() ? "wss" : "ws";
        return protocol + "://" + getControllerHost() + ":" + getControllerPort() + "/ws/server";
    }
    
    // ==================== 投票配置 ====================
    // 【重要】投票配置完全从中控服务器获取，本地不配置
    // 中控未连接时使用硬编码默认值

    public int getVoteDuration() {
        int remoteValue = remoteConfig.getVoteDurationSeconds();
        return remoteValue > 0 ? remoteValue : 300; // 默认5分钟
    }

    public int getMinPlayersToStart() {
        int remoteValue = remoteConfig.getMinPlayersToStart();
        return remoteValue > 0 ? remoteValue : 3; // 默认最少3人在线
    }

    public double getPassRate() {
        double remoteValue = remoteConfig.getPassRate();
        return remoteValue > 0 ? remoteValue : 0.6666; // 默认2/3通过率
    }

    public int getMinTotalVotes() {
        int remoteValue = remoteConfig.getMinTotalVotes();
        return remoteValue > 0 ? remoteValue : 3; // 默认最少3票
    }

    public int getVoteCooldownMinutes() {
        int remoteValue = remoteConfig.getVoteCooldownMinutes();
        return remoteValue > 0 ? remoteValue : 10; // 默认10分钟冷却
    }

    public int getMaxDailyVotes() {
        int remoteValue = remoteConfig.getMaxDailyVotes();
        return remoteValue > 0 ? remoteValue : 5; // 默认每日5次
    }

    /**
     * 管理员是否免疫投票（不能被投票）
     */
    public boolean isAdminImmune() {
        return getConfig().getBoolean("vote.admin-immune", true);
    }

    /**
     * 获取管理员权限列表（拥有这些权限的玩家不能被投票）
     */
    public List<String> getAdminPermissions() {
        return getConfig().getStringList("vote.admin-permissions");
    }

    /**
     * 检查玩家是否为管理员（不能被投票）
     */
    public boolean isAdmin(Player player) {
        if (!isAdminImmune()) {
            return false;
        }
        for (String perm : getAdminPermissions()) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== GUI配置 ====================

    public String getGuiTitlePrefix() {
        String remoteValue = remoteConfig.getGuiTitlePrefix();
        return remoteValue != null && !remoteValue.isEmpty() ? remoteValue : getConfig().getString("gui.title-prefix", "§8[§6PCS§8] ");
    }

    public int getPlayersPerPage() {
        int remoteValue = remoteConfig.getPlayersPerPage();
        return remoteValue > 0 ? remoteValue : getConfig().getInt("gui.players-per-page", 36);
    }

    public boolean isShowOfflinePlayers() {
        // 远程配置优先（boolean 类型没有未设置状态，所以直接返回远程值）
        return remoteConfig.isShowOfflinePlayers();
    }
    
    // ==================== 评分配置 ====================

    public int getRatingCooldownMinutes() {
        int remoteValue = remoteConfig.getRatingCooldownMinutes();
        return remoteValue > 0 ? remoteValue : getConfig().getInt("rating.cooldown-minutes", 30);
    }

    public int getSamePlayerRatingCooldownDays() {
        int remoteValue = remoteConfig.getSamePlayerRatingCooldownDays();
        return remoteValue > 0 ? remoteValue : getConfig().getInt("rating.same-player-cooldown-days", 7);
    }

    public int getMaxDailyRatings() {
        int remoteValue = remoteConfig.getMaxDailyRatings();
        return remoteValue > 0 ? remoteValue : getConfig().getInt("rating.max-daily-ratings", 10);
    }
    
    // ==================== 禁言配置 ====================

    public int getMuteDurationDays() {
        int remoteValue = remoteConfig.getMuteDays();
        return remoteValue > 0 ? remoteValue : getConfig().getInt("mute.duration-days", 7);
    }
    
    // ==================== 可用操作和理由 ====================

    public List<PCSConfig.VoteAction> getAvailableActions() {
        List<PCSConfig.VoteAction> actions = remoteConfig.getAvailableActions();
        // 如果远程配置为空，返回本地默认配置
        if (actions == null || actions.isEmpty()) {
            actions = getDefaultConfig().getAvailableActions();
        }
        return actions;
    }

    public List<String> getAvailableReasons() {
        List<String> reasons = remoteConfig.getAvailableReasons();
        // 如果远程配置为空，返回本地默认配置
        if (reasons == null || reasons.isEmpty()) {
            reasons = getDefaultConfig().getAvailableReasons();
        }
        return reasons;
    }

    /**
     * 获取默认配置（当远程配置不可用时使用）
     */
    private PCSConfig getDefaultConfig() {
        return new PCSConfig();
    }
    
    /**
     * 获取操作显示名称
     */
    public String getActionDisplayName(String actionId) {
        for (PCSConfig.VoteAction action : getAvailableActions()) {
            if (action.getId().equalsIgnoreCase(actionId)) {
                return action.getName();
            }
        }
        return actionId;
    }
    
    /**
     * 检查操作是否可用
     */
    public boolean isActionAvailable(String actionId) {
        for (PCSConfig.VoteAction action : getAvailableActions()) {
            if (action.getId().equalsIgnoreCase(actionId)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== 功能开关 ====================
    // 功能开关也优先从远程配置获取

    public boolean isVoteSystemEnabled() {
        // 【重要】投票系统必须开启，此配置仅用于显示状态
        // 实际投票功能始终可用
        return true;
    }

    public boolean isRatingSystemEnabled() {
        return remoteConfig.isRatingSystemEnabled();
    }

    public boolean isBanSyncEnabled() {
        return remoteConfig.isBanSyncEnabled();
    }

    public boolean isCreditQueryEnabled() {
        return remoteConfig.isCreditQueryEnabled();
    }

    public boolean isAutoBanEnabled() {
        return remoteConfig.isAutoBanEnabled();
    }

    // ==================== 消息配置 ====================
    // 消息配置也从中控服务器获取

    public String getMessagePrefix() {
        String remoteValue = remoteConfig.getMessagePrefix();
        return remoteValue != null && !remoteValue.isEmpty() ? remoteValue : "§8[§6PCS§8] §r";
    }

    public String getSuccessColor() {
        String remoteValue = remoteConfig.getSuccessColor();
        return remoteValue != null && !remoteValue.isEmpty() ? remoteValue : "§a";
    }

    public String getErrorColor() {
        String remoteValue = remoteConfig.getErrorColor();
        return remoteValue != null && !remoteValue.isEmpty() ? remoteValue : "§c";
    }

    public String getWarnColor() {
        String remoteValue = remoteConfig.getWarnColor();
        return remoteValue != null && !remoteValue.isEmpty() ? remoteValue : "§e";
    }

    public String getInfoColor() {
        String remoteValue = remoteConfig.getInfoColor();
        return remoteValue != null && !remoteValue.isEmpty() ? remoteValue : "§7";
    }
}
