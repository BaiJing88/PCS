/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 */

package com.pcs.spigot.gui;

import com.pcs.api.model.RatingInfo;
import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 评分GUI
 * 为其他玩家打分 (1-10分)
 */
public class RatingGUI {
    
    private static final int GUI_SIZE = 27;
    private static final String GUI_TITLE = "为玩家评分";
    
    // 存储正在进行的评分会话
    private static final Map<UUID, RatingSession> ratingSessions = new HashMap<>();
    
    // 存储等待输入评论的玩家（评分后进入评论输入模式）
    private static final Map<UUID, PendingComment> pendingComments = new HashMap<>();
    
    private static PCSSpigotPlugin pluginInstance;
    
    /**
     * 打开评分GUI
     */
    public static void open(PCSSpigotPlugin plugin, Player rater, UUID targetUuid, String targetName) {
        // 保存插件实例用于评论输入处理
        if (pluginInstance == null) {
            pluginInstance = plugin;
        }
        
        // 检查是否已评分过
        if (!plugin.getPlayerDataManager().canRatePlayer(rater, targetUuid)) {
            MessageUtil.error(rater, "你今天已经评分过该玩家了！");
            return;
        }
        
        // 检查是否是给自己评分
        if (rater.getUniqueId().equals(targetUuid)) {
            MessageUtil.error(rater, "不能给自己评分！");
            return;
        }
        
        // 创建评分会话
        RatingSession session = new RatingSession(targetUuid, targetName);
        ratingSessions.put(rater.getUniqueId(), session);
        
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getGuiManager().formatTitle(GUI_TITLE + " - " + targetName));
        
        // 填充边框
        fillBorder(gui);
        
        // 添加分数按钮 (1-10)
        for (int i = 1; i <= 10; i++) {
            int slot = getScoreSlot(i);
            gui.setItem(slot, createScoreItem(i));
        }
        
        // 添加被评分玩家信息
        gui.setItem(4, createTargetInfoItem(targetName));
        
        // 添加跳过评论按钮
        gui.setItem(21, createSkipCommentItem());
        
        // 添加关闭按钮
        gui.setItem(22, createCloseItem());
        
        rater.openInventory(gui);
    }
    
    /**
     * 处理点击事件
     */
    public static boolean handleClick(PCSSpigotPlugin plugin, Player player, int slot) {
        RatingSession session = ratingSessions.get(player.getUniqueId());
        if (session == null) return false;
        
        // 关闭按钮
        if (slot == 22) {
            plugin.getGuiManager().closeGUI(player);
            ratingSessions.remove(player.getUniqueId());
            return true;
        }
        
        // 跳过评论按钮
        if (slot == 21) {
            int score = session.getSelectedScore();
            if (score > 0) {
                submitRating(plugin, player, session, score, "");
            } else {
                MessageUtil.error(player, "请先选择分数！");
            }
            return true;
        }
        
        // 检查是否点击了分数按钮
        int score = getScoreFromSlot(slot);
        if (score > 0) {
            // 保存选择的分数，进入评论输入模式
            session.setSelectedScore(score);
            ratingSessions.put(player.getUniqueId(), session);
            
            // 关闭GUI并提示输入评论
            plugin.getGuiManager().closeGUI(player);
            
            // 进入评论输入等待模式
            pendingComments.put(player.getUniqueId(), new PendingComment(session, score));
            
            MessageUtil.info(player, "§e你选择了 §6" + score + " §e分");
            MessageUtil.info(player, "§7请在聊天框输入评论（30秒内），或输入 §cskip §7跳过");
            
            // 30秒后自动取消评论输入
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (pendingComments.remove(player.getUniqueId()) != null) {
                    MessageUtil.warn(player, "评论输入超时，评分已提交（无评论）");
                    submitRating(plugin, player, session, score, "");
                }
            }, 20 * 30); // 30秒
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 处理玩家聊天输入（用于评论）
     * @return true 如果处理了该消息
     */
    public static boolean handleChatInput(Player player, String message) {
        PendingComment pending = pendingComments.remove(player.getUniqueId());
        if (pending == null) return false;
        
        String comment;
        if (message.equalsIgnoreCase("skip")) {
            comment = "";
            MessageUtil.info(player, "§7已跳过评论");
        } else {
            comment = message;
            if (comment.length() > 100) {
                comment = comment.substring(0, 100);
                MessageUtil.warn(player, "评论过长，已截断至100字符");
            }
        }
        
        // 提交评分
        submitRating(pluginInstance, player, pending.getSession(), pending.getScore(), comment);
        return true;
    }
    
    /**
     * 检查玩家是否正在等待输入评论
     */
    public static boolean isWaitingForComment(UUID playerUuid) {
        return pendingComments.containsKey(playerUuid);
    }
    
    /**
     * 提交评分
     */
    private static void submitRating(PCSSpigotPlugin plugin, Player rater, RatingSession session, int score, String comment) {
        // 发送到中控
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isAuthenticated()) {
            plugin.getWebSocketClient().sendRating(
                rater.getUniqueId(),
                rater.getName(),
                session.getTargetUuid(),
                session.getTargetName(),
                score,
                comment
            );
            
            // 本地记录评分
            plugin.getPlayerDataManager().recordRating(rater, session.getTargetUuid());
            
            String msg = "你给了 " + session.getTargetName() + " " + score + " 分！";
            if (!comment.isEmpty()) {
                msg += " §7(附评论)";
            }
            MessageUtil.success(rater, msg);
        } else {
            MessageUtil.error(rater, "中控服务器未连接，无法提交评分！");
        }
        
        plugin.getGuiManager().closeGUI(rater);
        ratingSessions.remove(rater.getUniqueId());
    }
    
    /**
     * 获取分数对应的槽位
     */
    private static int getScoreSlot(int score) {
        // 1-5 放在第二行 (9-13)
        // 6-10 放在第三行 (18-22)
        if (score <= 5) {
            return 9 + score;
        } else {
            return 13 + score;
        }
    }
    
    /**
     * 从槽位获取分数
     */
    private static int getScoreFromSlot(int slot) {
        if (slot >= 10 && slot <= 14) {
            return slot - 9;
        } else if (slot >= 19 && slot <= 23) {
            return slot - 13;
        }
        return 0;
    }
    
    /**
     * 创建分数按钮
     */
    private static ItemStack createScoreItem(int score) {
        Material material;
        String color;
        
        if (score >= 8) {
            material = Material.EMERALD_BLOCK;
            color = "a";
        } else if (score >= 5) {
            material = Material.GOLD_BLOCK;
            color = "e";
        } else if (score >= 3) {
            material = Material.COPPER_BLOCK;
            color = "6";
        } else {
            material = Material.REDSTONE_BLOCK;
            color = "c";
        }
        
        return createItem(material, "&" + color + "&l" + score + "分",
            "&7点击给出 " + score + " 分"
        );
    }
    
    /**
     * 创建被评分玩家信息物品
     */
    private static ItemStack createTargetInfoItem(String targetName) {
        return createItem(Material.PLAYER_HEAD, "&e&l被评分玩家",
            "&7玩家: &f" + targetName,
            "",
            "&7请选择 1-10 分为该玩家评分"
        );
    }
    
    /**
     * 创建关闭按钮
     */
    private static ItemStack createCloseItem() {
        return createItem(Material.BARRIER, "&c&l关闭", "&7点击关闭界面");
    }
    
    /**
     * 创建跳过评论按钮
     */
    private static ItemStack createSkipCommentItem() {
        return createItem(Material.PAPER, "&e&l跳过评论", 
            "&7点击直接提交评分",
            "&7（不添加评论）"
        );
    }
    
    /**
     * 填充边框
     */
    private static void fillBorder(Inventory gui) {
        ItemStack borderItem = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
            gui.setItem(18 + i, borderItem);
        }
    }
    
    /**
     * 创建物品
     */
    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name.replace("&", "§"));
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore).stream()
                    .map(s -> s.replace("&", "§"))
                    .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 评分会话类
     */
    private static class RatingSession {
        private final UUID targetUuid;
        private final String targetName;
        private int selectedScore = 0;
        
        public RatingSession(UUID targetUuid, String targetName) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
        }
        
        public UUID getTargetUuid() {
            return targetUuid;
        }
        
        public String getTargetName() {
            return targetName;
        }
        
        public int getSelectedScore() {
            return selectedScore;
        }
        
        public void setSelectedScore(int score) {
            this.selectedScore = score;
        }
    }
    
    /**
     * 等待评论的评分信息
     */
    private static class PendingComment {
        private final RatingSession session;
        private final int score;
        
        public PendingComment(RatingSession session, int score) {
            this.session = session;
            this.score = score;
        }
        
        public RatingSession getSession() {
            return session;
        }
        
        public int getScore() {
            return score;
        }
    }
}
