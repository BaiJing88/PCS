package com.pcs.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pcs.fabric.PCSFabricMod;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;

/**
 * Fabric 版命令处理器
 * 与 Spigot 插件命令体验一致
 */
public class PCSCommandFabric {
    
    private final PCSFabricMod mod;
    
    public PCSCommandFabric(PCSFabricMod mod) {
        this.mod = mod;
    }
    
    /**
     * 注册所有命令
     */
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /pcs 主命令
        dispatcher.register(literal("pcs")
            .requires(source -> source.hasPermissionLevel(0))
            .executes(this::showHelp)
            
            // /pcs help
            .then(literal("help")
                .executes(this::showHelp))
            
            // /pcs vote - 打开投票GUI
            .then(literal("vote")
                .executes(this::openVoteGUI))
            
            // /pcs rate <玩家> <分数> - 评分
            .then(literal("rate")
                .then(argument("player", EntityArgumentType.player())
                    .then(argument("score", IntegerArgumentType.integer(1, 10))
                        .executes(this::ratePlayer))))
            
            // /pcs credit [玩家] - 查看信用分
            .then(literal("credit")
                .executes(this::checkOwnCredit)
                .then(argument("player", EntityArgumentType.player())
                    .executes(this::checkPlayerCredit)))
            
            // /pcs history [玩家] - 查看历史
            .then(literal("history")
                .executes(this::checkOwnHistory)
                .then(argument("player", EntityArgumentType.player())
                    .executes(this::checkPlayerHistory)))
            
            // /pcs banlist - 查看封禁列表
            .then(literal("banlist")
                .executes(this::showBanlist))
            
            // /pcs gui - 打开主GUI
            .then(literal("gui")
                .executes(this::openMainGUI))
        );
        
        // /pcsadmin 管理命令
        dispatcher.register(literal("pcsadmin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(literal("reload")
                .executes(this::reloadConfig))
            .then(literal("debug")
                .executes(this::toggleDebug))
        );
        
        mod.getLogger().info("PCS 命令已注册");
    }
    
    // ==================== 命令实现 ====================
    
    private int showHelp(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("""
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
    
    private int openVoteGUI(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 投票功能开发中，请使用 /pcs vote <玩家>"), false);
        return 1;
    }
    
    private int ratePlayer(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        int score = IntegerArgumentType.getInteger(ctx, "score");
        ServerPlayerEntity rater = ctx.getSource().getPlayer();
        
        if (rater == null) {
            ctx.getSource().sendError(net.minecraft.text.Text.literal("§c只有玩家才能使用此命令"));
            return 0;
        }
        
        if (rater.getUuid().equals(target.getUuid())) {
            ctx.getSource().sendError(net.minecraft.text.Text.literal("§c不能给自己评分！"));
            return 0;
        }
        
        // 发送到中控
        if (mod.getWebSocketClient() != null && mod.getWebSocketClient().isAuthenticated()) {
            mod.getWebSocketClient().sendRating(
                rater.getUuid(),
                rater.getName().getString(),
                target.getUuid(),
                target.getName().getString(),
                score,
                ""
            );
            ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§a你给了 " + target.getName().getString() + " " + score + " 分！"), false);
        } else {
            ctx.getSource().sendError(net.minecraft.text.Text.literal("§c中控服务器未连接，无法提交评分！"));
        }
        
        return 1;
    }
    
    private int checkOwnCredit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        
        // TODO: 从中控获取信用分
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 你的信用分查询功能开发中"), false);
        return 1;
    }
    
    private int checkPlayerCredit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        
        // TODO: 从中控获取信用分
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 玩家 " + target.getName().getString() + " 的信用分查询功能开发中"), false);
        return 1;
    }
    
    private int checkOwnHistory(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 历史记录查询功能开发中"), false);
        return 1;
    }
    
    private int checkPlayerHistory(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 玩家历史记录查询功能开发中"), false);
        return 1;
    }
    
    private int showBanlist(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 封禁列表查询功能开发中"), false);
        return 1;
    }
    
    private int openMainGUI(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e[PCS] 主界面开发中"), false);
        return 1;
    }
    
    // ==================== 管理命令 ====================
    
    private int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        mod.reloadConfig();
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§a配置已重载"), false);
        return 1;
    }
    
    private int toggleDebug(CommandContext<ServerCommandSource> ctx) {
        boolean debug = !mod.isDebug();
        mod.setDebug(debug);
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§e调试模式: " + (debug ? "开启" : "关闭")), false);
        return 1;
    }
}
