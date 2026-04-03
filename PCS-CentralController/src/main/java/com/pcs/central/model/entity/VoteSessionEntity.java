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

package com.pcs.central.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vote_sessions", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_target", columnList = "targetPlayerUuid"),
    @Index(name = "idx_server", columnList = "serverId")
})
public class VoteSessionEntity {
    
    @Id
    private String sessionId;
    
    @Column(nullable = false)
    private UUID targetPlayerUuid;
    
    @Column(nullable = false, length = 32)
    private String targetPlayerName;
    
    @Column(nullable = false)
    private UUID initiatorUuid;
    
    @Column(nullable = false, length = 32)
    private String initiatorName;
    
    @Column(nullable = false, length = 20)
    private String action;
    
    @Column(nullable = false, length = 500)
    private String reason;
    
    @Column(nullable = false)
    private String serverId;
    
    @Column(nullable = false, length = 64)
    private String serverName;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @Column(nullable = false)
    private int durationSeconds;
    
    @Column(nullable = false)
    private int yesVotes = 0;
    
    @Column(nullable = false)
    private int noVotes = 0;
    
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String votesJson = "[]";
    
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    
    private LocalDateTime endTime;
    
    private Boolean passed;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public int getYesVotes() { return yesVotes; }
    public void setYesVotes(int yesVotes) { this.yesVotes = yesVotes; }
    
    public int getNoVotes() { return noVotes; }
    public void setNoVotes(int noVotes) { this.noVotes = noVotes; }
    
    public String getVotesJson() { return votesJson; }
    public void setVotesJson(String votesJson) { this.votesJson = votesJson; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
