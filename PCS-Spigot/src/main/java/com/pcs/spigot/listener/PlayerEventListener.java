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

package com.pcs.spigot.listener;

import com.pcs.api.protocol.PacketType;
import com.pcs.api.protocol.PlayerEventPacket;
import com.pcs.api.protocol.ProtocolPacket;
import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 玩家事件监听器
 * 监听玩家行为并上报到中控服务器
 */
public class PlayerEventListener implements Listener {

    private final PCSSpigotPlugin plugin;
    private final ScheduledExecutorService pingUpdateExecutor;

    public PlayerEventListener(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
        this.pingUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
        startPingUpdateTask();
    }
    
    /**
     * 启动定期更新玩家ping的任务
     */
    private void startPingUpdateTask() {
        pingUpdateExecutor.scheduleAtFixedRate(() -> {
            if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isAuthenticated()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("ping", player.getPing());
                        data.put("world", player.getWorld().getName());
                        data.put("location", formatLocation(player));
                        
                        PlayerEventPacket event = new PlayerEventPacket();
                        event.setEventType("PING_UPDATE");
                        event.setPlayerUuid(player.getUniqueId().toString());
                        event.setPlayerName(player.getName());
                        event.setServerId(plugin.getConfigManager().getServerId());
                        event.setTimestamp(System.currentTimeMillis());
                        event.setData(data);
                        
                        ProtocolPacket packet = ProtocolPacket.request(PacketType.PLAYER_EVENT, event);
                        plugin.getWebSocketClient().send(packet.toJson());
                    } catch (Exception e) {
                        // 忽略单个玩家更新错误
                    }
                }
            }
        }, 10, 10, TimeUnit.SECONDS); // 每10秒更新一次
    }

    /**
     * 发送玩家事件
     */
    private void sendPlayerEvent(String eventType, Player player, Map<String, Object> data) {
        if (plugin.getWebSocketClient() == null) {
            // WebSocket 未初始化，跳过发送
            return;
        }

        if (!plugin.getWebSocketClient().isAuthenticated()) {
            // 尚未认证，不发送事件
            return;
        }

        try {
            PlayerEventPacket event = new PlayerEventPacket();
            event.setEventType(eventType);
            event.setPlayerUuid(player.getUniqueId().toString());
            event.setPlayerName(player.getName());
            event.setServerId(plugin.getConfigManager().getServerId());
            event.setTimestamp(System.currentTimeMillis());
            event.setData(data);

            ProtocolPacket packet = ProtocolPacket.request(PacketType.PLAYER_EVENT, event);
            plugin.getWebSocketClient().send(packet.toJson());

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "发送玩家事件失败", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = new HashMap<>();
        data.put("ip", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown");
        data.put("firstJoin", !player.hasPlayedBefore());
        data.put("location", formatLocation(player));
        data.put("world", player.getWorld().getName());
        data.put("ping", 0); // 初始ping为0，后续会更新

        sendPlayerEvent(PlayerEventPacket.JOIN, player, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = new HashMap<>();
        data.put("playTime", getPlayTime(player));
        data.put("location", formatLocation(player));
        data.put("world", player.getWorld().getName());
        data.put("reason", "normal"); // 正常退出

        // 使用同步发送确保退出事件能被中控接收
        sendPlayerEventSync(PlayerEventPacket.QUIT, player, data);
    }
    
    /**
     * 同步发送玩家事件（确保在服务器关闭前发送）
     */
    private void sendPlayerEventSync(String eventType, Player player, Map<String, Object> data) {
        if (plugin.getWebSocketClient() == null) {
            // WebSocket 未初始化，无法发送同步事件
            return;
        }

        try {
            PlayerEventPacket event = new PlayerEventPacket();
            event.setEventType(eventType);
            event.setPlayerUuid(player.getUniqueId().toString());
            event.setPlayerName(player.getName());
            event.setServerId(plugin.getConfigManager().getServerId());
            event.setTimestamp(System.currentTimeMillis());
            event.setData(data);

            ProtocolPacket packet = ProtocolPacket.request(PacketType.PLAYER_EVENT, event);
            
            // 同步发送，不检查认证状态（尽可能发送）
            if (plugin.getWebSocketClient().isOpen()) {
                plugin.getWebSocketClient().send(packet.toJson());
            } else {
                // 未连接，无法发送
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "同步发送玩家事件失败: " + eventType, e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = new HashMap<>();
        data.put("message", event.getMessage());
        data.put("format", event.getFormat());
        data.put("recipients", event.getRecipients().size());

        // 异步事件需要在主线程发送
        Bukkit.getScheduler().runTask(plugin, () ->
            sendPlayerEvent(PlayerEventPacket.CHAT, player, data)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].substring(1);

        // 检查是否上报命令事件（可在配置中设置白名单/黑名单）
        if (shouldReportCommand(command)) {
            Map<String, Object> data = new HashMap<>();
            data.put("command", command);
            data.put("fullCommand", event.getMessage());

            sendPlayerEvent(PlayerEventPacket.COMMAND, player, data);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Map<String, Object> data = new HashMap<>();
        data.put("deathMessage", event.getDeathMessage());
        data.put("keepInventory", event.getKeepInventory());
        data.put("keepLevel", event.getKeepLevel());
        data.put("droppedExp", event.getDroppedExp());

        if (player.getKiller() != null) {
            data.put("killer", player.getKiller().getName());
            data.put("killerUuid", player.getKiller().getUniqueId().toString());
        }

        sendPlayerEvent(PlayerEventPacket.DEATH, player, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = new HashMap<>();
        data.put("fromWorld", event.getFrom().getName());
        data.put("toWorld", player.getWorld().getName());
        data.put("location", formatLocation(player));

        sendPlayerEvent(PlayerEventPacket.WORLD_CHANGE, player, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = new HashMap<>();
        data.put("reason", event.getReason());
        data.put("leaveMessage", event.getLeaveMessage());

        sendPlayerEvent(PlayerEventPacket.KICK, player, data);
    }

    /**
     * 格式化位置信息
     */
    private String formatLocation(Player player) {
        return String.format("%s [%.1f, %.1f, %.1f]",
            player.getWorld().getName(),
            player.getLocation().getX(),
            player.getLocation().getY(),
            player.getLocation().getZ()
        );
    }

    /**
     * 获取游戏时间（分钟）
     */
    private long getPlayTime(Player player) {
        return player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60;
    }

    /**
     * 检查是否应该上报命令
     */
    private boolean shouldReportCommand(String command) {
        // 可以从配置读取命令上报规则
        // 目前上报所有命令，除了一些常见且无风险的原生命令
        String[] ignored = {"help", "?", "plugins", "pl", "version", "ver", "about"};
        for (String ignore : ignored) {
            if (command.equalsIgnoreCase(ignore)) {
                return false;
            }
        }
        return true;
    }
}
