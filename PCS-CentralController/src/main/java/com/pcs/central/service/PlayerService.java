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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.VoteHistory;
import com.pcs.central.database.PlayerCreditRepository;
import com.pcs.central.database.VoteHistoryRepository;
import com.pcs.central.model.entity.PlayerCreditEntity;
import com.pcs.central.model.entity.VoteHistoryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);
    private static final Gson GSON = new Gson();
    
    private final PlayerCreditRepository playerCreditRepository;
    private final VoteHistoryRepository voteHistoryRepository;
    
    public PlayerService(PlayerCreditRepository playerCreditRepository,
                         VoteHistoryRepository voteHistoryRepository) {
        this.playerCreditRepository = playerCreditRepository;
        this.voteHistoryRepository = voteHistoryRepository;
    }
    
    /**
     * 获取玩家信用数据
     */
    @Transactional(readOnly = true)
    public PlayerCredit getPlayerCredit(UUID playerUuid) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, "Unknown"));
        return convertToModel(entity);
    }
    
    /**
     * 获取所有玩家（从数据库）
     */
    @Transactional(readOnly = true)
    public List<PlayerCredit> getAllPlayers() {
        List<PlayerCreditEntity> entities = playerCreditRepository.findAll();
        return entities.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取玩家信用数据（按名称，忽略大小写）
     */
    @Transactional(readOnly = true)
    public Optional<PlayerCredit> getPlayerCreditByName(String playerName) {
        // 先尝试精确匹配，再尝试忽略大小写
        Optional<PlayerCreditEntity> result = playerCreditRepository.findByPlayerName(playerName);
        if (result.isEmpty()) {
            result = playerCreditRepository.findByPlayerNameIgnoreCase(playerName);
        }
        return result.map(this::convertToModel);
    }
    
    /**
     * 获取或创建离线玩家信用数据
     * 离线玩家使用名称作为唯一标识
     */
    @Transactional
    public PlayerCredit getOrCreateOfflinePlayerCredit(String playerName) {
        // 先尝试查找现有记录
        Optional<PlayerCreditEntity> existing = playerCreditRepository.findByPlayerName(playerName);
        if (existing.isPresent()) {
            return convertToModel(existing.get());
        }
        
        // 创建新的离线玩家记录
        PlayerCreditEntity entity = new PlayerCreditEntity();
        entity.setPlayerUuidString("OFFLINE:" + playerName.toLowerCase());
        entity.setPlayerName(playerName);
        entity.setOfflineMode(true);
        entity.setOfflinePlayerName(playerName);
        entity.setCreditScore(5.0);
        entity.setTotalBans(0);
        entity.setTotalKicks(0);
        entity.setCurrentlyBanned(false);
        entity.setCheaterTag(false);
        entity.setRatedPlayersJson("{}");
        
        PlayerCreditEntity saved = playerCreditRepository.save(entity);
        logger.info("创建离线玩家记录: {}", playerName);
        return convertToModel(saved);
    }
    
    /**
     * 通过玩家名封禁玩家（支持离线账号）
     */
    @Transactional
    public boolean banPlayerByName(String playerName, String reason) {
        Optional<PlayerCreditEntity> entityOpt = playerCreditRepository.findByPlayerName(playerName);
        if (entityOpt.isPresent()) {
            PlayerCreditEntity entity = entityOpt.get();
            entity.setTotalBans(entity.getTotalBans() + 1);
            entity.setCurrentlyBanned(true);
            playerCreditRepository.save(entity);
            logger.info("封禁玩家: {}, 原因: {}", playerName, reason);
            return true;
        }
        // 如果数据库中没有，创建一个离线玩家记录并封禁
        PlayerCreditEntity entity = new PlayerCreditEntity();
        entity.setPlayerUuidString("OFFLINE:" + playerName.toLowerCase());
        entity.setPlayerName(playerName);
        entity.setOfflineMode(true);
        entity.setOfflinePlayerName(playerName);
        entity.setCreditScore(5.0);
        entity.setTotalBans(1);
        entity.setTotalKicks(0);
        entity.setCurrentlyBanned(true);
        entity.setCheaterTag(false);
        entity.setRatedPlayersJson("{}");
        playerCreditRepository.save(entity);
        logger.info("创建并封禁离线玩家: {}, 原因: {}", playerName, reason);
        return true;
    }
    
    /**
     * 通过玩家名解封玩家（支持离线账号）
     */
    @Transactional
    public boolean unbanPlayerByName(String playerName) {
        Optional<PlayerCreditEntity> entityOpt = playerCreditRepository.findByPlayerName(playerName);
        if (entityOpt.isPresent()) {
            PlayerCreditEntity entity = entityOpt.get();
            entity.setCurrentlyBanned(false);
            playerCreditRepository.save(entity);
            logger.info("解封玩家: {}", playerName);
            return true;
        }
        logger.warn("尝试解封不存在的玩家: {}", playerName);
        return false;
    }
    
    /**
     * 获取或创建玩家信用数据
     */
    @Transactional
    public PlayerCredit getOrCreatePlayerCredit(UUID playerUuid, String playerName) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElse(null);
        
        if (entity == null) {
            entity = createDefaultPlayerCredit(playerUuid, playerName);
        } else if (!entity.getPlayerName().equals(playerName)) {
            // 更新玩家名
            entity.setPlayerName(playerName);
            playerCreditRepository.save(entity);
        }
        
        return convertToModel(entity);
    }
    
    /**
     * 更新玩家信用分数
     * 信用分范围 0-10
     */
    @Transactional
    public void updateCreditScore(UUID playerUuid, double delta) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, "Unknown"));
        
        entity.setCreditScore(Math.max(0, Math.min(10, entity.getCreditScore() + delta)));
        playerCreditRepository.save(entity);
    }
    
    /**
     * 设置玩家信用分数（管理员直接设置）
     * 信用分范围 0-10
     */
    @Transactional
    public void setCreditScore(UUID playerUuid, double score) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, "Unknown"));
        
        entity.setCreditScore(Math.max(0, Math.min(10, score)));
        playerCreditRepository.save(entity);
    }
    
    /**
     * 封禁玩家
     */
    @Transactional
    public void banPlayer(UUID playerUuid, String reason) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, "Unknown"));
        
        entity.setTotalBans(entity.getTotalBans() + 1);
        entity.setCurrentlyBanned(true);
        playerCreditRepository.save(entity);
    }
    
    /**
     * 解封玩家
     */
    @Transactional
    public void unbanPlayer(UUID playerUuid) {
        Optional<PlayerCreditEntity> entityOpt = playerCreditRepository.findById(playerUuid.toString());
        if (entityOpt.isPresent()) {
            PlayerCreditEntity entity = entityOpt.get();
            entity.setCurrentlyBanned(false);
            playerCreditRepository.save(entity);
        }
    }
    
    /**
     * 记录旧封禁（装载PCS前的封禁）
     */
    @Transactional
    public void recordLegacyBan(UUID playerUuid, String playerName, String reason, 
            java.util.Date expirationDate, String sourceServer) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, playerName));
        
        // 更新玩家信息
        entity.setPlayerName(playerName);
        entity.setCurrentlyBanned(true);
        entity.setTotalBans(entity.getTotalBans() + 1);
        
        playerCreditRepository.save(entity);
        
        logger.info("已记录旧封禁: {} ({}) 来自服务器 {}", playerName, playerUuid, sourceServer);
    }
    
    /**
     * 踢出玩家
     */
    @Transactional
    public void kickPlayer(UUID playerUuid) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, "Unknown"));
        
        entity.setTotalKicks(entity.getTotalKicks() + 1);
        playerCreditRepository.save(entity);
    }
    
    /**
     * 设置作弊者标签
     */
    @Transactional
    public void setCheaterTag(UUID playerUuid, boolean tag, String reason) {
        PlayerCreditEntity entity = playerCreditRepository.findById(playerUuid.toString())
                .orElseGet(() -> createDefaultPlayerCredit(playerUuid, "Unknown"));
        
        entity.setCheaterTag(tag);
        entity.setCheaterTagReason(reason);
        entity.setCheaterTagDate(tag ? LocalDateTime.now() : null);
        playerCreditRepository.save(entity);
    }
    
    /**
     * 获取投票历史
     */
    @Transactional(readOnly = true)
    public List<VoteHistory> getVoteHistory(UUID playerUuid) {
        List<VoteHistoryEntity> entities = voteHistoryRepository.findAllRelatedVotes(playerUuid);
        return entities.stream()
                .map(this::convertHistoryToModel)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取发起的投票历史
     */
    @Transactional(readOnly = true)
    public List<VoteHistory> getInitiatedVotes(UUID playerUuid) {
        List<VoteHistoryEntity> entities = voteHistoryRepository.findByInitiatorUuidOrderByVoteTimeDesc(playerUuid);
        return entities.stream()
                .map(this::convertHistoryToModel)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取收到的投票历史
     */
    @Transactional(readOnly = true)
    public List<VoteHistory> getReceivedVotes(UUID playerUuid) {
        List<VoteHistoryEntity> entities = voteHistoryRepository.findByTargetPlayerUuidOrderByVoteTimeDesc(playerUuid);
        return entities.stream()
                .map(this::convertHistoryToModel)
                .collect(Collectors.toList());
    }
    
    private PlayerCreditEntity createDefaultPlayerCredit(UUID playerUuid, String playerName) {
        PlayerCreditEntity entity = new PlayerCreditEntity();
        entity.setPlayerUuid(playerUuid);
        entity.setPlayerName(playerName);
        entity.setCreditScore(5.0); // 初始信用分5分，满分10分
        entity.setTotalBans(0);
        entity.setTotalKicks(0);
        entity.setCurrentlyBanned(false);
        entity.setCheaterTag(false);
        entity.setRatedPlayersJson("{}");
        return playerCreditRepository.save(entity);
    }
    
    private PlayerCredit convertToModel(PlayerCreditEntity entity) {
        PlayerCredit credit = new PlayerCredit();
        credit.setPlayerUuid(entity.getPlayerUuid());
        credit.setPlayerName(entity.getPlayerName());
        
        // 信用分转换：旧数据是0-100范围，新数据是0-10范围
        double oldScore = entity.getCreditScore();
        double newScore;
        if (oldScore > 10) {
            // 旧数据，需要转换：100分 -> 10分
            newScore = oldScore / 10.0;
        } else {
            // 新数据，直接使用
            newScore = oldScore;
        }
        credit.setCreditScore(Math.max(0, Math.min(10, newScore)));
        credit.setTotalBans(entity.getTotalBans());
        credit.setTotalKicks(entity.getTotalKicks());
        credit.setCurrentlyBanned(entity.isCurrentlyBanned());
        credit.setCheaterTag(entity.isCheaterTag());
        credit.setCheaterTagReason(entity.getCheaterTagReason());
        credit.setCheaterTagDate(entity.getCheaterTagDate() != null 
                ? Date.from(entity.getCheaterTagDate().atZone(ZoneId.systemDefault()).toInstant()) : null);
        credit.setCreatedAt(Date.from(entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        credit.setUpdatedAt(Date.from(entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        
        // 解析 ratedPlayers
        if (entity.getRatedPlayersJson() != null && !entity.getRatedPlayersJson().isEmpty()) {
            Map<String, Date> ratedMap = GSON.fromJson(entity.getRatedPlayersJson(), 
                    new TypeToken<Map<String, Date>>(){}.getType());
            if (ratedMap != null) {
                Map<UUID, Date> converted = new HashMap<>();
                ratedMap.forEach((k, v) -> converted.put(UUID.fromString(k), v));
                credit.setRatedPlayers(converted);
            }
        }
        
        // 加载投票历史
        credit.setVoteHistory(getVoteHistory(entity.getPlayerUuid()));
        
        return credit;
    }
    
    private VoteHistory convertHistoryToModel(VoteHistoryEntity entity) {
        VoteHistory history = new VoteHistory();
        history.setVoteId(entity.getVoteId());
        history.setTargetPlayerUuid(entity.getTargetPlayerUuid());
        history.setTargetPlayerName(entity.getTargetPlayerName());
        history.setInitiatorUuid(entity.getInitiatorUuid());
        history.setInitiatorName(entity.getInitiatorName());
        history.setAction(entity.getAction());
        history.setReason(entity.getReason());
        history.setServerId(entity.getServerId());
        history.setServerName(entity.getServerName());
        history.setStartTime(Date.from(entity.getVoteTime().atZone(ZoneId.systemDefault()).toInstant()));
        history.setPassed(entity.isPassed());
        history.setYesVotes(entity.getYesVotes());
        history.setNoVotes(entity.getNoVotes());
        return history;
    }
}
