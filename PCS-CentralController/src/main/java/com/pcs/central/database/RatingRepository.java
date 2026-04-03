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

import com.pcs.central.model.entity.RatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<RatingEntity, String> {
    
    List<RatingEntity> findByTargetUuidOrderByRatedAtDesc(UUID targetUuid);
    
    List<RatingEntity> findByRaterUuidOrderByRatedAtDesc(UUID raterUuid);
    
    Optional<RatingEntity> findByRaterUuidAndTargetUuid(UUID raterUuid, UUID targetUuid);
    
    @Query("SELECT AVG(r.score) FROM RatingEntity r WHERE r.targetUuid = :targetUuid")
    Double getAverageRatingForPlayer(@Param("targetUuid") UUID targetUuid);

    @Query("SELECT AVG(r.weightedScore) FROM RatingEntity r WHERE r.targetUuid = :targetUuid")
    Double getAverageWeightedRatingForPlayer(@Param("targetUuid") UUID targetUuid);
    
    @Query("SELECT r FROM RatingEntity r WHERE r.raterUuid = :raterUuid AND r.targetUuid = :targetUuid AND r.ratedAt > :since")
    Optional<RatingEntity> findRecentRating(@Param("raterUuid") UUID raterUuid, 
                                            @Param("targetUuid") UUID targetUuid,
                                            @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(r) FROM RatingEntity r WHERE r.raterUuid = :raterUuid AND r.ratedAt > :since")
    long countRecentRatingsByRater(@Param("raterUuid") UUID raterUuid, @Param("since") LocalDateTime since);
    
    // 通过玩家名称查询（用于API）
    List<RatingEntity> findByTargetNameOrderByRatedAtDesc(String targetName);
    
    List<RatingEntity> findByRaterNameOrderByRatedAtDesc(String raterName);
    
    // 统计查询
    @Query("SELECT COUNT(r) FROM RatingEntity r WHERE r.ratedAt >= :since")
    int countTodayRatings(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(r) FROM RatingEntity r")
    long count();
    
    @Query("SELECT COUNT(DISTINCT r.raterUuid) FROM RatingEntity r")
    int countDistinctRaters();
    
    @Query("SELECT COUNT(DISTINCT r.targetUuid) FROM RatingEntity r")
    int countDistinctTargets();
    
    @Query("SELECT AVG(r.score) FROM RatingEntity r")
    Double getAverageScore();
    
    @Query("SELECT AVG(r.weightedScore) FROM RatingEntity r")
    Double getAverageWeightedScore();
    
    @Query("SELECT r FROM RatingEntity r WHERE r.ratedAt >= :since ORDER BY r.ratedAt DESC")
    List<RatingEntity> findTodayRatings(@Param("since") LocalDateTime since);
}
