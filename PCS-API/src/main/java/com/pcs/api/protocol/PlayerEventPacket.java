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

import java.util.Map;

/**
 * 玩家事件数据包
 */
public class PlayerEventPacket {

    private String eventType;
    private String playerUuid;
    private String playerName;
    private String serverId;
    private long timestamp;
    private Map<String, Object> data;

    // 事件类型常量
    public static final String JOIN = "join";
    public static final String QUIT = "quit";
    public static final String CHAT = "chat";
    public static final String COMMAND = "command";
    public static final String DEATH = "death";
    public static final String WORLD_CHANGE = "world_change";
    public static final String KICK = "kick";
    public static final String BAN = "ban";

    // Getters and Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
