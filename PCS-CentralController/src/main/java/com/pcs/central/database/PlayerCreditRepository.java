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

import com.pcs.central.model.entity.PlayerCreditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerCreditRepository extends JpaRepository<PlayerCreditEntity, String> {
    
    Optional<PlayerCreditEntity> findByPlayerName(String playerName);
    
    // 忽略大小写的查询
    Optional<PlayerCreditEntity> findByPlayerNameIgnoreCase(String playerName);
    
    Optional<PlayerCreditEntity> findByPlayerUuid(String playerUuid);
    
    List<PlayerCreditEntity> findByOfflineModeTrue();
    
    List<PlayerCreditEntity> findByCurrentlyBannedTrue();
    
    List<PlayerCreditEntity> findByCheaterTagTrue();
    
    Page<PlayerCreditEntity> findByOrderByCreditScoreDesc(Pageable pageable);
    
    @Modifying
    @Query("UPDATE PlayerCreditEntity p SET p.creditScore = :score WHERE p.playerUuid = :uuid")
    void updateCreditScore(@Param("uuid") String uuid, @Param("score") double score);
    
    @Modifying
    @Query("UPDATE PlayerCreditEntity p SET p.currentlyBanned = :banned WHERE p.playerUuid = :uuid")
    void updateBanStatus(@Param("uuid") String uuid, @Param("banned") boolean banned);
    
    @Modifying
    @Query("UPDATE PlayerCreditEntity p SET p.totalBans = p.totalBans + 1, p.currentlyBanned = true WHERE p.playerUuid = :uuid")
    void incrementBanCount(@Param("uuid") String uuid);
    
    @Modifying
    @Query("UPDATE PlayerCreditEntity p SET p.totalKicks = p.totalKicks + 1 WHERE p.playerUuid = :uuid")
    void incrementKickCount(@Param("uuid") String uuid);
    
    @Query("SELECT p FROM PlayerCreditEntity p WHERE p.creditScore < :minScore")
    List<PlayerCreditEntity> findLowCreditPlayers(@Param("minScore") double minScore);
}
