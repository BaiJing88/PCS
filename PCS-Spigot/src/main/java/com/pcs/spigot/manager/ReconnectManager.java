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

import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * 重连管理器
 * 使用指数退避策略进行重连
 */
public class ReconnectManager {

    private final PCSSpigotPlugin plugin;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    // 重连间隔配置
    private static final int INITIAL_DELAY_MS = 1000;      // 初始延迟 1秒
    private static final int MAX_DELAY_MS = 60000;         // 最大延迟 60秒
    private static final double BACKOFF_MULTIPLIER = 2.0;  // 退避乘数

    // 连接质量监控
    private long lastSuccessfulConnection = 0;
    private long totalUptime = 0;
    private int successfulReconnects = 0;
    private int failedReconnects = 0;

    public ReconnectManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取下一次重连延迟（毫秒）
     */
    public int getNextReconnectDelay() {
        int attempts = reconnectAttempts.get();
        if (attempts == 0) {
            return INITIAL_DELAY_MS;
        }

        // 指数退避: delay = initial * (multiplier ^ attempts)
        long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempts));
        return (int) Math.min(delay, MAX_DELAY_MS);
    }

    /**
     * 增加重连尝试次数
     */
    public void incrementAttempts() {
        reconnectAttempts.incrementAndGet();
    }

    /**
     * 重置重连尝试次数
     */
    public void resetAttempts() {
        int attempts = reconnectAttempts.getAndSet(0);
        if (attempts > 0) {
            successfulReconnects++;
            lastSuccessfulConnection = System.currentTimeMillis();
            plugin.getLogger().info("重连成功，已重置重连计数器");
        }
    }

    /**
     * 记录失败重连
     */
    public void recordFailedReconnect() {
        failedReconnects++;
        incrementAttempts();
    }

    /**
     * 安排重连
     */
    public void scheduleReconnect(Runnable reconnectTask) {
        int delayTicks = getNextReconnectDelay() / 50; // 转换为 ticks

        plugin.getLogger().info("将在 " + (delayTicks / 20.0) + " 秒后尝试重连..." +
            " (第 " + (reconnectAttempts.get() + 1) + " 次尝试)");

        // 如果重连次数较多，通知在线OP
        if (reconnectAttempts.get() >= 3) {
            notifyOperators("§c[PCS] 中控连接已断开，正在尝试重连... (第 " +
                (reconnectAttempts.get() + 1) + " 次尝试)");
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, reconnectTask, delayTicks);
    }

    /**
     * 获取连接状态摘要
     */
    public String getStatusSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("连接状态:\n");
        sb.append("  当前重连尝试: ").append(reconnectAttempts.get()).append("\n");
        sb.append("  成功重连次数: ").append(successfulReconnects).append("\n");
        sb.append("  失败重连次数: ").append(failedReconnects).append("\n");

        if (lastSuccessfulConnection > 0) {
            long uptime = System.currentTimeMillis() - lastSuccessfulConnection;
            sb.append("  本次连接时长: ").append(formatDuration(uptime)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 通知在线OP
     */
    private void notifyOperators(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("pcs.admin") || player.isOp())
                .forEach(player -> player.sendMessage(message));
        });
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d天 %02d:%02d:%02d", days, hours % 24, minutes % 60, seconds % 60);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        }
    }

    /**
     * 获取连接质量评分 (0-100)
     */
    public int getConnectionQuality() {
        if (successfulReconnects + failedReconnects == 0) {
            return 100; // 没有重连记录，假设连接良好
        }

        int total = successfulReconnects + failedReconnects;
        int successRate = (successfulReconnects * 100) / total;

        // 如果有当前重连尝试，降低评分
        if (reconnectAttempts.get() > 0) {
            successRate = successRate * (10 - Math.min(reconnectAttempts.get(), 9)) / 10;
        }

        return Math.max(0, successRate);
    }
}
