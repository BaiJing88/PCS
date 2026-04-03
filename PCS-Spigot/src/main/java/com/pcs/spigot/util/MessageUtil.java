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

package com.pcs.spigot.util;

import com.pcs.spigot.PCSSpigotPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 消息工具类
 * 提供统一的消息发送和格式化功能
 */
public class MessageUtil {
    
    private static final String PREFIX = "§8[§6PCS§8] §r";
    
    /**
     * 格式化颜色代码
     */
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 发送带前缀的消息
     */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + color(message));
    }
    
    /**
     * 发送成功消息
     */
    public static void success(CommandSender sender, String message) {
        send(sender, "§a" + message);
    }
    
    /**
     * 发送错误消息
     */
    public static void error(CommandSender sender, String message) {
        send(sender, "§c" + message);
    }
    
    /**
     * 发送警告消息
     */
    public static void warn(CommandSender sender, String message) {
        send(sender, "§e" + message);
    }
    
    /**
     * 发送信息消息
     */
    public static void info(CommandSender sender, String message) {
        send(sender, "§7" + message);
    }
    
    /**
     * 广播消息给所有在线玩家
     */
    public static void broadcast(String message) {
        Bukkit.broadcastMessage(PREFIX + color(message));
    }
    
    /**
     * 发送标题给玩家
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
    }
    
    /**
     * 发送ActionBar消息
     */
    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(color(message)));
    }
    
    /**
     * 发送标题给所有在线玩家
     */
    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String coloredTitle = color(title);
        String coloredSubtitle = color(subtitle);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
        }
    }
    
    /**
     * 发送多行消息
     */
    public static void sendLines(CommandSender sender, List<String> lines) {
        for (String line : lines) {
            sender.sendMessage(color(line));
        }
    }
    
    /**
     * 创建进度条
     * 
     * @param current 当前值
     * @param max 最大值
     * @param length 进度条长度
     * @param filled 填充字符
     * @param empty 空字符
     * @return 进度条字符串
     */
    public static String createProgressBar(double current, double max, int length, String filled, String empty) {
        if (max <= 0) return repeat(empty, length);
        
        int filledLength = (int) Math.round((current / max) * length);
        filledLength = Math.max(0, Math.min(length, filledLength));
        
        return repeat(filled, filledLength) + repeat(empty, length - filledLength);
    }
    
    /**
     * 创建带颜色的信用分数进度条 (10分制)
     */
    public static String createCreditBar(double score) {
        String color;
        if (score >= 8) {
            color = "§a";
        } else if (score >= 5) {
            color = "§e";
        } else if (score >= 3) {
            color = "§6";
        } else {
            color = "§c";
        }
        
        int filled = (int) Math.round(score);
        return color + "█" + repeat("█", filled) + "§7" + repeat("░", 10 - filled) + " §f" + String.format("%.1f", score) + "/10";
    }
    
    /**
     * 重复字符串
     */
    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 中心对齐文本
     */
    public static String center(String text, int length) {
        int padding = (length - text.length()) / 2;
        return repeat(" ", padding) + text + repeat(" ", length - text.length() - padding);
    }
    
    /**
     * 获取配置中的前缀
     */
    public static String getPrefix() {
        PCSSpigotPlugin plugin = PCSSpigotPlugin.getInstance();
        if (plugin != null && plugin.getConfig() != null) {
            return color(plugin.getConfig().getString("messages.prefix", PREFIX));
        }
        return PREFIX;
    }
}
