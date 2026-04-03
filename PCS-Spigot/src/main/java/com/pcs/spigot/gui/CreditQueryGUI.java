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

import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.RatingInfo;
import com.pcs.api.model.VoteHistory;
import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 信用查询GUI
 * 显示玩家的信用信息、历史记录等
 */
public class CreditQueryGUI {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "信用查询";
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    // 槽位定义
    private static final int HEAD_SLOT = 13;
    private static final int CREDIT_SCORE_SLOT = 20;
    private static final int BAN_COUNT_SLOT = 21;
    private static final int KICK_COUNT_SLOT = 22;
    private static final int STATUS_SLOT = 23;
    private static final int HISTORY_START_SLOT = 28;
    
    private static final int CLOSE_SLOT = 49;
    
    /**
     * 打开GUI
     */
    public static void open(PCSSpigotPlugin plugin, Player player, String targetName) {
        // 获取目标玩家
        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;
        String displayName;
        OfflinePlayer offlinePlayer = null;
        
        if (target != null) {
            targetUuid = target.getUniqueId();
            displayName = target.getName();
        } else {
            // 尝试获取离线玩家
            offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore()) {
                targetUuid = offlinePlayer.getUniqueId();
                displayName = offlinePlayer.getName();
            } else {
                MessageUtil.error(player, "未找到玩家: " + targetName);
                return;
            }
        }
        
        // 获取信用数据
        PlayerCredit credit = plugin.getPlayerDataManager().getPlayerCredit(targetUuid, displayName);
        
        // 从服务器获取最新数据
        plugin.getPlayerDataManager().fetchPlayerCreditFromServer(targetUuid, displayName);
        
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getGuiManager().formatTitle(GUI_TITLE + " - " + displayName));
        
        // 填充边框
        fillBorder(gui);
        
        // 玩家头颅
        gui.setItem(HEAD_SLOT, createPlayerHead(target, offlinePlayer, displayName, credit));
        
        // 信用分
        gui.setItem(CREDIT_SCORE_SLOT, createCreditScoreItem(credit));
        
        // Ban次数
        gui.setItem(BAN_COUNT_SLOT, createBanCountItem(credit));
        
        // Kick次数
        gui.setItem(KICK_COUNT_SLOT, createKickCountItem(credit));
        
        // 当前状态
        gui.setItem(STATUS_SLOT, createStatusItem(credit));
        
        // 历史记录
        addHistoryItems(gui, credit);
        
        // 关闭按钮
        gui.setItem(CLOSE_SLOT, createCloseItem());
        
        player.openInventory(gui);
    }
    
    /**
     * 处理点击事件
     */
    public static boolean handleClick(PCSSpigotPlugin plugin, Player player, int slot) {
        if (slot == CLOSE_SLOT) {
            plugin.getGuiManager().closeGUI(player);
            return true;
        }
        return false;
    }
    
    /**
     * 填充边框
     */
    private static void fillBorder(Inventory gui) {
        ItemStack borderItem = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
            gui.setItem(45 + i, borderItem);
        }
        
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, borderItem);
            gui.setItem(i * 9 + 8, borderItem);
        }
    }
    
    /**
     * 创建玩家头颅
     */
    private static ItemStack createPlayerHead(Player online, OfflinePlayer offline, String name, PlayerCredit credit) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            if (online != null) {
                meta.setOwningPlayer(online);
            } else if (offline != null) {
                meta.setOwningPlayer(offline);
            }
            
            meta.setDisplayName("§e§l" + name);
            
            List<String> lore = new ArrayList<>();
            lore.add("§8§m                    §r");
            lore.add("§7UUID: §f" + credit.getPlayerUuid());
            
            if (credit.getCreatedAt() != null) {
                lore.add("§7首次记录: §f" + DATE_FORMAT.format(credit.getCreatedAt()));
            }
            
            if (credit.getUpdatedAt() != null) {
                lore.add("§7最后更新: §f" + DATE_FORMAT.format(credit.getUpdatedAt()));
            }
            
            lore.add("§8§m                    §r");
            
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        
        return skull;
    }
    
    /**
     * 创建信用分物品 (10分制)
     */
    private static ItemStack createCreditScoreItem(PlayerCredit credit) {
        double score = credit.getCreditScore();
        Material material;
        String color;
        String status;
        
        if (score >= 8) {
            material = Material.EMERALD_BLOCK;
            color = "§a";
            status = "优秀";
        } else if (score >= 5) {
            material = Material.GOLD_BLOCK;
            color = "§e";
            status = "良好";
        } else if (score >= 3) {
            material = Material.COPPER_BLOCK;
            color = "§6";
            status = "一般";
        } else {
            material = Material.REDSTONE_BLOCK;
            color = "§c";
            status = "较差";
        }
        
        return createItem(material, color + "§l信用分: " + String.format("%.1f", score) + "/10",
            "§7当前状态: " + color + status,
            "",
            MessageUtil.createCreditBar(score),
            "",
            "§7信用分影响:",
            " §a8-10 §7优秀玩家",
            " §e5-7  §7普通玩家", 
            " §63-4  §7需关注",
            " §c0-2  §7高风险"
        );
    }
    
    /**
     * 创建Ban次数物品
     */
    private static ItemStack createBanCountItem(PlayerCredit credit) {
        int bans = credit.getTotalBans();
        Material material = bans > 0 ? Material.RED_WOOL : Material.GREEN_WOOL;
        String color = bans > 0 ? "§c" : "§a";
        
        return createItem(material, color + "§l封禁次数: " + bans,
            "§7该玩家历史被封禁次数",
            "",
            bans > 0 ? "§c该玩家有违规记录" : "§a该玩家无封禁记录"
        );
    }
    
    /**
     * 创建Kick次数物品
     */
    private static ItemStack createKickCountItem(PlayerCredit credit) {
        int kicks = credit.getTotalKicks();
        Material material = kicks > 0 ? Material.YELLOW_WOOL : Material.GREEN_WOOL;
        String color = kicks > 3 ? "§c" : (kicks > 0 ? "§e" : "§a");
        
        return createItem(material, color + "§l踢出次数: " + kicks,
            "§7该玩家历史被踢出次数",
            "",
            kicks > 3 ? "§c该玩家经常被踢出" : 
                (kicks > 0 ? "§e该玩家偶尔被踢出" : "§a该玩家无踢出记录")
        );
    }
    
    /**
     * 创建状态物品
     */
    private static ItemStack createStatusItem(PlayerCredit credit) {
        List<String> lore = new ArrayList<>();
        
        if (credit.isCurrentlyBanned()) {
            lore.add("§c§l当前被封禁");
            lore.add("§7该玩家正处于封禁状态");
        } else {
            lore.add("§a§l当前正常");
            lore.add("§7该玩家目前可以正常游戏");
        }
        
        if (credit.isCheaterTag()) {
            lore.add("");
            lore.add("§4§l⚠ 作弊者标记");
            lore.add("§7原因: §c" + credit.getCheaterTagReason());
            if (credit.getCheaterTagDate() != null) {
                lore.add("§7时间: §f" + DATE_FORMAT.format(credit.getCheaterTagDate()));
            }
        }
        
        Material material = credit.isCurrentlyBanned() ? Material.RED_CONCRETE : Material.LIME_CONCRETE;
        String name = credit.isCurrentlyBanned() ? "§c§l封禁状态" : "§a§l正常状态";
        
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * 添加历史记录物品（投票历史 + 评分历史）
     */
    private static void addHistoryItems(Inventory gui, PlayerCredit credit) {
        List<VoteHistory> voteHistory = credit.getVoteHistory();
        List<RatingInfo> ratingHistory = credit.getRatingHistory();
        
        boolean hasVote = voteHistory != null && !voteHistory.isEmpty();
        boolean hasRating = ratingHistory != null && !ratingHistory.isEmpty();
        
        if (!hasVote && !hasRating) {
            gui.setItem(HISTORY_START_SLOT, createItem(Material.PAPER, "§7§l无历史记录",
                "§7该玩家暂无投票或评分历史记录"
            ));
            return;
        }
        
        int currentSlot = HISTORY_START_SLOT;
        
        // 展示投票历史（最多4条）
        if (hasVote) {
            int voteLimit = Math.min(voteHistory.size(), 4);
            for (int i = 0; i < voteLimit && currentSlot < 44; i++) {
                VoteHistory record = voteHistory.get(i);
                int slot = currentSlot;
                if (slot % 9 == 0) slot++;
                if (slot % 9 == 8) slot++;
                if (slot < 44) {
                    gui.setItem(slot, createVoteHistoryItem(record, i + 1));
                }
                currentSlot = slot + 1;
            }
        }
        
        // 展示评分历史（最多4条）
        if (hasRating) {
            int ratingLimit = Math.min(ratingHistory.size(), 4);
            for (int i = 0; i < ratingLimit && currentSlot < 44; i++) {
                RatingInfo rating = ratingHistory.get(i);
                int slot = currentSlot;
                if (slot % 9 == 0) slot++;
                if (slot % 9 == 8) slot++;
                if (slot < 44) {
                    gui.setItem(slot, createRatingHistoryItem(rating, i + 1));
                }
                currentSlot = slot + 1;
            }
        }
    }
    
    /**
     * 创建投票历史记录物品
     */
    private static ItemStack createVoteHistoryItem(VoteHistory history, int number) {
        Material material;
        String actionColor;
        String resultColor;
        
        switch (history.getAction().toUpperCase()) {
            case "BAN":
                material = Material.DIAMOND_SWORD;
                actionColor = "§c";
                break;
            case "KICK":
                material = Material.IRON_SWORD;
                actionColor = "§e";
                break;
            case "MUTE":
                material = Material.PAPER;
                actionColor = "§6";
                break;
            default:
                material = Material.BOOK;
                actionColor = "§f";
        }
        
        resultColor = history.isPassed() ? "§a通过" : "§c未通过";
        
        String dateStr = history.getStartTime() != null ? 
            DATE_FORMAT.format(history.getStartTime()) : "未知时间";
        
        return createItem(material, "§7投票 #" + number + " " + actionColor + history.getAction(),
            "§7时间: §f" + dateStr,
            "§7结果: " + resultColor,
            "§7理由: §f" + history.getReason(),
            "§7投票: §a" + history.getYesVotes() + " §7| §c" + history.getNoVotes()
        );
    }
    
    /**
     * 创建评分历史记录物品（预留接口）
     */
    private static ItemStack createRatingHistoryItem(RatingInfo rating, int number) {
        Material material;
        String scoreColor;
        
        if (rating.getScore() >= 8) {
            material = Material.EMERALD;
            scoreColor = "§a";
        } else if (rating.getScore() >= 5) {
            material = Material.GOLD_INGOT;
            scoreColor = "§e";
        } else if (rating.getScore() >= 3) {
            material = Material.COPPER_INGOT;
            scoreColor = "§6";
        } else {
            material = Material.REDSTONE;
            scoreColor = "§c";
        }
        
        String dateStr = rating.getRatedAt() != null ? 
            DATE_FORMAT.format(rating.getRatedAt()) : "未知时间";
        
        List<String> lore = new ArrayList<>();
        lore.add("§7评分者: §f" + rating.getRaterName());
        lore.add("§7分数: " + scoreColor + rating.getScore() + "/10");
        lore.add("§7时间: §f" + dateStr);
        
        if (rating.getComment() != null && !rating.getComment().isEmpty()) {
            lore.add("");
            lore.add("§7评论: §f" + (rating.getComment().length() > 30 ? 
                rating.getComment().substring(0, 30) + "..." : rating.getComment()));
        }
        
        return createItem(material, "§7评分 #" + number + " " + scoreColor + rating.getScore() + "分",
            lore.toArray(new String[0])
        );
    }
    
    /**
     * 创建关闭按钮
     */
    private static ItemStack createCloseItem() {
        return createItem(Material.BARRIER, "§c§l关闭", "§7点击关闭界面");
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
