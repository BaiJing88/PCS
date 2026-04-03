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
import com.pcs.spigot.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 禁言管理器
 * 处理投票禁言功能
 */
public class MuteManager implements Listener {

    private final PCSSpigotPlugin plugin;

    // 被禁言的玩家 Map<UUID, MuteInfo>
    private final Map<UUID, MuteInfo> mutedPlayers = new ConcurrentHashMap<>();

    /**
     * 禁言信息
     */
    public static class MuteInfo {
        private final String reason;
        private final Date endTime;
        private final Date startTime;

        public MuteInfo(String reason, Date endTime) {
            this.reason = reason;
            this.endTime = endTime;
            this.startTime = new Date();
        }

        public String getReason() { return reason; }
        public Date getEndTime() { return endTime; }
        public Date getStartTime() { return startTime; }

        public boolean isExpired() {
            return new Date().after(endTime);
        }

        public long getRemainingSeconds() {
            long remaining = (endTime.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        }
    }

    public MuteManager(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 禁言玩家
     */
    public void mutePlayer(UUID playerUuid, String reason, Date endTime) {
        mutedPlayers.put(playerUuid, new MuteInfo(reason, endTime));
        plugin.getLogger().info("Player " + playerUuid + " muted until " + endTime + " for reason: " + reason);
    }

    /**
     * 解除禁言
     */
    public void unmutePlayer(UUID playerUuid) {
        mutedPlayers.remove(playerUuid);
        plugin.getLogger().info("Player " + playerUuid + " unmuted");
    }

    /**
     * 检查玩家是否被禁言
     */
    public boolean isMuted(UUID playerUuid) {
        MuteInfo info = mutedPlayers.get(playerUuid);
        if (info == null) {
            return false;
        }

        // 检查是否过期
        if (info.isExpired()) {
            mutedPlayers.remove(playerUuid);
            return false;
        }

        return true;
    }

    /**
     * 获取禁言信息
     */
    public MuteInfo getMuteInfo(UUID playerUuid) {
        MuteInfo info = mutedPlayers.get(playerUuid);
        if (info != null && info.isExpired()) {
            mutedPlayers.remove(playerUuid);
            return null;
        }
        return info;
    }

    /**
     * 清理过期的禁言
     */
    public void cleanupExpiredMutes() {
        mutedPlayers.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 监听聊天事件，阻止被禁言玩家发言
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        MuteInfo muteInfo = getMuteInfo(uuid);

        if (muteInfo != null) {
            // 阻止发言
            event.setCancelled(true);

            // 发送提示
            long remainingSeconds = muteInfo.getRemainingSeconds();
            long remainingDays = remainingSeconds / 86400;
            long remainingHours = (remainingSeconds % 86400) / 3600;
            long remainingMinutes = (remainingSeconds % 3600) / 60;

            String timeStr;
            if (remainingDays > 0) {
                timeStr = remainingDays + " 天 " + remainingHours + " 小时";
            } else if (remainingHours > 0) {
                timeStr = remainingHours + " 小时 " + remainingMinutes + " 分钟";
            } else {
                timeStr = remainingMinutes + " 分钟";
            }

            MessageUtil.error(player, "你当前处于禁言状态！");
            player.sendMessage("§7剩余时间: §f" + timeStr);
            player.sendMessage("§7原因: §f" + muteInfo.getReason());
        }
    }

    /**
     * 获取所有被禁言的玩家
     */
    public Map<UUID, MuteInfo> getAllMutedPlayers() {
        cleanupExpiredMutes();
        return new ConcurrentHashMap<>(mutedPlayers);
    }
}
