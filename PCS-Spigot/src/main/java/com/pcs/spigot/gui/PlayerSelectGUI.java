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

package com.pcs.spigot.gui;

import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.manager.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 玩家选择GUI
 * 支持翻页显示在线玩家
 */
public class PlayerSelectGUI {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "选择玩家";
    
    // 槽位定义
    private static final int[] PLAYER_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 50;
    
    /**
     * 打开GUI
     */
    public static void open(PCSSpigotPlugin plugin, Player player, String action, int page) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getGuiManager().formatTitle(GUI_TITLE + " - 第 " + (page + 1) + " 页"));
        
        // 填充边框
        fillBorder(gui);
        
        // 获取在线玩家列表（排除管理员）
        List<Player> onlinePlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player) && p.hasPermission("pcs.vote.participate")) {
                // 如果开启管理员免疫，排除管理员
                if (plugin.getConfigManager().isAdmin(p)) {
                    continue;
                }
                onlinePlayers.add(p);
            }
        }
        
        int playersPerPage = plugin.getConfigManager().getPlayersPerPage();
        int startIndex = page * playersPerPage;
        int endIndex = Math.min(startIndex + PLAYER_SLOTS.length, onlinePlayers.size());
        
        // 添加玩家头颅
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < PLAYER_SLOTS.length; i++) {
            Player target = onlinePlayers.get(i);
            ItemStack skull = createPlayerSkull(target, action);
            gui.setItem(PLAYER_SLOTS[slotIndex], skull);
            slotIndex++;
        }
        
        // 添加翻页按钮
        if (page > 0) {
            gui.setItem(PREV_PAGE_SLOT, createNavigationItem(Material.ARROW, "§e上一页", "§7点击返回上一页"));
        } else {
            gui.setItem(PREV_PAGE_SLOT, createNavigationItem(Material.GRAY_DYE, "§7上一页", "§7已经是第一页"));
        }
        
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / PLAYER_SLOTS.length);
        if (page < totalPages - 1) {
            gui.setItem(NEXT_PAGE_SLOT, createNavigationItem(Material.ARROW, "§e下一页", "§7点击查看下一页"));
        } else {
            gui.setItem(NEXT_PAGE_SLOT, createNavigationItem(Material.GRAY_DYE, "§7下一页", "§7已经是最后一页"));
        }
        
        // 添加返回按钮
        gui.setItem(BACK_SLOT, createNavigationItem(Material.OAK_DOOR, "§e返回", "§7点击返回上一步"));
        
        // 添加信息按钮
        gui.setItem(INFO_SLOT, createInfoItem(action, onlinePlayers.size()));
        
        // 添加关闭按钮
        gui.setItem(CLOSE_SLOT, createNavigationItem(Material.BARRIER, "§c关闭", "§7点击关闭界面"));
        
        player.openInventory(gui);
    }
    
    /**
     * 处理点击事件
     */
    public static boolean handleClick(PCSSpigotPlugin plugin, Player player, int slot, int currentPage) {
        // 上一页
        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            String action = plugin.getVoteManager().getVoteBuilder(player.getUniqueId()).getAction();
            plugin.getGuiManager().previousPage(player, action);
            return true;
        }
        
        // 下一页
        if (slot == NEXT_PAGE_SLOT) {
            int totalPages = plugin.getGuiManager().getTotalPlayerPages();
            if (currentPage < totalPages - 1) {
                String action = plugin.getVoteManager().getVoteBuilder(player.getUniqueId()).getAction();
                plugin.getGuiManager().nextPage(player, action);
            }
            return true;
        }
        
        // 返回
        if (slot == BACK_SLOT) {
            plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
            plugin.getGuiManager().openVoteActionGUI(player);
            return true;
        }
        
        // 关闭
        if (slot == CLOSE_SLOT) {
            plugin.getGuiManager().closeGUI(player);
            plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
            return true;
        }
        
        // 检查是否点击了玩家头颅
        for (int i = 0; i < PLAYER_SLOTS.length; i++) {
            if (slot == PLAYER_SLOTS[i]) {
                // 获取点击的玩家
                Player target = getPlayerAtSlot(plugin, player, currentPage, i);
                if (target != null) {
                    // 检查当前操作类型
                    VoteManager.VoteBuilder builder = plugin.getVoteManager().getVoteBuilder(player.getUniqueId());
                    String action = builder.getAction();
                    
                    if ("RATE".equals(action)) {
                        // 评分操作 - 打开评分界面
                        plugin.getGuiManager().openRatingGUI(player, target.getUniqueId(), target.getName());
                    } else {
                        // 投票操作
                        builder.setTargetUuid(target.getUniqueId());
                        builder.setTargetName(target.getName());
                        builder.setCurrentStep(2);
                        
                        // 打开选择理由界面
                        plugin.getGuiManager().openReasonSelectGUI(player, builder.getAction(), target.getName());
                    }
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取指定槽位的玩家
     */
    private static Player getPlayerAtSlot(PCSSpigotPlugin plugin, Player viewer, int page, int slotIndex) {
        List<Player> onlinePlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(viewer) && p.hasPermission("pcs.vote.participate")) {
                // 如果开启管理员免疫，排除管理员
                if (plugin.getConfigManager().isAdmin(p)) {
                    continue;
                }
                onlinePlayers.add(p);
            }
        }
        
        int playersPerPage = plugin.getConfigManager().getPlayersPerPage();
        int playerIndex = page * playersPerPage + slotIndex;
        
        if (playerIndex >= 0 && playerIndex < onlinePlayers.size()) {
            return onlinePlayers.get(playerIndex);
        }
        return null;
    }
    
    /**
     * 填充边框
     */
    private static void fillBorder(Inventory gui) {
        ItemStack borderItem = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 上下边框
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
            gui.setItem(45 + i, borderItem);
        }
        
        // 左右边框
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, borderItem);
            gui.setItem(i * 9 + 8, borderItem);
        }
    }
    
    /**
     * 创建玩家头颅
     */
    private static ItemStack createPlayerSkull(Player player, String action) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName("§e" + player.getName());
            
            List<String> lore = new ArrayList<>();
            
            if ("RATE".equalsIgnoreCase(action)) {
                lore.add("§7选择此玩家进行评分");
            } else {
                lore.add("§7选择此玩家进行投票");
            }
            lore.add("");
            
            // 添加玩家信息
            com.pcs.api.model.PlayerCredit credit = 
                PCSSpigotPlugin.getInstance().getPlayerDataManager().getPlayerCredit(player.getUniqueId());
            lore.add("§7信用分: " + com.pcs.spigot.util.MessageUtil.createCreditBar(credit.getCreditScore()));
            lore.add("§7Ban次数: §f" + credit.getTotalBans());
            lore.add("§7Kick次数: §f" + credit.getTotalKicks());
            lore.add("");
            lore.add("§e点击选择");
            
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        
        return skull;
    }
    
    /**
     * 创建导航按钮
     */
    private static ItemStack createNavigationItem(Material material, String name, String... lore) {
        return createItem(material, name, lore);
    }
    
    /**
     * 创建信息按钮
     */
    private static ItemStack createInfoItem(String action, int totalPlayers) {
        String actionName;
        String description;
        
        switch (action.toUpperCase()) {
            case "KICK":
                actionName = "§e踢出";
                description = "请选择要投票踢出的玩家";
                break;
            case "BAN":
                actionName = "§c封禁";
                description = "请选择要投票封禁的玩家";
                break;
            case "MUTE":
                actionName = "§6禁言";
                description = "请选择要投票禁言的玩家";
                break;
            case "RATE":
                actionName = "§a评分";
                description = "请选择要评分的玩家";
                break;
            default:
                actionName = "§f" + action;
                description = "请选择玩家";
        }
        
        return createItem(Material.BOOK, "§b§l当前操作: " + actionName,
            "§7" + description,
            "§7在线玩家数: §f" + totalPlayers,
            "",
            "§7点击玩家头颅进入下一步"
        );
    }
    
    /**
     * 创建物品
     */
    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
