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
 * 玩家信用数据模型
 */
public class PlayerCredit {
    
    private UUID playerUuid;
    private String playerName;
    private double creditScore; // 信用评分 0-10，满分10分，初始5分
    private int totalBans;
    private int totalKicks;
    private boolean currentlyBanned;
    private Date lastRatingTime;
    private Map<UUID, Date> ratedPlayers; // 记录评价过的玩家及时间
    private List<VoteHistory> voteHistory;
    private List<RatingInfo> ratingHistory; // 收到的评分历史（最近N条）
    private Date createdAt;
    private Date updatedAt;
    
    // 作弊者标记
    private boolean cheaterTag;
    private String cheaterTagReason;
    private Date cheaterTagDate;
    
    public PlayerCredit() {
        this.creditScore = 5.0; // 初始信用分5分
        this.totalBans = 0;
        this.totalKicks = 0;
        this.currentlyBanned = false;
        this.ratedPlayers = new HashMap<>();
        this.voteHistory = new ArrayList<>();
        this.ratingHistory = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.cheaterTag = false;
    }
    
    public PlayerCredit(UUID playerUuid, String playerName) {
        this();
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }
    
    /**
     * 添加投票历史
     */
    public void addVoteHistory(VoteHistory history) {
        this.voteHistory.add(0, history); // 新记录放前面
        this.updatedAt = new Date();
    }
    
    /**
     * 检查是否可以在指定时间后评价某玩家
     */
    public boolean canRatePlayer(UUID targetUuid, long cooldownMillis) {
        if (!ratedPlayers.containsKey(targetUuid)) {
            return true;
        }
        Date lastRated = ratedPlayers.get(targetUuid);
        return System.currentTimeMillis() - lastRated.getTime() >= cooldownMillis;
    }
    
    /**
     * 记录对某玩家的评价
     */
    public void recordRating(UUID targetUuid) {
        ratedPlayers.put(targetUuid, new Date());
        this.lastRatingTime = new Date();
        this.updatedAt = new Date();
    }
    
    /**
     * 更新信用分数
     * 信用分范围 0-10
     */
    public void updateCreditScore(double delta) {
        this.creditScore = Math.max(0, Math.min(10, this.creditScore + delta));
        this.updatedAt = new Date();
    }
    
    /**
     * 增加封禁计数
     */
    public void incrementBanCount() {
        this.totalBans++;
        this.currentlyBanned = true;
        this.updatedAt = new Date();
    }
    
    /**
     * 增加踢出计数
     */
    public void incrementKickCount() {
        this.totalKicks++;
        this.updatedAt = new Date();
    }
    
    /**
     * 解封
     */
    public void unban() {
        this.currentlyBanned = false;
        this.updatedAt = new Date();
    }
    
    /**
     * 设置作弊者标记
     */
    public void setCheaterTag(boolean cheaterTag) {
        this.cheaterTag = cheaterTag;
        this.updatedAt = new Date();
    }
    
    /**
     * 设置作弊者标记（带原因）
     */
    public void setCheaterTag(boolean cheaterTag, String reason) {
        this.cheaterTag = cheaterTag;
        this.cheaterTagReason = reason;
        this.cheaterTagDate = cheaterTag ? new Date() : null;
        this.updatedAt = new Date();
    }
    
    // Getters and Setters
    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }
    
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
    
    public Date getLastRatingTime() { return lastRatingTime; }
    public void setLastRatingTime(Date lastRatingTime) { this.lastRatingTime = lastRatingTime; }
    
    public Map<UUID, Date> getRatedPlayers() { return ratedPlayers; }
    public void setRatedPlayers(Map<UUID, Date> ratedPlayers) { this.ratedPlayers = ratedPlayers; }
    
    public List<VoteHistory> getVoteHistory() { return voteHistory; }
    public void setVoteHistory(List<VoteHistory> voteHistory) { this.voteHistory = voteHistory; }
    
    public List<RatingInfo> getRatingHistory() { return ratingHistory; }
    public void setRatingHistory(List<RatingInfo> ratingHistory) { this.ratingHistory = ratingHistory; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isCheaterTag() { return cheaterTag; }
    
    public String getCheaterTagReason() { return cheaterTagReason; }
    public void setCheaterTagReason(String cheaterTagReason) { this.cheaterTagReason = cheaterTagReason; }
    
    public Date getCheaterTagDate() { return cheaterTagDate; }
    public void setCheaterTagDate(Date cheaterTagDate) { this.cheaterTagDate = cheaterTagDate; }
}
