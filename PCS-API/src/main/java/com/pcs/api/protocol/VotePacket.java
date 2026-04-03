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
 * 投票相关数据包
 */
public class VotePacket {
    
    // 发起投票
    public static class StartVoteRequest {
        private UUID initiatorUuid;
        private String initiatorName;
        private UUID targetUuid;
        private String targetName;
        private String action;
        private String reason;
        private String serverId;
        
        public UUID getInitiatorUuid() { return initiatorUuid; }
        public void setInitiatorUuid(UUID initiatorUuid) { this.initiatorUuid = initiatorUuid; }
        
        public String getInitiatorName() { return initiatorName; }
        public void setInitiatorName(String initiatorName) { this.initiatorName = initiatorName; }
        
        public UUID getTargetUuid() { return targetUuid; }
        public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
        
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
    }
    
    public static class StartVoteResponse {
        private boolean success;
        private String voteId;
        private String message;
        private int durationSeconds;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getVoteId() { return voteId; }
        public void setVoteId(String voteId) { this.voteId = voteId; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    }
    
    // 投票
    public static class CastVoteRequest {
        private String voteId;
        private UUID voterUuid;
        private String voterName;
        private boolean agree;
        private String serverId;
        
        public String getVoteId() { return voteId; }
        public void setVoteId(String voteId) { this.voteId = voteId; }
        
        public UUID getVoterUuid() { return voterUuid; }
        public void setVoterUuid(UUID voterUuid) { this.voterUuid = voterUuid; }
        
        public String getVoterName() { return voterName; }
        public void setVoterName(String voterName) { this.voterName = voterName; }
        
        public boolean isAgree() { return agree; }
        public void setAgree(boolean agree) { this.agree = agree; }
        
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
    }
    
    public static class CastVoteResponse {
        private boolean success;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    // 投票结果
    public static class VoteResult {
        private String voteId;
        private boolean passed;
        private int yesVotes;
        private int noVotes;
        private String action;
        private UUID targetUuid;
        private String targetName;
        
        public String getVoteId() { return voteId; }
        public void setVoteId(String voteId) { this.voteId = voteId; }
        
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public int getYesVotes() { return yesVotes; }
        public void setYesVotes(int yesVotes) { this.yesVotes = yesVotes; }
        
        public int getNoVotes() { return noVotes; }
        public void setNoVotes(int noVotes) { this.noVotes = noVotes; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public UUID getTargetUuid() { return targetUuid; }
        public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
        
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
    }
    
    // 投票通知（广播给所有服务器）
    public static class VoteNotification {
        private String voteId;
        private UUID targetUuid;
        private String targetName;
        private UUID initiatorUuid;
        private String initiatorName;
        private String action;
        private String reason;
        private String serverName;
        private int durationSeconds;
        private int remainingSeconds;
        
        public String getVoteId() { return voteId; }
        public void setVoteId(String voteId) { this.voteId = voteId; }
        
        public UUID getTargetUuid() { return targetUuid; }
        public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
        
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        
        public UUID getInitiatorUuid() { return initiatorUuid; }
        public void setInitiatorUuid(UUID initiatorUuid) { this.initiatorUuid = initiatorUuid; }
        
        public String getInitiatorName() { return initiatorName; }
        public void setInitiatorName(String initiatorName) { this.initiatorName = initiatorName; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getServerName() { return serverName; }
        public void setServerName(String serverName) { this.serverName = serverName; }
        
        public int getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public int getRemainingSeconds() { return remainingSeconds; }
        public void setRemainingSeconds(int remainingSeconds) { this.remainingSeconds = remainingSeconds; }
    }
    
    // 投票结束通知
    public static class VoteEndNotification {
        private String voteId;
        private boolean passed;
        private int yesVotes;
        private int noVotes;
        private String action;
        private UUID targetUuid;
        private String targetName;
        private String reason;
        
        public String getVoteId() { return voteId; }
        public void setVoteId(String voteId) { this.voteId = voteId; }
        
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public int getYesVotes() { return yesVotes; }
        public void setYesVotes(int yesVotes) { this.yesVotes = yesVotes; }
        
        public int getNoVotes() { return noVotes; }
        public void setNoVotes(int noVotes) { this.noVotes = noVotes; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public UUID getTargetUuid() { return targetUuid; }
        public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
        
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
