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
 * 远程命令数据包
 */
public class CommandPacket {
    
    // 执行命令请求
    public static class CommandRequest {
        private String commandId;
        private String targetServerId; // "ALL" 表示所有服务器
        private String command;
        private boolean requireOp;
        private String executedBy;
        
        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }
        
        public String getTargetServerId() { return targetServerId; }
        public void setTargetServerId(String targetServerId) { this.targetServerId = targetServerId; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public boolean isRequireOp() { return requireOp; }
        public void setRequireOp(boolean requireOp) { this.requireOp = requireOp; }
        
        public String getExecutedBy() { return executedBy; }
        public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    }
    
    // 命令响应
    public static class CommandResponse {
        private String commandId;
        private String serverId;
        private boolean success;
        private String output;
        private String error;
        private long executionTime;
        
        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }
        
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    }
    
    // 广播消息
    public static class BroadcastRequest {
        private String message;
        private String targetServerId; // "ALL" 表示所有服务器
        private boolean useTitle; // 是否使用标题显示
        private int titleFadeIn;
        private int titleStay;
        private int titleFadeOut;
        private String executedBy;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTargetServerId() { return targetServerId; }
        public void setTargetServerId(String targetServerId) { this.targetServerId = targetServerId; }
        
        public boolean isUseTitle() { return useTitle; }
        public void setUseTitle(boolean useTitle) { this.useTitle = useTitle; }
        
        public int getTitleFadeIn() { return titleFadeIn; }
        public void setTitleFadeIn(int titleFadeIn) { this.titleFadeIn = titleFadeIn; }
        
        public int getTitleStay() { return titleStay; }
        public void setTitleStay(int titleStay) { this.titleStay = titleStay; }
        
        public int getTitleFadeOut() { return titleFadeOut; }
        public void setTitleFadeOut(int titleFadeOut) { this.titleFadeOut = titleFadeOut; }
        
        public String getExecutedBy() { return executedBy; }
        public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    }
    
    public static class BroadcastResponse {
        private boolean success;
        private int serverCount;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getServerCount() { return serverCount; }
        public void setServerCount(int serverCount) { this.serverCount = serverCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
