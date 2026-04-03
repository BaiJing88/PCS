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

import com.pcs.central.database.PlayerCreditRepository;
import com.pcs.central.database.PlayerKickBanRepository;
import com.pcs.central.model.entity.PlayerKickBanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 踢出后临时封禁服务
 * 被踢出的玩家12小时内不能重新进入该特定服务器
 */
@Service
public class KickBanService {

    private static final Logger logger = LoggerFactory.getLogger(KickBanService.class);

    // 封禁时长：12小时
    public static final int BAN_DURATION_HOURS = 12;

    private final PlayerKickBanRepository kickBanRepository;
    private final PlayerCreditRepository playerCreditRepository;

    public KickBanService(PlayerKickBanRepository kickBanRepository,
                          PlayerCreditRepository playerCreditRepository) {
        this.kickBanRepository = kickBanRepository;
        this.playerCreditRepository = playerCreditRepository;
    }

    /**
     * 踢出玩家并添加临时封禁（12小时）
     */
    @Transactional
    public PlayerKickBanEntity kickAndBanPlayer(UUID playerUuid, String playerName,
                                                 String serverId, String reason, String kickedBy) {
        // 先使该玩家在该服务器上的旧封禁记录失效
        deactivateExistingBan(playerUuid, serverId);

        // 创建新的封禁记录
        PlayerKickBanEntity ban = new PlayerKickBanEntity();
        ban.setPlayerUuid(playerUuid);
        ban.setPlayerName(playerName);
        ban.setServerId(serverId);
        ban.setReason(reason != null ? reason : "管理员踢出");
        ban.setKickedBy(kickedBy);
        ban.setKickedAt(LocalDateTime.now());
        ban.setExpireAt(LocalDateTime.now().plusHours(BAN_DURATION_HOURS));
        ban.setActive(true);

        // 保存封禁记录
        PlayerKickBanEntity saved = kickBanRepository.save(ban);

        // 增加玩家的总踢出次数
        playerCreditRepository.incrementKickCount(playerUuid.toString());

        logger.info("玩家 {} ({}) 被踢出服务器 {}，临时封禁12小时，原因: {}",
                playerName, playerUuid, serverId, reason);

        return saved;
    }

    /**
     * 检查玩家是否被禁止进入特定服务器
     */
    @Transactional(readOnly = true)
    public boolean isPlayerBannedFromServer(UUID playerUuid, String serverId) {
        Optional<PlayerKickBanEntity> banOpt = kickBanRepository
                .findByPlayerUuidAndServerIdAndActiveTrue(playerUuid, serverId);

        if (banOpt.isPresent()) {
            PlayerKickBanEntity ban = banOpt.get();
            if (ban.isExpired()) {
                // 封禁已过期，自动失效
                deactivateBan(ban);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 获取玩家的封禁信息（如果存在）
     */
    @Transactional(readOnly = true)
    public Optional<PlayerKickBanEntity> getActiveBan(UUID playerUuid, String serverId) {
        Optional<PlayerKickBanEntity> banOpt = kickBanRepository
                .findByPlayerUuidAndServerIdAndActiveTrue(playerUuid, serverId);

        if (banOpt.isPresent()) {
            PlayerKickBanEntity ban = banOpt.get();
            if (ban.isExpired()) {
                deactivateBan(ban);
                return Optional.empty();
            }
            return banOpt;
        }
        return Optional.empty();
    }

    /**
     * 手动解除玩家封禁
     */
    @Transactional
    public boolean unbanPlayerFromServer(UUID playerUuid, String serverId) {
        int updated = kickBanRepository.deactivateBanForPlayerOnServer(playerUuid, serverId);
        if (updated > 0) {
            logger.info("玩家 {} 在服务器 {} 的封禁已被手动解除", playerUuid, serverId);
            return true;
        }
        return false;
    }

    /**
     * 获取玩家24小时内在特定服务器的被踢出次数
     */
    @Transactional(readOnly = true)
    public long getRecentKickCount(UUID playerUuid, String serverId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return kickBanRepository.countRecentKicks(playerUuid, serverId, since);
    }

    /**
     * 定时清理过期封禁（每10分钟执行一次）
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    @Transactional
    public void cleanupExpiredBans() {
        LocalDateTime now = LocalDateTime.now();
        int count = kickBanRepository.deactivateExpiredBans(now);
        if (count > 0) {
            logger.info("已清理 {} 条过期的踢出封禁记录", count);
        }
    }

    /**
     * 获取所有有效的封禁记录
     */
    @Transactional(readOnly = true)
    public List<PlayerKickBanEntity> getAllActiveBans() {
        return kickBanRepository.findAll().stream()
                .filter(PlayerKickBanEntity::isActive)
                .filter(ban -> !ban.isExpired())
                .toList();
    }

    /**
     * 获取特定服务器的所有有效封禁
     */
    @Transactional(readOnly = true)
    public List<PlayerKickBanEntity> getActiveBansForServer(String serverId) {
        return kickBanRepository.findByServerIdAndActiveTrue(serverId).stream()
                .filter(ban -> !ban.isExpired())
                .toList();
    }

    private void deactivateExistingBan(UUID playerUuid, String serverId) {
        kickBanRepository.deactivateBanForPlayerOnServer(playerUuid, serverId);
    }

    private void deactivateBan(PlayerKickBanEntity ban) {
        ban.setActive(false);
        kickBanRepository.save(ban);
    }
}
