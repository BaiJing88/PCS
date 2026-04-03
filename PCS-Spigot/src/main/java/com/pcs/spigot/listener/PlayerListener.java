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

package com.pcs.spigot.listener;

import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.gui.RatingGUI;
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家事件监听器
 */
public class PlayerListener implements Listener {
    
    private final PCSSpigotPlugin plugin;
    
    public PlayerListener(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家登录时检查封禁状态
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否被封禁
        com.pcs.api.model.PlayerCredit credit = 
            plugin.getPlayerDataManager().getPlayerCredit(player.getUniqueId());
        
        if (credit.isCurrentlyBanned()) {
            String banMessage = "§c你已被 PCS 系统封禁！\n" +
                "§7原因: 被投票封禁\n" +
                "§7如有疑问请联系管理员";
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage);
            return;
        }
        
        // 如果有作弊者标记，记录日志
        if (credit.isCheaterTag()) {
            plugin.getLogger().warning("作弊者标记玩家登录: " + player.getName() + 
                " - 原因: " + credit.getCheaterTagReason());
        }
    }
    
    /**
     * 玩家加入时更新数据
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 更新玩家名称
        com.pcs.api.model.PlayerCredit credit = 
            plugin.getPlayerDataManager().getPlayerCredit(player.getUniqueId());
        credit.setPlayerName(player.getName());
        
        // 从服务器获取最新数据
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
            plugin.getWebSocketClient().requestPlayerData(player.getUniqueId());
        }
        
        // 如果玩家有低信用分警告
        if (credit.getCreditScore() < 30) {
            plugin.getLogger().info("低信用分玩家加入: " + player.getName() + 
                " (分数: " + credit.getCreditScore() + ")");
        }
        
        // 通知玩家当前状态
        if (credit.isCheaterTag()) {
            MessageUtil.warn(player, "你的账号被标记为作弊者，请遵守服务器规则！");
        }
    }
    
    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 清理GUI状态
        plugin.getGuiManager().clearPlayerData(player.getUniqueId());
        plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
    }
    
    /**
     * 聊天事件 - 检查禁言状态和评分评论输入
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查是否是评分评论输入
        if (RatingGUI.isWaitingForComment(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            
            // 在主线程处理评分提交
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                RatingGUI.handleChatInput(player, message);
            });
            return;
        }
        
        // 检查是否被禁言
        if (plugin.getPlayerDataManager().isMuted(player.getUniqueId())) {
            long remaining = plugin.getPlayerDataManager().getMuteRemaining(player.getUniqueId());
            long days = remaining / (24 * 60 * 60 * 1000);
            long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
            
            String timeStr;
            if (days > 0) {
                timeStr = days + " 天 " + hours + " 小时";
            } else if (hours > 0) {
                long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
                timeStr = hours + " 小时 " + minutes + " 分钟";
            } else {
                long minutes = remaining / (60 * 1000);
                timeStr = minutes + " 分钟";
            }
            
            event.setCancelled(true);
            MessageUtil.error(player, "你当前处于禁言状态！");
            MessageUtil.info(player, "剩余禁言时间: " + timeStr);
        }
    }
}
