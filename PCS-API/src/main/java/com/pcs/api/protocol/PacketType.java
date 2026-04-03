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

/**
 * 数据包类型
 */
public enum PacketType {
    
    // 连接相关
    HANDSHAKE("handshake"),
    AUTH_REQUEST("auth_request"),
    AUTH_RESPONSE("auth_response"),
    HEARTBEAT("heartbeat"),
    DISCONNECT("disconnect"),
    
    // 配置相关
    CONFIG_REQUEST("config_request"),
    CONFIG_RESPONSE("config_response"),
    CONFIG_UPDATE("config_update"),
    
    // 玩家数据相关
    PLAYER_DATA_REQUEST("player_data_request"),
    PLAYER_DATA_RESPONSE("player_data_response"),
    PLAYER_UPDATE("player_update"),
    
    // 投票相关
    VOTE_START("vote_start"),
    VOTE_CAST("vote_cast"),
    VOTE_RESULT("vote_result"),
    VOTE_NOTIFY("vote_notify"),
    VOTE_END("vote_end"),
    
    // 评分相关
    RATING_SUBMIT("rating_submit"),
    RATING_RESPONSE("rating_response"),
    RATING_UPDATE("rating_update"),  // 评分后推送信用分更新
    
    // 封禁相关
    BAN_NOTIFY("ban_notify"),
    UNBAN_NOTIFY("unban_notify"),
    UNBAN_REQUEST("unban_request"),
    UNBAN_RESPONSE("unban_response"),
    BAN_SYNC("ban_sync"),
    LEGACY_BAN_SYNC("legacy_ban_sync"),  // 旧封禁同步
    
    // 广播相关
    BROADCAST("broadcast"),
    BROADCAST_RESPONSE("broadcast_response"),
    
    // 远程命令
    REMOTE_COMMAND("remote_command"),
    REMOTE_COMMAND_RESPONSE("remote_command_response"),

    // 玩家管理
    KICK_PLAYER("kick_player"),
    PRIVATE_MESSAGE("private_message"),
    
    // 禁言相关
    MUTE_PLAYER("mute_player"),
    UNMUTE_PLAYER("unmute_player"),

    // 错误
    ERROR("error"),

    // Encryption
    KEY_EXCHANGE("key_exchange"),
    ENCRYPTED("encrypted"),

    // 服务器状态上报
    STATUS_REPORT("status_report"),
    STATUS_RESPONSE("status_response"),

    // 玩家事件
    PLAYER_EVENT("player_event"),

    // 服务器控制命令
    SERVER_CONTROL("server_control"),
    SERVER_CONTROL_RESPONSE("server_control_response"),
    
    // CLI Master认证
    MASTER_AUTH("master_auth"),
    MASTER_COMMAND("master_command"),
    MASTER_RESPONSE("master_response");

    private final String type;
    
    PacketType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public static PacketType fromString(String type) {
        for (PacketType pt : values()) {
            if (pt.type.equalsIgnoreCase(type)) {
                return pt;
            }
        }
        return null;
    }
}
