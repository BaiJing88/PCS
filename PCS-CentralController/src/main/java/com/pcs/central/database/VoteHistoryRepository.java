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

import com.pcs.central.model.entity.VoteHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoteHistoryRepository extends JpaRepository<VoteHistoryEntity, String> {
    
    List<VoteHistoryEntity> findByTargetPlayerUuidOrderByVoteTimeDesc(UUID targetPlayerUuid);
    
    List<VoteHistoryEntity> findByInitiatorUuidOrderByVoteTimeDesc(UUID initiatorUuid);
    
    Page<VoteHistoryEntity> findByTargetPlayerUuidOrderByVoteTimeDesc(UUID targetPlayerUuid, Pageable pageable);
    
    Page<VoteHistoryEntity> findByInitiatorUuidOrderByVoteTimeDesc(UUID initiatorUuid, Pageable pageable);
    
    @Query("SELECT v FROM VoteHistoryEntity v WHERE v.targetPlayerUuid = :uuid OR v.initiatorUuid = :uuid ORDER BY v.voteTime DESC")
    List<VoteHistoryEntity> findAllRelatedVotes(@Param("uuid") UUID uuid);
    
    @Query("SELECT COUNT(v) FROM VoteHistoryEntity v WHERE v.targetPlayerUuid = :uuid AND v.passed = true AND v.action = 'BAN'")
    long countSuccessfulBansAgainstPlayer(@Param("uuid") UUID uuid);
    
    @Query("SELECT COUNT(v) FROM VoteHistoryEntity v WHERE v.targetPlayerUuid = :uuid AND v.passed = true AND v.action = 'KICK'")
    long countSuccessfulKicksAgainstPlayer(@Param("uuid") UUID uuid);
}
