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

import com.pcs.api.model.VoteHistory;
import com.pcs.api.model.VoteRecord;
import com.pcs.api.model.VoteSession;
import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.util.MessageUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 投票管理器
 * 管理投票会话、处理投票逻辑
 */
public class VoteManager {
    
    private final PCSSpigotPlugin plugin;
    
    // 活跃的投票会话
    private final Map<String, VoteSession> activeVotes;
    
    // 玩家当前参与的投票
    private final Map<UUID, String> playerActiveVote;
    
    // 投票任务
    private final Map<String, BukkitTask> voteTasks;
    
    // 投票发起者的临时数据存储
    private final Map<UUID, VoteBuilder> voteBuilders;
    
    public VoteManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.activeVotes = new ConcurrentHashMap<>();
        this.playerActiveVote = new ConcurrentHashMap<>();
        this.voteTasks = new ConcurrentHashMap<>();
        this.voteBuilders = new ConcurrentHashMap<>();
    }
    
    /**
     * 开始一个新的投票
     * 
     * @param initiator 发起者
     * @param target 目标玩家
     * @param action 操作 (KICK/BAN/MUTE)
     * @param reason 理由
     * @return 是否成功开始
     */
    public boolean startVote(Player initiator, Player target, String action, String reason) {
        // 检查是否已有活跃投票
        if (playerActiveVote.containsKey(initiator.getUniqueId())) {
            MessageUtil.error(initiator, "你已经有一个进行中的投票！");
            return false;
        }
        
        // 检查在线人数
        int onlineCount = plugin.getServer().getOnlinePlayers().size();
        if (onlineCount < plugin.getConfigManager().getMinPlayersToStart()) {
            MessageUtil.error(initiator, "在线人数不足，无法发起投票！需要至少 " + 
                plugin.getConfigManager().getMinPlayersToStart() + " 人在线。");
            return false;
        }
        
        // 检查发起者权限
        if (!plugin.getPlayerDataManager().canStartVote(initiator)) {
            long cooldown = plugin.getPlayerDataManager().getVoteCooldownRemaining(initiator);
            if (cooldown > 0) {
                MessageUtil.error(initiator, "投票冷却中，请等待 " + cooldown + " 秒后再试。");
            } else {
                MessageUtil.error(initiator, "你今日的发起投票次数已用完！");
            }
            return false;
        }
        
        // 【新增】检查目标玩家是否为管理员（管理员免疫投票）
        if (plugin.getConfigManager().isAdmin(target)) {
            MessageUtil.error(initiator, "该玩家是管理员，不能被投票！");
            return false;
        }
        
        // 创建投票会话
        VoteSession session = new VoteSession();
        session.setTargetPlayerUuid(target.getUniqueId());
        session.setTargetPlayerName(target.getName());
        session.setInitiatorUuid(initiator.getUniqueId());
        session.setInitiatorName(initiator.getName());
        session.setAction(action.toUpperCase());
        session.setReason(reason);
        session.setServerId(plugin.getConfigManager().getServerId());
        session.setServerName(plugin.getConfigManager().getServerName());
        session.setDurationSeconds(plugin.getConfigManager().getVoteDuration());
        
        // 存储投票
        activeVotes.put(session.getSessionId(), session);
        playerActiveVote.put(initiator.getUniqueId(), session.getSessionId());
        
        // 记录发起者投票次数
        plugin.getPlayerDataManager().recordVote(initiator);
        
        // 广播投票开始（仅本服务器）
        broadcastVoteStart(session);
        
        // 启动投票计时器
        startVoteTimer(session);
        
        // 【移除跨服投票】不再发送到中控服务器
        // if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
        //     plugin.getWebSocketClient().sendVoteStart(session);
        // }
        
        return true;
    }
    
    /**
     * 玩家投票
     */
    public boolean castVote(Player voter, String voteId, boolean agree) {
        VoteSession session = activeVotes.get(voteId);
        if (session == null) {
            MessageUtil.error(voter, "该投票不存在或已结束！");
            return false;
        }
        
        if (session.hasVoted(voter.getUniqueId())) {
            MessageUtil.error(voter, "你已经投过票了！");
            return false;
        }
        
        if (session.isExpired()) {
            MessageUtil.error(voter, "该投票已过期！");
            endVote(voteId, false);
            return false;
        }
        
        // 创建投票记录
        VoteRecord record = new VoteRecord(
            voter.getUniqueId(),
            voter.getName(),
            agree,
            plugin.getConfigManager().getServerId()
        );
        
        session.addVote(record);
        
        // 发送确认消息
        String action = plugin.getConfigManager().getActionDisplayName(session.getAction());
        if (agree) {
            MessageUtil.success(voter, "你已投票支持对 §e" + session.getTargetPlayerName() + " §a执行 " + action);
        } else {
            MessageUtil.warn(voter, "你已投票反对对 §e" + session.getTargetPlayerName() + " §c执行 " + action);
        }
        
        // 【移除跨服投票】不再发送到中控服务器
        // if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
        //     plugin.getWebSocketClient().sendVoteCast(voteId, voter, agree);
        // }
        
        // 检查是否满足结束条件
        checkVoteEnd(session);
        
        return true;
    }
    
    /**
     * 检查投票是否应该结束
     */
    private void checkVoteEnd(VoteSession session) {
        int totalVotes = session.getTotalVotes();
        int minVotes = plugin.getConfigManager().getMinTotalVotes();
        
        // 如果所有在线玩家都投票了，提前结束
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size() - 1; // 排除目标玩家
        if (totalVotes >= onlinePlayers && totalVotes >= minVotes) {
            endVote(session.getSessionId(), true);
        }
    }
    
    /**
     * 结束投票
     */
    public void endVote(String voteId, boolean checkResult) {
        VoteSession session = activeVotes.remove(voteId);
        if (session == null) return;
        
        // 取消任务
        BukkitTask task = voteTasks.remove(voteId);
        if (task != null) {
            task.cancel();
        }
        
        // 从发起者映射中移除
        playerActiveVote.remove(session.getInitiatorUuid());
        
        // 计算结果
        boolean passed = false;
        if (checkResult) {
            int yesVotes = session.getYesCount();
            int totalVotes = session.getTotalVotes();
            int minVotes = plugin.getConfigManager().getMinTotalVotes();
            double passRate = plugin.getConfigManager().getPassRate();
            
            passed = totalVotes >= minVotes && (double) yesVotes / totalVotes >= passRate;
        }
        
        // 执行操作
        if (passed) {
            executeAction(session);
        }
        
        // 广播结果（仅本服务器）
        broadcastVoteResult(session, passed);
        
        // 记录历史
        recordVoteHistory(session, passed);
        
        // 【移除跨服投票】不再发送到中控服务器
        // if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
        //     plugin.getWebSocketClient().sendVoteEnd(voteId, passed, session.getYesCount(), session.getNoCount());
        // }
    }
    
    /**
     * 执行投票通过后的操作
     * 投票在本服务器进行，但结果同步到所有服务器
     */
    private void executeAction(VoteSession session) {
        String action = session.getAction().toUpperCase();
        UUID targetUuid = session.getTargetPlayerUuid();
        String targetName = session.getTargetPlayerName();
        String reason = session.getReason();
        
        switch (action) {
            case "KICK":
                // 本地执行踢出
                plugin.getPlayerDataManager().kickPlayer(targetUuid, reason);
                MessageUtil.broadcast("§e玩家 §c" + targetName + " §e已被投票踢出！");
                // 同步到所有服务器
                broadcastActionToAllServers("KICK", targetUuid, targetName, reason, 0);
                break;
                
            case "BAN":
                // 本地执行封禁
                plugin.getPlayerDataManager().banPlayer(targetUuid, reason);
                MessageUtil.broadcast("§e玩家 §c" + targetName + " §e已被投票封禁！");
                // 同步到所有服务器
                broadcastActionToAllServers("BAN", targetUuid, targetName, reason, 30);
                break;
                
            case "MUTE":
                int muteDays = plugin.getConfigManager().getMuteDurationDays();
                long muteDuration = muteDays * 24 * 60 * 60 * 1000L;
                // 本地执行禁言
                plugin.getPlayerDataManager().mutePlayer(targetUuid, muteDuration);
                MessageUtil.broadcast("§e玩家 §c" + targetName + " §e已被投票禁言 " + muteDays + " 天！");
                // 同步到所有服务器
                broadcastActionToAllServers("MUTE", targetUuid, targetName, reason, muteDays);
                break;
                
            default:
                plugin.getLogger().warning("未知的投票操作类型: " + action);
        }
    }
    
    /**
     * 广播投票结果到所有服务器
     * 投票在本服务器进行，但结果应用到全服
     */
    private void broadcastActionToAllServers(String action, UUID targetUuid, String targetName, String reason, int durationDays) {
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
            switch (action) {
                case "KICK":
                    // 广播踢出命令到所有服务器
                    plugin.getWebSocketClient().broadcastKickToAllServers(targetUuid, targetName, reason);
                    break;
                case "BAN":
                    // 广播封禁命令到所有服务器
                    plugin.getWebSocketClient().broadcastBanToAllServers(targetUuid, targetName, reason, durationDays);
                    break;
                case "MUTE":
                    // 广播禁言命令到所有服务器
                    plugin.getWebSocketClient().broadcastMuteToAllServers(targetUuid, targetName, reason, durationDays);
                    break;
            }
            plugin.getLogger().info("投票结果已同步到所有服务器: " + action + " -> " + targetName);
        } else {
            plugin.getLogger().warning("WebSocket未连接，投票结果仅在本服务器生效");
        }
    }
    
    /**
     * 广播投票开始
     */
    private void broadcastVoteStart(VoteSession session) {
        String action = plugin.getConfigManager().getActionDisplayName(session.getAction());
        String voteId = session.getSessionId();
        
        // 发送标题
        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtil.sendTitle(player, "§6§l投票开始", "§e对 §c" + session.getTargetPlayerName() + " §e的" + action, 10, 70, 20);
        }
        
        // 广播普通消息
        String message = "\n§8§m                                    §r\n" +
            " §6§l投票发起 §7由 " + session.getInitiatorName() + "\n" +
            " §7目标: §e" + session.getTargetPlayerName() + "\n" +
            " §7操作: §c" + action + "\n" +
            " §7理由: §f" + session.getReason() + "\n" +
            " §7时长: §f" + (session.getDurationSeconds() / 60) + " 分钟\n" +
            "§8§m                                    §r";
        MessageUtil.broadcast(message);
        
        // 发送可点击的投票按钮
        int sentCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 跳过目标玩家
            if (player.getUniqueId().equals(session.getTargetPlayerUuid())) {
                continue;
            }
            
            // 检查玩家是否有投票权限
            if (!player.hasPermission("pcs.vote.participate")) {
                continue;
            }
            
            // 创建可点击的消息组件
            TextComponent base = new TextComponent(" ");
            
            // [支持] 按钮
            TextComponent yesBtn = new TextComponent("§a§l[支持]");
            yesBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pcs voteyes " + voteId));
            
            // 分隔
            TextComponent separator = new TextComponent(" §7| ");
            
            // [反对] 按钮
            TextComponent noBtn = new TextComponent("§c§l[反对]");
            noBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pcs voteno " + voteId));
            
            // 组合消息
            base.addExtra(yesBtn);
            base.addExtra(separator);
            base.addExtra(noBtn);
            
            player.spigot().sendMessage(base);
            sentCount++;
        }
        plugin.getLogger().info("[投票] 已向 " + sentCount + " 位玩家发送投票按钮");
    }
    
    /**
     * 广播投票结果
     */
    private void broadcastVoteResult(VoteSession session, boolean passed) {
        String action = plugin.getConfigManager().getActionDisplayName(session.getAction());
        String resultColor = passed ? "§a" : "§c";
        String resultText = passed ? "§a§l通过" : "§c§l未通过";
        
        String message = "\n§8§m                                    §r\n" +
            " §6§l投票结束\n" +
            " §7目标: §e" + session.getTargetPlayerName() + "\n" +
            " §7操作: " + action + "\n" +
            " §7结果: " + resultColor + resultText + "\n" +
            " §a支持: §f" + session.getYesCount() + " §7| §c反对: §f" + session.getNoCount() + "\n" +
            "§8§m                                    §r";
        
        MessageUtil.broadcast(message);
    }
    
    /**
     * 记录投票历史
     */
    private void recordVoteHistory(VoteSession session, boolean passed) {
        VoteHistory history = new VoteHistory();
        history.setVoteId(session.getSessionId());
        history.setTargetPlayerUuid(session.getTargetPlayerUuid());
        history.setTargetPlayerName(session.getTargetPlayerName());
        history.setInitiatorUuid(session.getInitiatorUuid());
        history.setInitiatorName(session.getInitiatorName());
        history.setAction(session.getAction());
        history.setReason(session.getReason());
        history.setServerId(session.getServerId());
        history.setServerName(session.getServerName());
        history.setEndTime(new Date());
        history.setPassed(passed);
        history.setYesVotes(session.getYesCount());
        history.setNoVotes(session.getNoCount());
        history.setVotes(new ArrayList<>(session.getVotes().values()));
        history.setStatus(passed ? "PASSED" : "REJECTED");
        
        plugin.getPlayerDataManager().addVoteHistory(session.getTargetPlayerUuid(), history);
    }
    
    /**
     * 启动投票计时器
     */
    private void startVoteTimer(VoteSession session) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endVote(session.getSessionId(), true);
        }, session.getDurationSeconds() * 20L);
        
        voteTasks.put(session.getSessionId(), task);
    }
    
    /**
     * 获取投票建造器
     */
    public VoteBuilder getVoteBuilder(UUID playerUuid) {
        return voteBuilders.computeIfAbsent(playerUuid, VoteBuilder::new);
    }
    
    /**
     * 清除投票建造器
     */
    public void clearVoteBuilder(UUID playerUuid) {
        voteBuilders.remove(playerUuid);
    }
    
    /**
     * 获取活跃投票
     */
    public VoteSession getActiveVote(String voteId) {
        return activeVotes.get(voteId);
    }
    
    /**
     * 获取所有活跃投票
     */
    public Collection<VoteSession> getAllActiveVotes() {
        return Collections.unmodifiableCollection(activeVotes.values());
    }

    /**
     * 获取活跃投票ID列表
     */
    public List<String> getActiveVoteIds() {
        return new ArrayList<>(activeVotes.keySet());
    }
    
    /**
     * 检查玩家是否有进行中的投票
     */
    public boolean hasActiveVote(UUID playerUuid) {
        return playerActiveVote.containsKey(playerUuid);
    }
    
    /**
     * 投票建造器类
     */
    public static class VoteBuilder {
        private String action;
        private UUID targetUuid;
        private String targetName;
        private String reason;
        private int currentStep; // 0: 选择操作, 1: 选择玩家, 2: 选择理由
        
        public VoteBuilder(UUID initiatorUuid) {
            this.currentStep = 0;
        }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public UUID getTargetUuid() { return targetUuid; }
        public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
        
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public int getCurrentStep() { return currentStep; }
        public void setCurrentStep(int step) { this.currentStep = step; }
        
        public boolean isComplete() {
            return action != null && targetUuid != null && reason != null;
        }
    }
}
