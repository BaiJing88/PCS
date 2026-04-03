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

package com.pcs.spigot.command;

import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.manager.VoteManager;
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PCS主命令处理器
 * /pcs 命令实现
 */
public class PCSCommand implements CommandExecutor, TabCompleter {
    
    private final PCSSpigotPlugin plugin;
    
    // 可用操作
    private static final List<String> ACTIONS = Arrays.asList("kick", "ban", "mute");
    
    public PCSCommand(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("pcs.use")) {
            MessageUtil.error(sender, "你没有权限使用此命令！");
            return true;
        }
        
        // 必须是玩家才能使用GUI
        if (!(sender instanceof Player)) {
            // 控制台可以使用子命令
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                return handleReload(sender);
            }
            MessageUtil.error(sender, "此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 无参数 - 打开选择操作界面
        if (args.length == 0) {
            if (!plugin.getConfigManager().isVoteSystemEnabled()) {
                MessageUtil.error(player, "投票系统当前已禁用！");
                return true;
            }
            plugin.getGuiManager().openVoteActionGUI(player);
            return true;
        }
        
        // 处理子命令
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "help":
                return handleHelp(sender);
            case "status":
                return handleStatus(sender);
            case "credit":
            case "query":
                return handleCreditQuery(player, args);
            case "rate":
                return handleRate(player, args);
            case "voteyes":
                return handleVoteYes(player, args);
            case "voteno":
                return handleVoteNo(player, args);
            case "admin":
                return handleAdmin(sender);
            default:
                // 可能是操作类型
                if (ACTIONS.contains(subCommand)) {
                    return handleVoteCommand(player, args);
                }
                MessageUtil.error(sender, "未知命令！使用 /pcs help 查看帮助。");
                return true;
        }
    }
    
    /**
     * 处理投票相关命令
     * /pcs [action] [player] [reason]
     */
    private boolean handleVoteCommand(Player player, String[] args) {
        if (!plugin.getConfigManager().isVoteSystemEnabled()) {
            MessageUtil.error(player, "投票系统当前已禁用！");
            return true;
        }
        
        String action = args[0].toUpperCase();
        
        // 只提供了操作，打开玩家选择界面
        if (args.length == 1) {
            VoteManager.VoteBuilder builder = plugin.getVoteManager().getVoteBuilder(player.getUniqueId());
            builder.setAction(action);
            builder.setCurrentStep(1);
            plugin.getGuiManager().openPlayerSelectGUI(player, action, 0);
            return true;
        }
        
        // 提供了操作和目标玩家
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        
        if (target == null || !target.isOnline()) {
            MessageUtil.error(player, "目标玩家不在线！");
            return true;
        }
        
        if (target.equals(player)) {
            MessageUtil.error(player, "不能对自己发起投票！");
            return true;
        }
        
        // 提供了操作、目标玩家和理由
        if (args.length >= 3) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) reasonBuilder.append(" ");
                reasonBuilder.append(args[i]);
            }
            String reason = reasonBuilder.toString();
            
            // 直接发起投票
            boolean success = plugin.getVoteManager().startVote(player, target, action, reason);
            if (success) {
                MessageUtil.success(player, "投票发起成功！");
            }
            return true;
        }
        
        // 只提供了操作和目标玩家，打开理由选择界面
        VoteManager.VoteBuilder builder = plugin.getVoteManager().getVoteBuilder(player.getUniqueId());
        builder.setAction(action);
        builder.setTargetUuid(target.getUniqueId());
        builder.setTargetName(target.getName());
        builder.setCurrentStep(2);
        plugin.getGuiManager().openReasonSelectGUI(player, action, target.getName());
        
        return true;
    }
    
    /**
     * 处理信用查询命令
     */
    private boolean handleCreditQuery(Player player, String[] args) {
        if (!plugin.getConfigManager().isCreditQueryEnabled()) {
            MessageUtil.error(player, "信用查询系统当前已禁用！");
            return true;
        }
        
        String targetName;
        if (args.length >= 2) {
            targetName = args[1];
        } else {
            targetName = player.getName();
        }
        
        plugin.getGuiManager().openCreditQueryGUI(player, targetName);
        return true;
    }
    
    /**
     * 处理评分命令
     */
    private boolean handleRate(Player player, String[] args) {
        if (!plugin.getConfigManager().isRatingSystemEnabled()) {
            MessageUtil.error(player, "评分系统当前已禁用！");
            return true;
        }
        
        if (args.length < 2) {
            // 打开评分玩家选择界面
            plugin.getGuiManager().openPlayerSelectGUI(player, "RATE");
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        
        if (target == null) {
            MessageUtil.error(player, "目标玩家不在线！");
            return true;
        }
        
        if (target.equals(player)) {
            MessageUtil.error(player, "不能给自己评分！");
            return true;
        }
        
        // 如果没有指定分数，打开评分GUI
        if (args.length < 3) {
            plugin.getGuiManager().openRatingGUI(player, target.getUniqueId(), target.getName());
            return true;
        }
        
        // 直接提交评分
        try {
            int score = Integer.parseInt(args[2]);
            if (score < 1 || score > 10) {
                MessageUtil.error(player, "分数必须在 1-10 之间！");
                return true;
            }
            
            String comment = args.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "";
            
            // 提交评分
            if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isAuthenticated()) {
                plugin.getWebSocketClient().sendRating(
                    player.getUniqueId(),
                    player.getName(),
                    target.getUniqueId(),
                    target.getName(),
                    score,
                    comment
                );
                
                plugin.getPlayerDataManager().recordRating(player, target.getUniqueId());
                MessageUtil.success(player, "你给 " + targetName + " 打了 " + score + " 分！");
            } else {
                MessageUtil.error(player, "中控服务器未连接，无法提交评分！");
            }
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "分数必须是数字！");
        }
        
        return true;
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("pcs.admin")) {
            MessageUtil.error(sender, "你没有权限执行此命令！");
            return true;
        }
        
        plugin.reloadConfig();
        MessageUtil.success(sender, "配置已重载！");
        
        // 重新连接中控服务器
        MessageUtil.info(sender, "正在重新连接中控服务器...");
        plugin.reconnectToController();
        
        return true;
    }
    
    /**
     * 处理状态命令
     */
    private boolean handleStatus(CommandSender sender) {
        MessageUtil.send(sender, "§6§lPCS-Spigot 状态");
        MessageUtil.send(sender, "§7版本: §f" + plugin.getDescription().getVersion());
        MessageUtil.send(sender, "§7服务器ID: §f" + plugin.getConfigManager().getServerId());
        MessageUtil.send(sender, "§7服务器名称: §f" + plugin.getConfigManager().getServerName());
        
        boolean connected = plugin.getWebSocketClient() != null && 
                           plugin.getWebSocketClient().isConnected();
        MessageUtil.send(sender, "§7中控连接: " + (connected ? "§a已连接" : "§c未连接"));
        
        MessageUtil.send(sender, "§7在线玩家: §f" + Bukkit.getOnlinePlayers().size());
        MessageUtil.send(sender, "§7活跃投票: §f" + plugin.getVoteManager().getAllActiveVotes().size());
        
        return true;
    }
    
    /**
     * 处理投票支持
     */
    private boolean handleVoteYes(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.error(player, "用法: /pcs voteyes <投票ID>");
            return true;
        }
        
        String voteId = args[1];
        boolean success = plugin.getVoteManager().castVote(player, voteId, true);
        return true;
    }
    
    /**
     * 处理投票反对
     */
    private boolean handleVoteNo(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.error(player, "用法: /pcs voteno <投票ID>");
            return true;
        }
        
        String voteId = args[1];
        boolean success = plugin.getVoteManager().castVote(player, voteId, false);
        return true;
    }

    /**
     * 处理管理员命令
     */
    private boolean handleAdmin(CommandSender sender) {
        if (!sender.hasPermission("pcs.admin")) {
            MessageUtil.error(sender, "你没有权限使用管理员命令！");
            return true;
        }

        MessageUtil.send(sender, "§6§lPCS 管理员命令");
        MessageUtil.send(sender, "§7/pcs reload §f- 重载配置文件");
        MessageUtil.send(sender, "§7/pcs admin §f- 显示本帮助");
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "§e提示：更多管理功能请访问Web控制面板");
        return true;
    }

    /**
     * 处理帮助命令
     */
    private boolean handleHelp(CommandSender sender) {
        MessageUtil.send(sender, "§6§lPCS-Spigot 帮助");
        MessageUtil.send(sender, "§7/pcs §f- 打开投票界面");
        MessageUtil.send(sender, "§7/pcs <kick|ban|mute> §f- 选择操作类型");
        MessageUtil.send(sender, "§7/pcs <kick|ban|mute> <玩家> §f- 选择目标玩家");
        MessageUtil.send(sender, "§7/pcs <kick|ban|mute> <玩家> <理由> §f- 直接发起投票");
        MessageUtil.send(sender, "§7/pcs credit [玩家] §f- 查询玩家信用");
        MessageUtil.send(sender, "§7/pcs status §f- 查看系统状态");
        
        if (sender.hasPermission("pcs.admin")) {
            MessageUtil.send(sender, "§7/pcs reload §f- 重载配置");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("pcs.use")) {
            return completions;
        }
        
        if (args.length == 1) {
            // 第一级补全
            completions.addAll(ACTIONS);
            completions.addAll(Arrays.asList("help", "status", "credit", "reload", "rate", "voteyes", "voteno"));
            
            if (!sender.hasPermission("pcs.admin")) {
                completions.remove("reload");
            }
            
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (ACTIONS.contains(subCommand) || subCommand.equals("credit") || subCommand.equals("rate")) {
                // 补全在线玩家名（用于ban/kick/mute/credit/rate）
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (subCommand.equals("voteyes") || subCommand.equals("voteno")) {
                // 补全投票ID（可选）
                List<String> activeVoteIds = plugin.getVoteManager().getActiveVoteIds();
                return activeVoteIds.stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (ACTIONS.contains(subCommand)) {
                // 补全理由（用于ban/kick/mute）
                return plugin.getConfigManager().getAvailableReasons().stream()
                    .filter(reason -> reason.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (subCommand.equals("rate")) {
                // 补全分数 1-10
                return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
