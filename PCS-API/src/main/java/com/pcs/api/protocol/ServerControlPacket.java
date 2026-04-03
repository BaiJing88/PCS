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
 * 服务器控制命令数据包
 */
public class ServerControlPacket {

    // 命令类型常量
    public static final String RESTART = "restart";
    public static final String RELOAD_CONFIG = "reload_config";
    public static final String SHUTDOWN = "shutdown";
    public static final String BROADCAST_TITLE = "broadcast_title";
    public static final String EXECUTE_AS_PLAYER = "execute_as_player";
    public static final String GET_PLAYER_LIST = "get_player_list";
    public static final String GET_WORLD_INFO = "get_world_info";

    /**
     * 控制命令请求
     */
    public static class ControlRequest {
        private String commandId;
        private String commandType;
        private String targetServer;
        private Object params;

        // Getters and Setters
        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }

        public String getCommandType() { return commandType; }
        public void setCommandType(String commandType) { this.commandType = commandType; }

        public String getTargetServer() { return targetServer; }
        public void setTargetServer(String targetServer) { this.targetServer = targetServer; }

        public Object getParams() { return params; }
        public void setParams(Object params) { this.params = params; }
    }

    /**
     * 控制命令响应
     */
    public static class ControlResponse {
        private String commandId;
        private boolean success;
        private String message;
        private Object data;

        // Getters and Setters
        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    /**
     * 标题广播参数
     */
    public static class TitleParams {
        private String title;
        private String subtitle;
        private int fadeIn;
        private int stay;
        private int fadeOut;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

        public int getFadeIn() { return fadeIn; }
        public void setFadeIn(int fadeIn) { this.fadeIn = fadeIn; }

        public int getStay() { return stay; }
        public void setStay(int stay) { this.stay = stay; }

        public int getFadeOut() { return fadeOut; }
        public void setFadeOut(int fadeOut) { this.fadeOut = fadeOut; }
    }

    /**
     * 以玩家执行命令参数
     */
    public static class ExecuteAsPlayerParams {
        private String playerUuid;
        private String command;

        // Getters and Setters
        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }
}
