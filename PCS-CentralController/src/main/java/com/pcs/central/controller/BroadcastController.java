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

package com.pcs.central.controller;

import com.pcs.central.websocket.WebSocketSessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/broadcast")
public class BroadcastController {
    
    private final WebSocketSessionManager sessionManager;
    
    public BroadcastController(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    /**
     * 广播消息 - 支持JSON请求体（供Web和CLI使用）
     * 需要ADMIN或SUPER_ADMIN权限
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> broadcast(
            @RequestBody Map<String, Object> request,
            @RequestAttribute(name = "username", required = false) String executedBy) {
        
        // 验证用户是否已认证
        if (executedBy == null || executedBy.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Unauthorized: Please login first"
            ));
        }
        
        String message = (String) request.get("message");
        String targetServerId = (String) request.getOrDefault("targetServerId", "ALL");
        Boolean useTitle = (Boolean) request.getOrDefault("useTitle", false);
        
        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Message cannot be empty"
            ));
        }
        
        // 检查是否有在线服务器
        int onlineServers = sessionManager.getOnlineServerCount();
        if (onlineServers == 0) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "No online servers available for broadcast"
            ));
        }
        
        // 默认title参数
        int titleFadeIn = 10;
        int titleStay = 70;
        int titleFadeOut = 20;
        
        try {
            sessionManager.broadcastMessage(message, targetServerId, useTitle, titleFadeIn, titleStay, titleFadeOut);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Broadcast sent successfully",
                "targetServers", targetServerId.equals("ALL") ? "all" : targetServerId,
                "useTitle", useTitle,
                "executedBy", executedBy,
                "onlineServerCount", onlineServers
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Broadcast failed: " + e.getMessage()
            ));
        }
    }
}
