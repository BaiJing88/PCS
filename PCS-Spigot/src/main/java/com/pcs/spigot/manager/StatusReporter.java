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

import com.pcs.api.protocol.PacketType;
import com.pcs.api.protocol.ProtocolPacket;
import com.pcs.api.protocol.StatusPacket;
import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 服务器状态报告器
 * 定期收集和上报服务器性能数据
 */
public class StatusReporter {

    private final PCSSpigotPlugin plugin;
    private final ScheduledExecutorService executor;

    // TPS 计算相关
    private long lastTickTime;
    private long tickCount;
    private final double[] tpsHistory = new double[600]; // 10分钟历史
    private int tpsIndex = 0;

    // 启动时间
    private final long startTime;

    public StatusReporter(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = System.currentTimeMillis();

        // 启动 TPS 监控
        startTpsMonitor();
    }

    /**
     * 启动定时上报
     */
    public void start() {
        int interval = plugin.getConfigManager().getConfig().getInt("status-report.interval", 30);

        executor.scheduleAtFixedRate(() -> {
            if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isAuthenticated()) {
                sendStatusReport();
            }
        }, interval, interval, TimeUnit.SECONDS);

        plugin.getLogger().info("状态上报已启动，间隔: " + interval + "秒");
    }

    /**
     * 停止上报
     */
    public void stop() {
        executor.shutdownNow();
    }

    /**
     * 启动 TPS 监控
     */
    private void startTpsMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long diff = now - lastTickTime;

            if (diff > 0) {
                double tps = Math.min(20.0, 1000.0 / (diff / 20.0));
                tpsHistory[tpsIndex] = tps;
                tpsIndex = (tpsIndex + 1) % tpsHistory.length;
            }

            lastTickTime = now;
            tickCount++;
        }, 1L, 1L);
    }

    /**
     * 发送状态报告
     */
    private void sendStatusReport() {
        try {
            StatusPacket.StatusReport report = collectStatus();

            ProtocolPacket packet = ProtocolPacket.request(PacketType.STATUS_REPORT, report);
            plugin.getWebSocketClient().send(packet.toJson());

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "发送状态报告失败", e);
        }
    }

    /**
     * 收集服务器状态
     */
    public StatusPacket.StatusReport collectStatus() {
        StatusPacket.StatusReport report = new StatusPacket.StatusReport();
        report.setServerId(plugin.getConfigManager().getServerId());
        report.setTimestamp(System.currentTimeMillis());

        // TPS
        report.setTps(getCurrentTps());
        report.setTps1m(getTpsAverage(1200)); // 1分钟 = 1200 ticks
        report.setTps5m(getTpsAverage(6000)); // 5分钟
        report.setTps15m(getTpsAverage(18000)); // 15分钟

        // 内存
        Runtime runtime = Runtime.getRuntime();
        report.setMemoryMax(runtime.maxMemory() / 1024 / 1024);
        report.setMemoryUsed((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        report.setMemoryFree(runtime.freeMemory() / 1024 / 1024);

        // CPU
        report.setCpuUsage(getCpuUsage());

        // 玩家
        report.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        report.setMaxPlayers(Bukkit.getMaxPlayers());

        // 世界信息
        List<StatusPacket.WorldInfo> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            StatusPacket.WorldInfo info = new StatusPacket.WorldInfo();
            info.setName(world.getName());
            info.setLoadedChunks(world.getLoadedChunks().length);
            info.setEntities(world.getEntities().size());
            info.setPlayers(world.getPlayers().size());
            info.setDifficulty(world.getDifficulty().name());
            worlds.add(info);
        }
        report.setWorlds(worlds);

        // 运行时间
        report.setUptimeMinutes((System.currentTimeMillis() - startTime) / 1000 / 60);

        return report;
    }

    /**
     * 获取当前 TPS
     */
    private double getCurrentTps() {
        int index = (tpsIndex - 1 + tpsHistory.length) % tpsHistory.length;
        return tpsHistory[index] > 0 ? tpsHistory[index] : 20.0;
    }

    /**
     * 获取 TPS 平均值
     */
    private double getTpsAverage(int ticks) {
        if (tickCount < ticks) {
            ticks = (int) tickCount;
        }
        if (ticks == 0) return 20.0;

        double sum = 0;
        int count = 0;

        for (int i = 0; i < ticks && i < tpsHistory.length; i++) {
            int index = (tpsIndex - 1 - i + tpsHistory.length) % tpsHistory.length;
            if (tpsHistory[index] > 0) {
                sum += tpsHistory[index];
                count++;
            }
        }

        return count > 0 ? sum / count : 20.0;
    }

    /**
     * 获取 CPU 使用率
     */
    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getSystemLoadAverage();
            if (load >= 0) {
                return Math.min(100.0, load * 100.0 / osBean.getAvailableProcessors());
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return 0.0;
    }
}
