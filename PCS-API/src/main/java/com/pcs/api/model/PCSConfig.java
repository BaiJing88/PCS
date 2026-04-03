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
 * PCS 配置（中控下发给客户端的配置）
 */
public class PCSConfig {

    // 消息配置
    private String messagePrefix; // 消息前缀
    private String successColor; // 成功颜色
    private String errorColor; // 错误颜色
    private String warnColor; // 警告颜色
    private String infoColor; // 信息颜色

    // 投票配置
    private int minTotalVotes; // 最少总票数
    private double passRate; // 通过率 (0.0 - 1.0)
    private int voteDurationSeconds; // 投票持续时间
    private int minPlayersToStart; // 启动投票所需的最少在线玩家数
    
    // 评分配置
    private int ratingCooldownMinutes; // 评分冷却时间（分钟）
    private int samePlayerRatingCooldownDays; // 对同一玩家评分的冷却时间（天）
    
    // 自动封禁配置
    private int autoBanAfterKicks; // 多少次kick后自动ban
    
    // 禁言天数
    private int muteDays; // 禁言天数

    // 投票冷却时间（分钟）
    private int voteCooldownMinutes;

    // 每日最大投票次数
    private int maxDailyVotes;

    // 每日最大评分次数
    private int maxDailyRatings;
    
    // 投票期间聊天冷却时间（秒），0表示不限制
    private int voteChatCooldownSeconds;

    // GUI配置
    private String guiTitlePrefix;
    private int playersPerPage;
    private boolean showOfflinePlayers;

    // 功能开关
    private boolean voteSystemEnabled;
    private boolean ratingSystemEnabled;
    private boolean banSyncEnabled;
    private boolean creditQueryEnabled;
    private boolean autoBanEnabled;

    // 可用操作列表
    private List<VoteAction> availableActions;
    
    // 可用理由列表
    private List<String> availableReasons;
    
    public PCSConfig() {

        // 消息配置默认值
        this.messagePrefix = "§8[§6PCS§8] §r";
        this.successColor = "§a";
        this.errorColor = "§c";
        this.warnColor = "§e";
        this.infoColor = "§7";

        this.minTotalVotes = 3;
        this.passRate = 0.6666;
        this.voteDurationSeconds = 300;
        this.minPlayersToStart = 2;
        this.ratingCooldownMinutes = 30;
        this.samePlayerRatingCooldownDays = 7;
        this.autoBanAfterKicks = 5;
        this.muteDays = 7;
        this.voteCooldownMinutes = 10;
        this.maxDailyVotes = 5;
        this.maxDailyRatings = 10;
        this.voteChatCooldownSeconds = 3;  // 默认3秒
        this.guiTitlePrefix = "§8[§6PCS§8] ";
        this.playersPerPage = 36;
        this.showOfflinePlayers = true;
        this.voteSystemEnabled = true;
        this.ratingSystemEnabled = true;
        this.banSyncEnabled = true;
        this.creditQueryEnabled = true;
        this.autoBanEnabled = true;
        this.availableActions = new ArrayList<>();
        this.availableReasons = new ArrayList<>();

        // 默认操作
        availableActions.add(new VoteAction("KICK", "踢出玩家", "将玩家踢出服务器"));
        availableActions.add(new VoteAction("BAN", "封禁玩家", "在所有服务器封禁该玩家"));
        availableActions.add(new VoteAction("MUTE", "禁言玩家", "禁言玩家指定天数"));

        // 默认理由
        availableReasons.add("盗窃、诈骗财物");
        availableReasons.add("恶意破坏他人建筑或地形（含TNT、熔岩等）");
        availableReasons.add("辱骂、歧视、人身攻击");
        availableReasons.add("刷屏、泄露隐私、网络暴力");
        availableReasons.add("强行组队、恶意跟随");
        availableReasons.add("非PVP区域偷袭");
        availableReasons.add("故意造成服务器卡顿（堆积物品、低效红石机器）");
        availableReasons.add("未经许可在他人领地建造");
        availableReasons.add("违规宣传其他服务器");
        availableReasons.add("使用作弊手段（开挂、矿透、机器人等）");
    }
    
    /**
     * 投票操作
     */
    public static class VoteAction {
        private String id;
        private String name;
        private String description;
        
        public VoteAction() {}
        
        public VoteAction(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    // Getters and Setters
    public int getMinTotalVotes() { return minTotalVotes; }
    public void setMinTotalVotes(int minTotalVotes) { this.minTotalVotes = minTotalVotes; }
    
    public double getPassRate() { return passRate; }
    public void setPassRate(double passRate) { this.passRate = passRate; }
    
    public int getVoteDurationSeconds() { return voteDurationSeconds; }
    public void setVoteDurationSeconds(int voteDurationSeconds) { this.voteDurationSeconds = voteDurationSeconds; }
    
    public int getMinPlayersToStart() { return minPlayersToStart; }
    public void setMinPlayersToStart(int minPlayersToStart) { this.minPlayersToStart = minPlayersToStart; }
    
    public int getRatingCooldownMinutes() { return ratingCooldownMinutes; }
    public void setRatingCooldownMinutes(int ratingCooldownMinutes) { this.ratingCooldownMinutes = ratingCooldownMinutes; }
    
    public int getSamePlayerRatingCooldownDays() { return samePlayerRatingCooldownDays; }
    public void setSamePlayerRatingCooldownDays(int samePlayerRatingCooldownDays) { this.samePlayerRatingCooldownDays = samePlayerRatingCooldownDays; }
    
    public int getAutoBanAfterKicks() { return autoBanAfterKicks; }
    public void setAutoBanAfterKicks(int autoBanAfterKicks) { this.autoBanAfterKicks = autoBanAfterKicks; }
    
    public int getMuteDays() { return muteDays; }
    public void setMuteDays(int muteDays) { this.muteDays = muteDays; }

    public int getVoteCooldownMinutes() { return voteCooldownMinutes; }
    public void setVoteCooldownMinutes(int voteCooldownMinutes) { this.voteCooldownMinutes = voteCooldownMinutes; }

    public int getMaxDailyVotes() { return maxDailyVotes; }
    public void setMaxDailyVotes(int maxDailyVotes) { this.maxDailyVotes = maxDailyVotes; }

    public int getMaxDailyRatings() { return maxDailyRatings; }
    public void setMaxDailyRatings(int maxDailyRatings) { this.maxDailyRatings = maxDailyRatings; }

    public String getGuiTitlePrefix() { return guiTitlePrefix; }
    public void setGuiTitlePrefix(String guiTitlePrefix) { this.guiTitlePrefix = guiTitlePrefix; }

    public int getPlayersPerPage() { return playersPerPage; }
    public void setPlayersPerPage(int playersPerPage) { this.playersPerPage = playersPerPage; }

    public boolean isShowOfflinePlayers() { return showOfflinePlayers; }
    public void setShowOfflinePlayers(boolean showOfflinePlayers) { this.showOfflinePlayers = showOfflinePlayers; }

    public boolean isVoteSystemEnabled() { return voteSystemEnabled; }
    public void setVoteSystemEnabled(boolean voteSystemEnabled) { this.voteSystemEnabled = voteSystemEnabled; }

    public boolean isRatingSystemEnabled() { return ratingSystemEnabled; }
    public void setRatingSystemEnabled(boolean ratingSystemEnabled) { this.ratingSystemEnabled = ratingSystemEnabled; }

    public boolean isBanSyncEnabled() { return banSyncEnabled; }
    public void setBanSyncEnabled(boolean banSyncEnabled) { this.banSyncEnabled = banSyncEnabled; }

    public boolean isCreditQueryEnabled() { return creditQueryEnabled; }
    public void setCreditQueryEnabled(boolean creditQueryEnabled) { this.creditQueryEnabled = creditQueryEnabled; }

    public boolean isAutoBanEnabled() { return autoBanEnabled; }
    public void setAutoBanEnabled(boolean autoBanEnabled) { this.autoBanEnabled = autoBanEnabled; }

    public List<VoteAction> getAvailableActions() { return availableActions; }
    public void setAvailableActions(List<VoteAction> availableActions) { this.availableActions = availableActions; }
    
    public List<String> getAvailableReasons() { return availableReasons; }
    public void setAvailableReasons(List<String> availableReasons) { this.availableReasons = availableReasons; }

    // 消息配置 getter/setter
    public String getMessagePrefix() { return messagePrefix; }
    public void setMessagePrefix(String messagePrefix) { this.messagePrefix = messagePrefix; }

    public String getSuccessColor() { return successColor; }
    public void setSuccessColor(String successColor) { this.successColor = successColor; }

    public String getErrorColor() { return errorColor; }
    public void setErrorColor(String errorColor) { this.errorColor = errorColor; }

    public String getWarnColor() { return warnColor; }
    public void setWarnColor(String warnColor) { this.warnColor = warnColor; }

    public String getInfoColor() { return infoColor; }
    public void setInfoColor(String infoColor) { this.infoColor = infoColor; }

    public int getVoteChatCooldownSeconds() { return voteChatCooldownSeconds; }
    public void setVoteChatCooldownSeconds(int voteChatCooldownSeconds) { this.voteChatCooldownSeconds = voteChatCooldownSeconds; }
}
