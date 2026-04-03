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

@Entity
@Table(name = "ratings", indexes = {
    @Index(name = "idx_rater", columnList = "raterUuid"),
    @Index(name = "idx_target", columnList = "targetUuid"),
    @Index(name = "idx_rated_at", columnList = "ratedAt")
})
public class RatingEntity {
    
    @Id
    private String ratingId;
    
    @Column(nullable = false)
    private UUID raterUuid;
    
    @Column(nullable = false, length = 32)
    private String raterName;
    
    @Column(nullable = false)
    private UUID targetUuid;
    
    @Column(nullable = false, length = 32)
    private String targetName;
    
    @Column(nullable = false)
    private int score; // 原始分数 1-10

    @Column(nullable = false)
    private double weightedScore; // 加权后的分数

    @Column(nullable = false)
    private double weight; // 评分权重 0.1-1.0

    @Column(length = 500)
    private String comment;
    
    @Column(nullable = false)
    private String serverId;
    
    @Column(nullable = false)
    private LocalDateTime ratedAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }
    
    public UUID getRaterUuid() { return raterUuid; }
    public void setRaterUuid(UUID raterUuid) { this.raterUuid = raterUuid; }
    
    public String getRaterName() { return raterName; }
    public void setRaterName(String raterName) { this.raterName = raterName; }
    
    public UUID getTargetUuid() { return targetUuid; }
    public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
    
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public double getWeightedScore() { return weightedScore; }
    public void setWeightedScore(double weightedScore) { this.weightedScore = weightedScore; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public LocalDateTime getRatedAt() { return ratedAt; }
    public void setRatedAt(LocalDateTime ratedAt) { this.ratedAt = ratedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
