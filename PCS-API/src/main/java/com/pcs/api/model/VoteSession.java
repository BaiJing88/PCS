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

import java.util.*;

/**
 * 投票会话（进行中的投票）
 */
public class VoteSession {
    
    private String sessionId;
    private UUID targetPlayerUuid;
    private String targetPlayerName;
    private UUID initiatorUuid;
    private String initiatorName;
    private String action;
    private String reason;
    private String serverId;
    private String serverName;
    private Date startTime;
    private int durationSeconds;
    private Map<UUID, VoteRecord> votes;
    private Set<String> votedServers;
    private String status;
    
    public VoteSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = new Date();
        this.votes = new HashMap<>();
        this.votedServers = new HashSet<>();
        this.status = "ACTIVE";
    }
    
    /**
     * 添加投票
     */
    public boolean addVote(VoteRecord vote) {
        if (votes.containsKey(vote.getVoterUuid())) {
            return false; // 已经投过票
        }
        votes.put(vote.getVoterUuid(), vote);
        votedServers.add(vote.getServerId());
        return true;
    }
    
    /**
     * 检查玩家是否已投票
     */
    public boolean hasVoted(UUID playerUuid) {
        return votes.containsKey(playerUuid);
    }
    
    /**
     * 获取同意票数
     */
    public int getYesCount() {
        return (int) votes.values().stream().filter(VoteRecord::isAgree).count();
    }
    
    /**
     * 获取反对票数
     */
    public int getNoCount() {
        return (int) votes.values().stream().filter(v -> !v.isAgree()).count();
    }
    
    /**
     * 获取总票数
     */
    public int getTotalVotes() {
        return votes.size();
    }
    
    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        long elapsed = (System.currentTimeMillis() - startTime.getTime()) / 1000;
        return elapsed > durationSeconds;
    }
    
    /**
     * 获取剩余时间（秒）
     */
    public int getRemainingSeconds() {
        long elapsed = (System.currentTimeMillis() - startTime.getTime()) / 1000;
        return Math.max(0, durationSeconds - (int) elapsed);
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
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
    
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public Map<UUID, VoteRecord> getVotes() { return votes; }
    public void setVotes(Map<UUID, VoteRecord> votes) { this.votes = votes; }
    
    public Set<String> getVotedServers() { return votedServers; }
    public void setVotedServers(Set<String> votedServers) { this.votedServers = votedServers; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
