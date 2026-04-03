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

package com.pcs.api.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 投票历史记录
 */
public class VoteHistory {
    
    private String voteId;
    private UUID targetPlayerUuid;
    private String targetPlayerName;
    private UUID initiatorUuid;
    private String initiatorName;
    private String action; // KICK, BAN, MUTE
    private String reason;
    private String serverId;
    private String serverName;
    private Date startTime;
    private Date endTime;
    private boolean passed;
    private List<VoteRecord> votes;
    private int yesVotes;
    private int noVotes;
    private String status; // PENDING, PASSED, REJECTED, EXPIRED
    
    public VoteHistory() {
        this.voteId = UUID.randomUUID().toString();
        this.startTime = new Date();
        this.status = "PENDING";
    }
    
    // Getters and Setters
    public String getVoteId() { return voteId; }
    public void setVoteId(String voteId) { this.voteId = voteId; }
    
    public UUID getTargetPlayerUuid() { return targetPlayerUuid; }
    public void setTargetPlayerUuid(UUID targetPlayerUuid) { this.targetPlayerUuid = targetPlayerUuid; }
    
    public String getTargetPlayerName() { return targetPlayerName; }
    public void setTargetPlayerName(String targetPlayerName) { this.targetPlayerName = targetPlayerName; }
    
    public UUID getInitiatorUuid() { return initiatorUuid; }
    public void setInitiatorUuid(UUID initiatorUuid) { this.initiatorUuid = initiatorUuid; }
    
    public String getInitiatorName() { return initiatorName; }
    public void setInitiatorName(String initiatorName) { this.initiatorName = initiatorName; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    
    public List<VoteRecord> getVotes() { return votes; }
    public void setVotes(List<VoteRecord> votes) { this.votes = votes; }
    
    public int getYesVotes() { return yesVotes; }
    public void setYesVotes(int yesVotes) { this.yesVotes = yesVotes; }
    
    public int getNoVotes() { return noVotes; }
    public void setNoVotes(int noVotes) { this.noVotes = noVotes; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
