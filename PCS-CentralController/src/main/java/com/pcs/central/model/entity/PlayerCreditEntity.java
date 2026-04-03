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
@Table(name = "player_credits", indexes = {
    @Index(name = "idx_player_name", columnList = "playerName"),
    @Index(name = "idx_credit_score", columnList = "creditScore"),
    @Index(name = "idx_currently_banned", columnList = "currentlyBanned"),
    @Index(name = "idx_offline_mode", columnList = "offlineMode")
})
public class PlayerCreditEntity {
    
    @Id
    @Column(length = 36)
    private String playerUuid;  // 改为String类型，支持离线玩家的UUID格式
    
    @Column(nullable = false, length = 32)
    private String playerName;
    
    @Column(nullable = false)
    private boolean offlineMode = false;  // 是否为离线账号
    
    @Column(length = 32)
    private String offlinePlayerName;  // 离线玩家原始名称（区分大小写）
    
    @Column(nullable = false)
    private double creditScore = 5.0; // 初始信用分5分，满分10分
    
    @Column(nullable = false)
    private int totalBans = 0;
    
    @Column(nullable = false)
    private int totalKicks = 0;
    
    @Column(nullable = false)
    private boolean currentlyBanned = false;
    
    private LocalDateTime lastRatingTime;
    
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String ratedPlayersJson = "{}";
    
    @Column(nullable = false)
    private boolean cheaterTag = false;
    
    private String cheaterTagReason;
    
    private LocalDateTime cheaterTagDate;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getPlayerUuid() { 
        if (playerUuid == null || playerUuid.isEmpty()) return null;
        try {
            return UUID.fromString(playerUuid); 
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public void setPlayerUuid(UUID playerUuid) { 
        this.playerUuid = playerUuid != null ? playerUuid.toString() : null; 
    }
    
    public String getPlayerUuidString() { return playerUuid; }
    public void setPlayerUuidString(String playerUuid) { this.playerUuid = playerUuid; }
    
    public boolean isOfflineMode() { return offlineMode; }
    public void setOfflineMode(boolean offlineMode) { this.offlineMode = offlineMode; }
    
    public String getOfflinePlayerName() { return offlinePlayerName; }
    public void setOfflinePlayerName(String offlinePlayerName) { this.offlinePlayerName = offlinePlayerName; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public double getCreditScore() { return creditScore; }
    public void setCreditScore(double creditScore) { this.creditScore = creditScore; }
    
    public int getTotalBans() { return totalBans; }
    public void setTotalBans(int totalBans) { this.totalBans = totalBans; }
    
    public int getTotalKicks() { return totalKicks; }
    public void setTotalKicks(int totalKicks) { this.totalKicks = totalKicks; }
    
    public boolean isCurrentlyBanned() { return currentlyBanned; }
    public void setCurrentlyBanned(boolean currentlyBanned) { this.currentlyBanned = currentlyBanned; }
    
    public LocalDateTime getLastRatingTime() { return lastRatingTime; }
    public void setLastRatingTime(LocalDateTime lastRatingTime) { this.lastRatingTime = lastRatingTime; }
    
    public String getRatedPlayersJson() { return ratedPlayersJson; }
    public void setRatedPlayersJson(String ratedPlayersJson) { this.ratedPlayersJson = ratedPlayersJson; }
    
    public boolean isCheaterTag() { return cheaterTag; }
    public void setCheaterTag(boolean cheaterTag) { this.cheaterTag = cheaterTag; }
    
    public String getCheaterTagReason() { return cheaterTagReason; }
    public void setCheaterTagReason(String cheaterTagReason) { this.cheaterTagReason = cheaterTagReason; }
    
    public LocalDateTime getCheaterTagDate() { return cheaterTagDate; }
    public void setCheaterTagDate(LocalDateTime cheaterTagDate) { this.cheaterTagDate = cheaterTagDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
