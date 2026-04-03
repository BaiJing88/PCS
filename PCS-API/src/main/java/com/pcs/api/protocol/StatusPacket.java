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

package com.pcs.api.protocol;

import java.util.List;
import java.util.Map;

/**
 * 服务器状态报告数据包
 */
public class StatusPacket {

    /**
     * 服务器状态报告请求
     */
    public static class StatusReport {
        private String serverId;
        private long timestamp;

        // TPS 信息
        private double tps;
        private double tps1m;
        private double tps5m;
        private double tps15m;

        // 内存信息 (MB)
        private long memoryUsed;
        private long memoryMax;
        private long memoryFree;

        // CPU 使用率 (%)
        private double cpuUsage;

        // 在线玩家
        private int onlinePlayers;
        private int maxPlayers;

        // 世界信息
        private List<WorldInfo> worlds;

        // 服务器运行时间 (分钟)
        private long uptimeMinutes;

        // Getters and Setters
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public double getTps() { return tps; }
        public void setTps(double tps) { this.tps = tps; }

        public double getTps1m() { return tps1m; }
        public void setTps1m(double tps1m) { this.tps1m = tps1m; }

        public double getTps5m() { return tps5m; }
        public void setTps5m(double tps5m) { this.tps5m = tps5m; }

        public double getTps15m() { return tps15m; }
        public void setTps15m(double tps15m) { this.tps15m = tps15m; }

        public long getMemoryUsed() { return memoryUsed; }
        public void setMemoryUsed(long memoryUsed) { this.memoryUsed = memoryUsed; }

        public long getMemoryMax() { return memoryMax; }
        public void setMemoryMax(long memoryMax) { this.memoryMax = memoryMax; }

        public long getMemoryFree() { return memoryFree; }
        public void setMemoryFree(long memoryFree) { this.memoryFree = memoryFree; }

        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

        public int getOnlinePlayers() { return onlinePlayers; }
        public void setOnlinePlayers(int onlinePlayers) { this.onlinePlayers = onlinePlayers; }

        public int getMaxPlayers() { return maxPlayers; }
        public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

        public List<WorldInfo> getWorlds() { return worlds; }
        public void setWorlds(List<WorldInfo> worlds) { this.worlds = worlds; }

        public long getUptimeMinutes() { return uptimeMinutes; }
        public void setUptimeMinutes(long uptimeMinutes) { this.uptimeMinutes = uptimeMinutes; }
    }

    /**
     * 世界信息
     */
    public static class WorldInfo {
        private String name;
        private int loadedChunks;
        private int entities;
        private int players;
        private String difficulty;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getLoadedChunks() { return loadedChunks; }
        public void setLoadedChunks(int loadedChunks) { this.loadedChunks = loadedChunks; }

        public int getEntities() { return entities; }
        public void setEntities(int entities) { this.entities = entities; }

        public int getPlayers() { return players; }
        public void setPlayers(int players) { this.players = players; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    }

    /**
     * 状态报告响应
     */
    public static class StatusResponse {
        private boolean success;
        private String message;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
