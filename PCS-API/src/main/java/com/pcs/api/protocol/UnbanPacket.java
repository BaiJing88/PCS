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

import java.util.UUID;

/**
 * 解除封禁数据包
 */
public class UnbanPacket {

    /**
     * 解除封禁请求
     */
    public static class UnbanRequest {
        private String targetUuid;
        private String targetName;
        private String serverId;
        private String reason;
        private String operator;

        public UnbanRequest() {}

        public UnbanRequest(UUID targetUuid, String targetName, String serverId, String operator) {
            this.targetUuid = targetUuid.toString();
            this.targetName = targetName;
            this.serverId = serverId;
            this.operator = operator;
        }

        // Getters and Setters
        public String getTargetUuid() { return targetUuid; }
        public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }

        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
    }

    /**
     * 解除封禁响应
     */
    public static class UnbanResponse {
        private boolean success;
        private String message;
        private String targetUuid;
        private String targetName;

        public UnbanResponse() {}

        public UnbanResponse(boolean success, String message, String targetUuid, String targetName) {
            this.success = success;
            this.message = message;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTargetUuid() { return targetUuid; }
        public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
    }
}
