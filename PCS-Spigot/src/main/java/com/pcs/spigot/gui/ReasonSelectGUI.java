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
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 理由选择GUI
 * 显示可用的投票理由
 */
public class
ReasonSelectGUI {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "选择理由";
    
    // 槽位定义
    private static final int[] REASON_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    
    private static final int BACK_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 50;
    
    /**
     * 打开GUI
     */
    public static void open(PCSSpigotPlugin plugin, Player player, String action, String targetName) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getGuiManager().formatTitle(GUI_TITLE));
        
        // 填充边框
        fillBorder(gui);
        
        // 获取可用理由
        List<String> reasons = plugin.getConfigManager().getAvailableReasons();
        
        // 添加理由按钮
        for (int i = 0; i < reasons.size() && i < REASON_SLOTS.length; i++) {
            String reason = reasons.get(i);
            ItemStack item = createReasonItem(reason, i + 1);
            gui.setItem(REASON_SLOTS[i], item);
        }
        
        // 添加返回按钮
        gui.setItem(BACK_SLOT, createNavigationItem(Material.OAK_DOOR, "§e返回", "§7点击返回上一步"));
        
        // 添加信息按钮
        gui.setItem(INFO_SLOT, createInfoItem(action, targetName));
        
        // 添加关闭按钮
        gui.setItem(CLOSE_SLOT, createNavigationItem(Material.BARRIER, "§c关闭", "§7点击关闭界面"));
        
        player.openInventory(gui);
    }
    
    /**
     * 处理点击事件
     */
    public static boolean handleClick(PCSSpigotPlugin plugin, Player player, int slot) {
        // 返回
        if (slot == BACK_SLOT) {
            VoteManager.VoteBuilder builder = plugin.getVoteManager().getVoteBuilder(player.getUniqueId());
            plugin.getGuiManager().openPlayerSelectGUI(player, builder.getAction(), 
                plugin.getGuiManager().getCurrentPage(player));
            return true;
        }
        
        // 关闭
        if (slot == CLOSE_SLOT) {
            plugin.getGuiManager().closeGUI(player);
            plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
            return true;
        }
        
        // 检查是否点击了理由
        List<String> reasons = plugin.getConfigManager().getAvailableReasons();
        for (int i = 0; i < reasons.size() && i < REASON_SLOTS.length; i++) {
            if (slot == REASON_SLOTS[i]) {
                String selectedReason = reasons.get(i);
                
                // 完成投票构建
                VoteManager.VoteBuilder builder = plugin.getVoteManager().getVoteBuilder(player.getUniqueId());
                builder.setReason(selectedReason);
                
                // 开始投票
                startVote(plugin, player, builder);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 开始投票
     */
    private static void startVote(PCSSpigotPlugin plugin, Player player, VoteManager.VoteBuilder builder) {
        Player target = Bukkit.getPlayer(builder.getTargetUuid());
        
        if (target == null || !target.isOnline()) {
            MessageUtil.error(player, "目标玩家已离线！");
            plugin.getGuiManager().closeGUI(player);
            plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
            return;
        }
        
        // 关闭GUI
        plugin.getGuiManager().closeGUI(player);
        
        // 开始投票
        boolean success = plugin.getVoteManager().startVote(
            player, 
            target, 
            builder.getAction(), 
            builder.getReason()
        );
        
        if (success) {
            MessageUtil.success(player, "投票发起成功！");
        }
        
        // 清除投票构建器
        plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
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
     * 创建理由按钮
     */
    private static ItemStack createReasonItem(String reason, int number) {
        Material material;
        String color;
        
        // 根据理由类型选择不同材质和颜色
        if (reason.contains("作弊") || reason.contains("开挂") || reason.contains("外挂")) {
            material = Material.DIAMOND_SWORD;
            color = "§c";
        } else if (reason.contains("破坏") || reason.contains("盗窃") || reason.contains("TNT")) {
            material = Material.TNT;
            color = "§6";
        } else if (reason.contains("辱骂") || reason.contains("攻击") || reason.contains("歧视")) {
            material = Material.PAPER;
            color = "§e";
        } else if (reason.contains("刷屏") || reason.contains("宣传")) {
            material = Material.BOOK;
            color = "§a";
        } else {
            material = Material.EMERALD;
            color = "§b";
        }
        
        return createItem(material, color + "§l理由 #" + number,
            "§7" + reason,
            "",
            "§e点击选择此理由发起投票"
        );
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
    private static ItemStack createInfoItem(String action, String targetName) {
        String actionName;
        String actionColor;
        switch (action.toUpperCase()) {
            case "KICK":
                actionName = "踢出";
                actionColor = "§e";
                break;
            case "BAN":
                actionName = "封禁";
                actionColor = "§c";
                break;
            case "MUTE":
                actionName = "禁言";
                actionColor = "§6";
                break;
            default:
                actionName = action;
                actionColor = "§f";
        }
        
        return createItem(Material.BOOK, "§b§l投票信息",
            "§7目标玩家: §e" + targetName,
            "§7操作类型: " + actionColor + actionName,
            "",
            "§7请选择一个理由来发起投票",
            "§7理由需要真实有效，恶意投票",
            "§7将会影响你的信用分！"
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
