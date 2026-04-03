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
import com.pcs.api.model.*;
import com.pcs.central.database.*;
import com.pcs.central.model.entity.*;
import com.pcs.central.websocket.WebSocketSessionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VoteService {
    
    private static final Gson GSON = new Gson();
    
    private final VoteSessionRepository voteSessionRepository;
    private final VoteHistoryRepository voteHistoryRepository;
    private final PlayerCreditRepository playerCreditRepository;
    private final ConfigService configService;
    private final WebSocketSessionManager sessionManager;
    private final PlayerService playerService;
    
    // 内存缓存活跃投票
    private final Map<String, VoteSession> activeSessions = new ConcurrentHashMap<>();
    
    public VoteService(VoteSessionRepository voteSessionRepository,
                       VoteHistoryRepository voteHistoryRepository,
                       PlayerCreditRepository playerCreditRepository,
                       ConfigService configService,
                       WebSocketSessionManager sessionManager,
                       PlayerService playerService) {
        this.voteSessionRepository = voteSessionRepository;
        this.voteHistoryRepository = voteHistoryRepository;
        this.playerCreditRepository = playerCreditRepository;
        this.configService = configService;
        this.sessionManager = sessionManager;
        this.playerService = playerService;
    }
    
    /**
     * 发起投票
     */
    @Transactional
    public VoteSession startVote(UUID initiatorUuid, String initiatorName,
                                  UUID targetUuid, String targetName,
                                  String action, String reason, String serverId, String serverName) {
        PCSConfig config = configService.getConfig();
        
        // 检查是否已有针对该玩家的活跃投票
        List<VoteSessionEntity> existingSessions = voteSessionRepository
                .findByTargetPlayerUuidAndStatus(targetUuid, "ACTIVE");
        if (!existingSessions.isEmpty()) {
            throw new IllegalStateException("该玩家已有进行中的投票");
        }
        
        VoteSession session = new VoteSession();
        session.setTargetPlayerUuid(targetUuid);
        session.setTargetPlayerName(targetName);
        session.setInitiatorUuid(initiatorUuid);
        session.setInitiatorName(initiatorName);
        session.setAction(action);
        session.setReason(reason);
        session.setServerId(serverId);
        session.setServerName(serverName);
        session.setDurationSeconds(config.getVoteDurationSeconds());
        
        // 保存到数据库
        VoteSessionEntity entity = new VoteSessionEntity();
        entity.setSessionId(session.getSessionId());
        entity.setTargetPlayerUuid(targetUuid);
        entity.setTargetPlayerName(targetName);
        entity.setInitiatorUuid(initiatorUuid);
        entity.setInitiatorName(initiatorName);
        entity.setAction(action);
        entity.setReason(reason);
        entity.setServerId(serverId);
        entity.setServerName(serverName);
        entity.setStartTime(LocalDateTime.ofInstant(session.getStartTime().toInstant(), ZoneId.systemDefault()));
        entity.setDurationSeconds(config.getVoteDurationSeconds());
        voteSessionRepository.save(entity);
        
        // 加入内存缓存
        activeSessions.put(session.getSessionId(), session);
        
        // 【移除跨服投票】不再广播到所有服务器
        // broadcastVoteNotification(session);
        
        return session;
    }
    
    /**
     * 投票
     */
    @Transactional
    public boolean castVote(String sessionId, UUID voterUuid, String voterName, boolean agree, String serverId) {
        VoteSession session = activeSessions.get(sessionId);
        if (session == null) {
            // 从数据库加载
            Optional<VoteSessionEntity> entityOpt = voteSessionRepository
                    .findBySessionIdAndStatus(sessionId, "ACTIVE");
            if (entityOpt.isEmpty()) {
                throw new IllegalStateException("投票不存在或已结束");
            }
            session = loadFromEntity(entityOpt.get());
            activeSessions.put(sessionId, session);
        }
        
        if (session.isExpired()) {
            throw new IllegalStateException("投票已过期");
        }
        
        VoteRecord vote = new VoteRecord(voterUuid, voterName, agree, serverId);
        if (!session.addVote(vote)) {
            throw new IllegalStateException("您已经投过票了");
        }
        
        // 更新数据库
        if (agree) {
            voteSessionRepository.incrementYesVotes(sessionId);
        } else {
            voteSessionRepository.incrementNoVotes(sessionId);
        }
        
        // 检查投票结果
        checkVoteResult(session);
        
        return true;
    }
    
    /**
     * 检查投票结果
     */
    private void checkVoteResult(VoteSession session) {
        PCSConfig config = configService.getConfig();
        
        int totalVotes = session.getTotalVotes();
        int yesVotes = session.getYesCount();
        int noVotes = session.getNoCount();
        
        // 检查是否达到通过条件
        boolean canDecide = totalVotes >= config.getMinTotalVotes();
        boolean passed = false;
        boolean shouldEnd = session.isExpired();
        
        if (canDecide) {
            double passRate = (double) yesVotes / totalVotes;
            if (passRate >= config.getPassRate()) {
                passed = true;
                shouldEnd = true;
            } else if (noVotes > totalVotes * (1 - config.getPassRate())) {
                // 反对票超过阈值，投票失败
                passed = false;
                shouldEnd = true;
            }
        }
        
        if (shouldEnd) {
            endVote(session, passed);
        }
    }
    
    /**
     * 结束投票
     */
    @Transactional
    public void endVote(VoteSession session, boolean passed) {
        session.setStatus(passed ? "PASSED" : "REJECTED");
        activeSessions.remove(session.getSessionId());
        
        LocalDateTime endTime = LocalDateTime.now();
        
        // 更新数据库
        voteSessionRepository.updateStatus(
                session.getSessionId(),
                passed ? "PASSED" : "REJECTED",
                endTime,
                passed
        );
        
        // 记录历史
        VoteHistoryEntity history = new VoteHistoryEntity();
        history.setVoteId(session.getSessionId());
        history.setTargetPlayerUuid(session.getTargetPlayerUuid());
        history.setTargetPlayerName(session.getTargetPlayerName());
        history.setInitiatorUuid(session.getInitiatorUuid());
        history.setInitiatorName(session.getInitiatorName());
        history.setAction(session.getAction());
        history.setReason(session.getReason());
        history.setServerId(session.getServerId());
        history.setServerName(session.getServerName());
        history.setVoteTime(LocalDateTime.ofInstant(session.getStartTime().toInstant(), ZoneId.systemDefault()));
        history.setPassed(passed);
        history.setYesVotes(session.getYesCount());
        history.setNoVotes(session.getNoCount());
        voteHistoryRepository.save(history);
        
        // 如果通过，执行操作
        if (passed) {
            executeAction(session);
        }
        
        // 广播结果
        // 【移除跨服投票】不再广播到所有服务器
        // broadcastVoteResult(session, passed);
    }
    
    /**
     * 执行投票通过的操作
     */
    private void executeAction(VoteSession session) {
        String action = session.getAction();
        UUID targetUuid = session.getTargetPlayerUuid();
        PCSConfig config = configService.getConfig();

        switch (action.toUpperCase()) {
            case "KICK":
                playerCreditRepository.incrementKickCount(targetUuid.toString());
                // 检查是否需要自动Ban
                checkAutoBan(targetUuid);
                break;
            case "BAN":
                playerCreditRepository.incrementBanCount(targetUuid.toString());
                break;
            case "MUTE":
                // 禁言记录由服务器端处理
                break;
        }

        // 同步到所有服务器，包含禁言天数配置
        sessionManager.broadcastBanSync(targetUuid, action, session.getReason(),
            config.getMuteDays());
    }
    
    /**
     * 检查自动Ban
     */
    private void checkAutoBan(UUID playerUuid) {
        PCSConfig config = configService.getConfig();
        Optional<PlayerCreditEntity> creditOpt = playerCreditRepository.findById(playerUuid.toString());
        
        if (creditOpt.isPresent()) {
            PlayerCreditEntity credit = creditOpt.get();
            if (credit.getTotalKicks() >= config.getAutoBanAfterKicks() && !credit.isCheaterTag()) {
                // 打上作弊者标签
                credit.setCheaterTag(true);
                credit.setCheaterTagReason("累计被踢出 " + credit.getTotalKicks() + " 次");
                credit.setCheaterTagDate(LocalDateTime.now());
                playerCreditRepository.save(credit);
            }
        }
    }
    
    /**
     * 定时检查过期投票
     */
    @Scheduled(fixedRate = 10000) // 每10秒
    @Transactional
    public void checkExpiredVotes() {
        LocalDateTime expireTime = LocalDateTime.now().minusSeconds(1);
        List<VoteSessionEntity> expired = voteSessionRepository.findExpiredSessions(expireTime);
        
        for (VoteSessionEntity entity : expired) {
            VoteSession session = activeSessions.get(entity.getSessionId());
            if (session == null) {
                session = loadFromEntity(entity);
            }
            
            // 计算当前结果
            int totalVotes = entity.getYesVotes() + entity.getNoVotes();
            boolean passed = false;
            
            if (totalVotes >= configService.getConfig().getMinTotalVotes()) {
                double passRate = (double) entity.getYesVotes() / totalVotes;
                passed = passRate >= configService.getConfig().getPassRate();
            }
            
            endVote(session, passed);
        }
    }
    
    /**
     * 从实体加载会话
     */
    private VoteSession loadFromEntity(VoteSessionEntity entity) {
        VoteSession session = new VoteSession();
        session.setSessionId(entity.getSessionId());
        session.setTargetPlayerUuid(entity.getTargetPlayerUuid());
        session.setTargetPlayerName(entity.getTargetPlayerName());
        session.setInitiatorUuid(entity.getInitiatorUuid());
        session.setInitiatorName(entity.getInitiatorName());
        session.setAction(entity.getAction());
        session.setReason(entity.getReason());
        session.setServerId(entity.getServerId());
        session.setServerName(entity.getServerName());
        session.setStartTime(Date.from(entity.getStartTime().atZone(ZoneId.systemDefault()).toInstant()));
        session.setDurationSeconds(entity.getDurationSeconds());
        session.setStatus(entity.getStatus());
        
        // 加载投票记录
        if (entity.getVotesJson() != null) {
            List<VoteRecord> votes = GSON.fromJson(entity.getVotesJson(), 
                    new TypeToken<List<VoteRecord>>(){}.getType());
            if (votes != null) {
                for (VoteRecord vote : votes) {
                    session.getVotes().put(vote.getVoterUuid(), vote);
                }
            }
        }
        
        return session;
    }
    
    public Optional<VoteSession> getActiveVote(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }
    
    public List<VoteSession> getAllActiveVotes() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * 获取最近的投票历史记录
     */
    public List<VoteHistoryEntity> getRecentVoteHistory(int limit) {
        return voteHistoryRepository.findAll().stream()
            .sorted((a, b) -> b.getVoteTime().compareTo(a.getVoteTime()))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
