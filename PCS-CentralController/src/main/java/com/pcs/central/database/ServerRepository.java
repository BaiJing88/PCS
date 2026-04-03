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

import com.pcs.central.model.entity.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<ServerEntity, String> {
    
    Optional<ServerEntity> findByServerName(String serverName);
    
    List<ServerEntity> findByOnlineTrue();
    
    List<ServerEntity> findByServerType(String serverType);
    
    @Modifying
    @Query("UPDATE ServerEntity s SET s.online = :online, s.lastHeartbeat = :heartbeat WHERE s.serverId = :serverId")
    void updateOnlineStatus(@Param("serverId") String serverId, 
                           @Param("online") boolean online,
                           @Param("heartbeat") LocalDateTime heartbeat);
    
    @Query("SELECT s FROM ServerEntity s WHERE s.online = true AND s.lastHeartbeat < :timeout")
    List<ServerEntity> findStaleServers(@Param("timeout") LocalDateTime timeout);
}
