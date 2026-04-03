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

package com.pcs.central.service;

import com.pcs.api.model.RatingInfo;
import com.pcs.api.protocol.PacketType;
import com.pcs.api.protocol.ProtocolPacket;
import com.pcs.central.database.PlayerCreditRepository;
import com.pcs.central.database.RatingRepository;
import com.pcs.central.model.entity.PlayerCreditEntity;
import com.pcs.central.model.entity.RatingEntity;
import com.pcs.central.websocket.WebSocketSessionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 评分服务
 * 处理玩家评分逻辑，包括权重计算
 */
@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final PlayerCreditRepository playerCreditRepository;
    private final ConfigService configService;
    private final WebSocketSessionManager sessionManager;

    public RatingService(RatingRepository ratingRepository,
                        PlayerCreditRepository playerCreditRepository,
                        ConfigService configService,
                        WebSocketSessionManager sessionManager) {
        this.ratingRepository = ratingRepository;
        this.playerCreditRepository = playerCreditRepository;
        this.configService = configService;
        this.sessionManager = sessionManager;
    }

    /**
     * 提交评分（带权重计算）
     * 自身评分越低，给别人打出的评分影响越小
     */
    @Transactional
    public void submitRating(RatingInfo ratingInfo) {
        UUID raterUuid = ratingInfo.getRaterUuid();
        UUID targetUuid = ratingInfo.getTargetUuid();
        int rawScore = ratingInfo.getScore(); // 1-10

        // 获取评分者的信用分
        PlayerCreditEntity raterCredit = playerCreditRepository.findById(raterUuid.toString())
            .orElse(null);

        // 计算权重（基于评分者的信用分）
        // 信用分范围 0-10，初始5分
        // 权重公式: 0.1 + (creditScore / 10.0) * 0.9
        // 结果: 0分->0.1权重, 5分->0.55权重, 10分->1.0权重
        double weight;
        if (raterCredit != null) {
            double creditScore = raterCredit.getCreditScore();
            // 确保信用分在0-10范围内
            creditScore = Math.max(0, Math.min(10, creditScore));
            weight = 0.1 + (creditScore / 10.0) * 0.9;
        } else {
            // 未知玩家使用默认权重0.55（对应5分）
            weight = 0.55;
        }

        // 计算加权后的分数
        // 加权分数 = 1 + (原始分数 - 1) * 权重
        // 这样1分始终保持为1分（最低），10分根据权重调整
        double weightedScore = 1 + (rawScore - 1) * weight;

        // 检查冷却时间
        var config = configService.getConfig();
        LocalDateTime cooldownTime = LocalDateTime.now()
            .minusMinutes(config.getRatingCooldownMinutes());

        var existingRating = ratingRepository.findRecentRating(
            raterUuid, targetUuid, cooldownTime);

        if (existingRating.isPresent()) {
            // 更新现有评分
            RatingEntity entity = existingRating.get();
            entity.setScore(rawScore);
            entity.setWeightedScore(weightedScore);
            entity.setWeight(weight);
            entity.setComment(ratingInfo.getComment());
            entity.setRatedAt(LocalDateTime.now());
            ratingRepository.save(entity);
        } else {
            // 创建新评分
            RatingEntity entity = new RatingEntity();
            entity.setRatingId(UUID.randomUUID().toString());
            entity.setRaterUuid(raterUuid);
            entity.setRaterName(ratingInfo.getRaterName());
            entity.setTargetUuid(targetUuid);
            entity.setTargetName(ratingInfo.getTargetName());
            entity.setScore(rawScore);
            entity.setWeightedScore(weightedScore);
            entity.setWeight(weight);
            entity.setComment(ratingInfo.getComment());
            entity.setServerId(ratingInfo.getServerId());
            entity.setRatedAt(LocalDateTime.now());
            ratingRepository.save(entity);
        }

        // 更新目标玩家的信用分
        updateTargetCreditScore(targetUuid);
    }

    /**
     * 更新目标玩家的信用分
     */
    private void updateTargetCreditScore(UUID targetUuid) {
        // 计算加权平均分
        Double avgWeightedScore = ratingRepository.getAverageWeightedRatingForPlayer(targetUuid);

        if (avgWeightedScore != null) {
            // 限制信用分在 0-10 范围内（满分10分）
            double finalScore = Math.max(0, Math.min(10, avgWeightedScore));

            // 更新玩家信用分
            PlayerCreditEntity credit = playerCreditRepository.findById(targetUuid.toString())
                .orElse(null);
            if (credit != null) {
                credit.setCreditScore(finalScore);
                playerCreditRepository.save(credit);
                
                // 广播信用分更新到所有连接的Spigot服务器
                broadcastCreditUpdate(targetUuid, credit);
            }
        }
    }
    
    /**
     * 广播信用分更新到所有Spigot服务器
     */
    private void broadcastCreditUpdate(UUID targetUuid, PlayerCreditEntity credit) {
        if (sessionManager == null) return;
        
        Map<String, Object> updateData = Map.of(
            "playerUuid", targetUuid.toString(),
            "playerName", credit.getPlayerName() != null ? credit.getPlayerName() : "Unknown",
            "creditScore", credit.getCreditScore(),
            "banCount", credit.getTotalBans(),
            "kickCount", credit.getTotalKicks(),
            "currentlyBanned", credit.isCurrentlyBanned(),
            "cheater", credit.isCheaterTag(),
            "timestamp", System.currentTimeMillis()
        );
        
        ProtocolPacket packet = ProtocolPacket.request(PacketType.RATING_UPDATE, updateData);
        sessionManager.broadcast(packet);
    }

    /**
     * 获取玩家的加权平均评分
     * 返回0-10范围内的分数
     */
    @Transactional(readOnly = true)
    public double getWeightedAverageRating(UUID playerUuid) {
        Double avg = ratingRepository.getAverageWeightedRatingForPlayer(playerUuid);
        return avg != null ? avg : 5.0; // 默认5分
    }

    /**
     * 获取评分者的权重
     * 信用分0-10映射到权重0.1-1.0
     */
    @Transactional(readOnly = true)
    public double getRaterWeight(UUID raterUuid) {
        PlayerCreditEntity credit = playerCreditRepository.findById(raterUuid.toString())
            .orElse(null);
        if (credit != null) {
            double creditScore = Math.max(0, Math.min(10, credit.getCreditScore()));
            return 0.1 + (creditScore / 10.0) * 0.9;
        }
        return 0.55; // 默认权重（对应5分）
    }
}
