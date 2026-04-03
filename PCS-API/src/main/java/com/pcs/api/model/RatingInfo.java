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
import java.util.UUID;

/**
 * 评分信息
 */
public class RatingInfo {
    
    private String ratingId;
    private UUID raterUuid;
    private String raterName;
    private UUID targetUuid;
    private String targetName;
    private int score; // 评分 1-10
    private String comment;
    private String serverId;
    private Date ratedAt;
    
    public RatingInfo() {
        this.ratingId = UUID.randomUUID().toString();
        this.ratedAt = new Date();
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
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public Date getRatedAt() { return ratedAt; }
    public void setRatedAt(Date ratedAt) { this.ratedAt = ratedAt; }
}
