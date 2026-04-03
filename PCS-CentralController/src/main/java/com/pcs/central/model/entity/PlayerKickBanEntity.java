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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 玩家被踢出后的临时封禁记录
 * 被踢出后12小时内不能重新进入该特定服务器
 */
@Entity
@Table(name = "player_kick_bans", indexes = {
    @Index(name = "idx_player_server", columnList = "playerUuid,serverId"),
    @Index(name = "idx_expire_time", columnList = "expireAt"),
    @Index(name = "idx_active", columnList = "active")
})
public class PlayerKickBanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerUuid;

    @Column(nullable = false, length = 32)
    private String playerName;

    @Column(nullable = false, length = 64)
    private String serverId;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime kickedAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 32)
    private String kickedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getKickedAt() { return kickedAt; }
    public void setKickedAt(LocalDateTime kickedAt) { this.kickedAt = kickedAt; }

    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getKickedBy() { return kickedBy; }
    public void setKickedBy(String kickedBy) { this.kickedBy = kickedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * 检查此封禁是否已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireAt);
    }
}
