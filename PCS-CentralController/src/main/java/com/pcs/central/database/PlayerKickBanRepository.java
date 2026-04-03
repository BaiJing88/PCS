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

package com.pcs.central.database;

import com.pcs.central.model.entity.PlayerKickBanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerKickBanRepository extends JpaRepository<PlayerKickBanEntity, Long> {

    /**
     * 查找玩家在特定服务器的有效封禁记录
     */
    Optional<PlayerKickBanEntity> findByPlayerUuidAndServerIdAndActiveTrue(UUID playerUuid, String serverId);

    /**
     * 查找玩家在所有服务器的有效封禁记录
     */
    List<PlayerKickBanEntity> findByPlayerUuidAndActiveTrue(UUID playerUuid);

    /**
     * 查找特定服务器的所有有效封禁记录
     */
    List<PlayerKickBanEntity> findByServerIdAndActiveTrue(String serverId);

    /**
     * 检查玩家是否在特定服务器被临时封禁
     */
    boolean existsByPlayerUuidAndServerIdAndActiveTrueAndExpireAtAfter(UUID playerUuid, String serverId, LocalDateTime now);

    /**
     * 查找所有已过期的封禁记录
     */
    List<PlayerKickBanEntity> findByExpireAtBeforeAndActiveTrue(LocalDateTime now);

    /**
     * 使过期封禁记录失效
     */
    @Modifying
    @Query("UPDATE PlayerKickBanEntity p SET p.active = false WHERE p.expireAt < :now AND p.active = true")
    int deactivateExpiredBans(@Param("now") LocalDateTime now);

    /**
     * 删除特定玩家的特定服务器封禁记录（用于手动解除）
     */
    @Modifying
    @Query("UPDATE PlayerKickBanEntity p SET p.active = false WHERE p.playerUuid = :playerUuid AND p.serverId = :serverId AND p.active = true")
    int deactivateBanForPlayerOnServer(@Param("playerUuid") UUID playerUuid, @Param("serverId") String serverId);

    /**
     * 统计玩家在特定服务器的被踢出次数（24小时内）
     */
    @Query("SELECT COUNT(p) FROM PlayerKickBanEntity p WHERE p.playerUuid = :playerUuid AND p.serverId = :serverId AND p.kickedAt > :since")
    long countRecentKicks(@Param("playerUuid") UUID playerUuid, @Param("serverId") String serverId, @Param("since") LocalDateTime since);
}
