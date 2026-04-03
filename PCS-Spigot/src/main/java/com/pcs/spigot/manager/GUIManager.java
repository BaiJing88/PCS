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

import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.gui.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI管理器
 * 管理所有GUI界面和玩家GUI状态
 */
public class GUIManager {
    
    private final PCSSpigotPlugin plugin;
    
    // 存储玩家当前打开的GUI类型
    private final Map<UUID, GUIType> playerOpenGUI;
    
    // 存储玩家GUI的页码数据
    private final Map<UUID, Integer> playerPage;
    
    // GUI标题前缀
    private final String titlePrefix;
    
    public enum GUIType {
        VOTE_ACTION,      // 选择操作界面
        PLAYER_SELECT,    // 选择玩家界面
        REASON_SELECT,    // 选择理由界面
        CREDIT_QUERY,     // 信用查询界面
        VOTE_CONFIRM,     // 投票确认界面
        RATING            // 评分界面
    }
    
    public GUIManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.playerOpenGUI = new HashMap<>();
        this.playerPage = new HashMap<>();
        this.titlePrefix = plugin.getConfig().getString("gui.title-prefix", "§8[§6PCS§8] ");
    }
    
    /**
     * 打开选择操作界面
     */
    public void openVoteActionGUI(Player player) {
        playerOpenGUI.put(player.getUniqueId(), GUIType.VOTE_ACTION);
        VoteActionGUI.open(plugin, player);
    }
    
    /**
     * 打开选择玩家界面
     */
    public void openPlayerSelectGUI(Player player, String action) {
        playerOpenGUI.put(player.getUniqueId(), GUIType.PLAYER_SELECT);
        playerPage.put(player.getUniqueId(), 0);
        PlayerSelectGUI.open(plugin, player, action, 0);
    }
    
    /**
     * 打开选择玩家界面（指定页码）
     */
    public void openPlayerSelectGUI(Player player, String action, int page) {
        playerOpenGUI.put(player.getUniqueId(), GUIType.PLAYER_SELECT);
        playerPage.put(player.getUniqueId(), page);
        PlayerSelectGUI.open(plugin, player, action, page);
    }
    
    /**
     * 打开选择理由界面
     */
    public void openReasonSelectGUI(Player player, String action, String targetName) {
        playerOpenGUI.put(player.getUniqueId(), GUIType.REASON_SELECT);
        ReasonSelectGUI.open(plugin, player, action, targetName);
    }
    
    /**
     * 打开信用查询界面
     */
    public void openCreditQueryGUI(Player player, String targetName) {
        playerOpenGUI.put(player.getUniqueId(), GUIType.CREDIT_QUERY);
        CreditQueryGUI.open(plugin, player, targetName);
    }
    
    /**
     * 打开评分界面
     */
    public void openRatingGUI(Player player, UUID targetUuid, String targetName) {
        playerOpenGUI.put(player.getUniqueId(), GUIType.RATING);
        RatingGUI.open(plugin, player, targetUuid, targetName);
    }
    
    /**
     * 翻页（下一页）
     */
    public void nextPage(Player player, String action) {
        int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);
        int totalPages = getTotalPlayerPages();
        
        if (currentPage < totalPages - 1) {
            openPlayerSelectGUI(player, action, currentPage + 1);
        }
    }
    
    /**
     * 翻页（上一页）
     */
    public void previousPage(Player player, String action) {
        int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);
        
        if (currentPage > 0) {
            openPlayerSelectGUI(player, action, currentPage - 1);
        }
    }
    
    /**
     * 关闭玩家GUI
     */
    public void closeGUI(Player player) {
        playerOpenGUI.remove(player.getUniqueId());
        playerPage.remove(player.getUniqueId());
        player.closeInventory();
    }
    
    /**
     * 打开评分GUI
     */
    // ...existing code...
    
    /**
     * 获取玩家当前打开的GUI类型
     */
    public GUIType getOpenGUIType(Player player) {
        return playerOpenGUI.get(player.getUniqueId());
    }
    
    /**
     * 获取玩家当前页码
     */
    public int getCurrentPage(Player player) {
        return playerPage.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * 获取总页数
     */
    public int getTotalPlayerPages() {
        int playersPerPage = plugin.getConfigManager().getPlayersPerPage();
        int totalPlayers = plugin.getServer().getOnlinePlayers().size();
        return (int) Math.ceil((double) totalPlayers / playersPerPage);
    }
    
    /**
     * 获取GUI标题前缀
     */
    public String getTitlePrefix() {
        return titlePrefix;
    }
    
    /**
     * 格式化GUI标题
     */
    public String formatTitle(String title) {
        return titlePrefix + title;
    }
    
    /**
     * 检查玩家是否打开了某个GUI
     */
    public boolean isInGUI(Player player, GUIType type) {
        GUIType openType = playerOpenGUI.get(player.getUniqueId());
        return openType == type;
    }
    
    /**
     * 检查玩家是否打开了任何PCS GUI
     */
    public boolean isInAnyGUI(Player player) {
        return playerOpenGUI.containsKey(player.getUniqueId());
    }
    
    /**
     * 清除玩家GUI状态
     */
    public void clearPlayerData(UUID playerUuid) {
        playerOpenGUI.remove(playerUuid);
        playerPage.remove(playerUuid);
    }
}
