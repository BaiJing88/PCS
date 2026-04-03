package com.pcs.neoforge.command;

import com.pcs.neoforge.PCSNeoForgeMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.commands.PcsCommand;
import net.minecraft.server.commands.VoteCommands;
import net.minecraft.server.commands.RateCommands;
import net.minecraft.server.commands.CreditCommands;
import net.minecraft.server.commands.HistoryCommands;
import net.minecraft.server.commands.BanlistCommands;
import net.minecraft.server.commands.GUICommands;

/**
 * NeoForge 版命令处理器
 * 与 Spigot 插件命令体验一致
 */
public class PCSCommandNeoForge {
    
    private final PCSNeoForgeMod mod;
    
    public PCSCommandNeoForge(PCSNeoForgeMod mod) {
        this.mod = mod;
    }
    
    /**
     * 注册所有命令
     */
    public void register(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        // /pcs 主命令
        dispatcher.register(Commands.literal("pcs")
            .requires(source -> source.hasPermission(0))
            .executes(this::showHelp)
            
            .then(Commands.literal("help").executes(this::showHelp))
            .then(Commands.literal("vote").executes(this::openVoteGUI))
            
            .then(Commands.literal("rate")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("score", IntegerArgumentType.integer(1, 10))
                        .executes(this::ratePlayer))))
            
            .then(Commands.literal("credit")
                .executes(this::checkOwnCredit)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(this::checkPlayerCredit)))
            
            .then(Commands.literal("history")
                .executes(this::checkOwnHistory)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(this::checkPlayerHistory)))
            
            .then(Commands.literal("banlist").executes(this::showBanlist))
            .then(Commands.literal("gui").executes(this::openMainGUI))
        );
        
        // /pcsadmin 管理命令
        dispatcher.register(Commands.literal("pcsadmin")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("reload").executes(this::reloadConfig))
            .then(Commands.literal("debug").executes(this::toggleDebug))
        );
        
        mod.getLogger().info("PCS 命令已注册");
    }
    
    // ==================== 命令实现 ====================
    
    private int showHelp(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("""
            §6=== PCS 玩家信用系统 ===
            §e/pcs §7- 显示帮助
            §e/pcs vote §7- 发起投票
            §e/pcs rate <玩家> <1-10> §7- 给玩家评分
            §e/pcs credit [玩家] §7- 查看信用分
            §e/pcs history [玩家] §7- 查看历史记录
            §e/pcs banlist §7- 查看封禁列表
            §e/pcs gui §7- 打开主界面
            """), false);
        return 1;
    }
    
    private int openVoteGUI(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 投票功能开发中，请使用 /pcs vote <玩家>"), false);
        return 1;
    }
    
    private int ratePlayer(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        var target = EntityArgument.getPlayer(ctx, "player");
        int score = IntegerArgumentType.getInteger(ctx, "score");
        var rater = ctx.getSource().getPlayerOrException();
        
        if (rater.getUUID().equals(target.getUUID())) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§c不能给自己评分！"));
            return 0;
        }
        
        if (mod.getWebSocketClient() != null && mod.getWebSocketClient().isAuthenticated()) {
            mod.getWebSocketClient().sendRating(
                rater.getUUID(),
                rater.getName().getString(),
                target.getUUID(),
                target.getName().getString(),
                score,
                ""
            );
            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a你给了 " + target.getName().getString() + " " + score + " 分！"), false);
        } else {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§c中控服务器未连接，无法提交评分！"));
        }
        
        return 1;
    }
    
    private int checkOwnCredit(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        var player = ctx.getSource().getPlayerOrException();
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 你的信用分查询功能开发中"), false);
        return 1;
    }
    
    private int checkPlayerCredit(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        var target = EntityArgument.getPlayer(ctx, "player");
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 玩家 " + target.getName().getString() + " 的信用分查询功能开发中"), false);
        return 1;
    }
    
    private int checkOwnHistory(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 历史记录查询功能开发中"), false);
        return 1;
    }
    
    private int checkPlayerHistory(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        var target = EntityArgument.getPlayer(ctx, "player");
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 玩家历史记录查询功能开发中"), false);
        return 1;
    }
    
    private int showBanlist(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 封禁列表查询功能开发中"), false);
        return 1;
    }
    
    private int openMainGUI(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e[PCS] 主界面开发中"), false);
        return 1;
    }
    
    // ==================== 管理命令 ====================
    
    private int reloadConfig(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        mod.reloadConfig();
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a配置已重载"), false);
        return 1;
    }
    
    private int toggleDebug(CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        boolean debug = !mod.isDebug();
        mod.setDebug(debug);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e调试模式: " + (debug ? "开启" : "关闭")), false);
        return 1;
    }
}
