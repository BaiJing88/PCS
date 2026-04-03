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

import com.pcs.api.model.PCSConfig;
import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.manager.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 投票操作选择GUI
 * 显示可用的投票操作（Kick/Ban/禁言）
 */
public class VoteActionGUI {
    
    private static final int GUI_SIZE = 27;
    private static final String GUI_TITLE = "选择操作";
    
    // 材质定义
    private static final Material KICK_MATERIAL = Material.IRON_SWORD;
    private static final Material BAN_MATERIAL = Material.DIAMOND_SWORD;
    private static final Material MUTE_MATERIAL = Material.PAPER;
    private static final Material INFO_MATERIAL = Material.BOOK;
    private static final Material CLOSE_MATERIAL = Material.BARRIER;
    
    /**
     * 打开GUI
     */
    public static void open(PCSSpigotPlugin plugin, Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getGuiManager().formatTitle(GUI_TITLE));
        
        // 填充边框
        fillBorder(gui);
        
        // 获取可用操作
        List<PCSConfig.VoteAction> actions = plugin.getConfigManager().getAvailableActions();
        
        // 计算居中位置
        int startSlot = 11;
        if (actions.size() == 3) {
            startSlot = 11; // 位置: 11, 13, 15
        } else if (actions.size() == 2) {
            startSlot = 12; // 位置: 12, 14
        }
        
        // 添加操作按钮
        for (int i = 0; i < actions.size(); i++) {
            PCSConfig.VoteAction action = actions.get(i);
            int slot = startSlot + (i * 2);
            
            Material material = getMaterialForAction(action.getId());
            ItemStack item = createActionItem(material, action);
            gui.setItem(slot, item);
        }
        
        // 添加说明按钮
        gui.setItem(22, createInfoItem(plugin));
        
        // 添加关闭按钮
        gui.setItem(26, createCloseItem());
        
        player.openInventory(gui);
    }
    
    /**
     * 处理点击事件
     */
    public static boolean handleClick(PCSSpigotPlugin plugin, Player player, int slot) {
        List<PCSConfig.VoteAction> actions = plugin.getConfigManager().getAvailableActions();
        
        int startSlot = 11;
        if (actions.size() == 2) {
            startSlot = 12;
        }
        
        // 检查是否点击了操作按钮
        for (int i = 0; i < actions.size(); i++) {
            if (slot == startSlot + (i * 2)) {
                String actionId = actions.get(i).getId();
                
                // 存储选择的操作
                VoteManager.VoteBuilder builder = plugin.getVoteManager().getVoteBuilder(player.getUniqueId());
                builder.setAction(actionId);
                builder.setCurrentStep(1);
                
                // 打开选择玩家界面
                plugin.getGuiManager().openPlayerSelectGUI(player, actionId);
                return true;
            }
        }
        
        // 关闭按钮
        if (slot == 26) {
            plugin.getGuiManager().closeGUI(player);
            plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
            return true;
        }
        
        return false;
    }
    
    /**
     * 填充边框
     */
    private static void fillBorder(Inventory gui) {
        ItemStack borderItem = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 上边框
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
        }
        
        // 下边框
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, borderItem);
        }
        
        // 左右边框
        for (int i = 1; i < 3; i++) {
            gui.setItem(i * 9, borderItem);
            gui.setItem(i * 9 + 8, borderItem);
        }
    }
    
    /**
     * 创建操作按钮
     */
    private static ItemStack createActionItem(Material material, PCSConfig.VoteAction action) {
        String displayName;
        List<String> lore;
        
        switch (action.getId().toUpperCase()) {
            case "KICK":
                displayName = "§e§l踢出玩家";
                lore = Arrays.asList(
                    "§7将目标玩家踢出服务器",
                    "",
                    "§7持续时间: §f即时",
                    "§7影响范围: §f当前服务器",
                    "",
                    "§e点击选择此操作"
                );
                break;
            case "BAN":
                displayName = "§c§l封禁玩家";
                lore = Arrays.asList(
                    "§7在所有服务器封禁该玩家",
                    "",
                    "§7持续时间: §f永久",
                    "§7影响范围: §c所有服务器",
                    "",
                    "§c§l严重违规行为使用",
                    "",
                    "§e点击选择此操作"
                );
                break;
            case "MUTE":
                displayName = "§6§l禁言玩家";
                lore = Arrays.asList(
                    "§7禁止目标玩家发送消息",
                    "",
                    "§7持续时间: §f7天",
                    "§7影响范围: §f所有服务器",
                    "",
                    "§e点击选择此操作"
                );
                break;
            default:
                displayName = "§f" + action.getName();
                lore = Arrays.asList(
                    "§7" + action.getDescription(),
                    "",
                    "§e点击选择此操作"
                );
        }
        
        return createItem(material, displayName, lore.toArray(new String[0]));
    }
    
    /**
     * 创建说明按钮
     * 从配置动态读取数据
     */
    private static ItemStack createInfoItem(PCSSpigotPlugin plugin) {
        // 从配置读取数据
        int minTotalVotes = plugin.getConfigManager().getMinTotalVotes();
        double passRate = plugin.getConfigManager().getPassRate() * 100; // 转换为百分比
        int maxDailyVotes = plugin.getConfigManager().getMaxDailyVotes();
        int voteDuration = plugin.getConfigManager().getVoteDuration();
        
        // 格式化通过率（保留1位小数）
        String passRateStr = String.format("%.1f", passRate);
        
        // 格式化持续时间
        String durationStr;
        if (voteDuration < 60) {
            durationStr = voteDuration + "秒";
        } else {
            durationStr = (voteDuration / 60) + "分钟";
        }
        
        return createItem(INFO_MATERIAL, "§b§l使用说明", 
            "§7请选择一个操作来对违规玩家",
            "§7进行投票处置。",
            "",
            "§7投票规则:",
            " §7• 最少票数: §f" + minTotalVotes + "票",
            " §7• 通过率: §f" + passRateStr + "%",
            " §7• 投票时长: §f" + durationStr,
            "",
            "§7每位玩家每日最多发起 §f" + maxDailyVotes + " §7次投票。"
        );
    }
    
    /**
     * 创建关闭按钮
     */
    private static ItemStack createCloseItem() {
        return createItem(CLOSE_MATERIAL, "§c§l关闭", "§7点击关闭界面");
    }
    
    /**
     * 根据操作类型获取材质
     */
    private static Material getMaterialForAction(String actionId) {
        switch (actionId.toUpperCase()) {
            case "KICK":
                return KICK_MATERIAL;
            case "BAN":
                return BAN_MATERIAL;
            case "MUTE":
                return MUTE_MATERIAL;
            default:
                return Material.STONE;
        }
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
