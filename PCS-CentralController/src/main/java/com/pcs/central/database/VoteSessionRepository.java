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

import com.pcs.central.model.entity.VoteSessionEntity;
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
public interface VoteSessionRepository extends JpaRepository<VoteSessionEntity, String> {
    
    List<VoteSessionEntity> findByStatus(String status);
    
    List<VoteSessionEntity> findByTargetPlayerUuidAndStatus(UUID targetPlayerUuid, String status);
    
    List<VoteSessionEntity> findByServerId(String serverId);
    
    Optional<VoteSessionEntity> findBySessionIdAndStatus(String sessionId, String status);
    
    @Modifying
    @Query("UPDATE VoteSessionEntity v SET v.status = :status, v.endTime = :endTime, v.passed = :passed WHERE v.sessionId = :sessionId")
    void updateStatus(@Param("sessionId") String sessionId, 
                      @Param("status") String status,
                      @Param("endTime") LocalDateTime endTime,
                      @Param("passed") Boolean passed);
    
    @Modifying
    @Query("UPDATE VoteSessionEntity v SET v.yesVotes = v.yesVotes + 1 WHERE v.sessionId = :sessionId")
    void incrementYesVotes(@Param("sessionId") String sessionId);
    
    @Modifying
    @Query("UPDATE VoteSessionEntity v SET v.noVotes = v.noVotes + 1 WHERE v.sessionId = :sessionId")
    void incrementNoVotes(@Param("sessionId") String sessionId);
    
    @Query("SELECT v FROM VoteSessionEntity v WHERE v.status = 'ACTIVE' AND v.startTime < :expireTime")
    List<VoteSessionEntity> findExpiredSessions(@Param("expireTime") LocalDateTime expireTime);
}
