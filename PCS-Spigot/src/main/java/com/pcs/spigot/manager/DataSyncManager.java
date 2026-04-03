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

import com.pcs.api.model.PlayerCredit;
import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * 数据同步管理器
 * 管理批量数据同步、本地缓存和离线队列
 */
public class DataSyncManager {

    private final PCSSpigotPlugin plugin;

    // 本地缓存
    private final Map<UUID, PlayerCredit> creditCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheUpdateTime = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 缓存有效期 5分钟

    // 离线数据队列
    private final BlockingQueue<QueuedData> offlineQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService syncExecutor;

    // 批量同步配置
    private static final int BATCH_SIZE = 50;
    private static final int SYNC_INTERVAL_MS = 30000; // 30秒

    public DataSyncManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.syncExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 启动同步服务
     */
    public void start() {
        // 启动批量同步定时任务
        syncExecutor.scheduleAtFixedRate(this::processBatchSync,
            SYNC_INTERVAL_MS, SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);

        plugin.getLogger().info("数据同步管理器已启动");
    }

    /**
     * 停止同步服务
     */
    public void stop() {
        syncExecutor.shutdownNow();
        // 保存未发送的数据
        saveOfflineQueue();
    }

    /**
     * 获取玩家信用分（优先从缓存）
     */
    public PlayerCredit getPlayerCredit(UUID uuid) {
        // 检查缓存
        PlayerCredit cached = creditCache.get(uuid);
        if (cached != null && isCacheValid(uuid)) {
            return cached;
        }

        // 请求最新数据
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isAuthenticated()) {
            plugin.getWebSocketClient().requestPlayerData(uuid);
        }

        // 返回缓存数据（即使过期）或 null
        return cached;
    }

    /**
     * 更新缓存
     */
    public void updateCache(PlayerCredit credit) {
        if (credit != null && credit.getPlayerUuid() != null) {
            UUID uuid = credit.getPlayerUuid();
            creditCache.put(uuid, credit);
            cacheUpdateTime.put(uuid, System.currentTimeMillis());
        }
    }

    /**
     * 批量请求玩家数据
     */
    public void requestBatchPlayerData(Collection<UUID> uuids) {
        if (uuids.isEmpty()) return;

        // 过滤掉缓存有效的玩家
        List<UUID> needRequest = new ArrayList<>();
        for (UUID uuid : uuids) {
            if (!creditCache.containsKey(uuid) || !isCacheValid(uuid)) {
                needRequest.add(uuid);
            }
        }

        // 批量发送请求
        if (!needRequest.isEmpty() && plugin.getWebSocketClient() != null
            && plugin.getWebSocketClient().isAuthenticated()) {

            // 每BATCH_SIZE个一批
            for (int i = 0; i < needRequest.size(); i += BATCH_SIZE) {
                List<UUID> batch = needRequest.subList(i, Math.min(i + BATCH_SIZE, needRequest.size()));
                // TODO: 实现批量请求协议
                for (UUID uuid : batch) {
                    plugin.getWebSocketClient().requestPlayerData(uuid);
                }
            }
        }
    }

    /**
     * 添加到离线队列
     */
    public void queueData(String type, Object data) {
        QueuedData queued = new QueuedData(type, data, System.currentTimeMillis());
        offlineQueue.offer(queued);

        // 如果队列太大，移除旧数据
        if (offlineQueue.size() > 1000) {
            offlineQueue.poll();
        }
    }

    /**
     * 处理批量同步
     */
    private void processBatchSync() {
        if (plugin.getWebSocketClient() == null || !plugin.getWebSocketClient().isAuthenticated()) {
            return;
        }

        // 发送离线队列中的数据
        List<QueuedData> batch = new ArrayList<>();
        offlineQueue.drainTo(batch, 100);

        for (QueuedData data : batch) {
            try {
                sendQueuedData(data);
            } catch (Exception e) {
                // 发送失败，重新入队
                offlineQueue.offer(data);
                break; // 网络可能有问题，停止本次处理
            }
        }
    }

    /**
     * 发送队列数据
     */
    private void sendQueuedData(QueuedData data) {
        // 根据类型发送数据
        switch (data.getType()) {
            case "player_event":
                // 发送玩家事件
                break;
            case "status_report":
                // 发送状态报告
                break;
            // ... 其他类型
        }
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(UUID uuid) {
        Long updateTime = cacheUpdateTime.get(uuid);
        return updateTime != null &&
            (System.currentTimeMillis() - updateTime) < CACHE_TTL_MS;
    }

    /**
     * 清理过期缓存
     */
    public void cleanupCache() {
        long now = System.currentTimeMillis();
        cacheUpdateTime.entrySet().removeIf(entry -> {
            if ((now - entry.getValue()) > CACHE_TTL_MS) {
                creditCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 保存离线队列到文件
     */
    private void saveOfflineQueue() {
        // TODO: 实现持久化
        plugin.getLogger().info("保存 " + offlineQueue.size() + " 条离线数据");
    }

    /**
     * 获取缓存统计
     */
    public String getCacheStats() {
        return String.format("缓存: %d 玩家, 队列: %d 待发送",
            creditCache.size(), offlineQueue.size());
    }

    /**
     * 队列数据项
     */
    private static class QueuedData {
        private final String type;
        private final Object data;
        private final long timestamp;
        private int retryCount = 0;

        public QueuedData(String type, Object data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }

        public String getType() { return type; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetry() { retryCount++; }
    }
}
