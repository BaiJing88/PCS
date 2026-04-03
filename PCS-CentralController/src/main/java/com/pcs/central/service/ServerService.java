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

import com.pcs.central.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ServerService {

    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);
    
    private final WebSocketSessionManager sessionManager;

    public ServerService(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public List<Map<String, Object>> getAllServers() {
        List<Map<String, Object>> servers = new ArrayList<>();
        
        Map<String, Map<String, Object>> onlineServers = sessionManager.getOnlineServers();
        
        for (Map.Entry<String, Map<String, Object>> entry : onlineServers.entrySet()) {
            Map<String, Object> server = new HashMap<>();
            server.put("serverId", entry.getKey());
            server.putAll(entry.getValue());
            servers.add(server);
        }
        
        return servers;
    }

    public List<Map<String, Object>> getServerPlayers(String serverId) {
        return sessionManager.getServerPlayers(serverId);
    }

    public void broadcast(String message, String serverId) {
        if (serverId == null || serverId.isEmpty()) {
            // Broadcast to all servers
            sessionManager.broadcastMessage(message, null, false, 10, 70, 20);
        } else {
            // Broadcast to specific server
            sessionManager.broadcastMessage(message, serverId, false, 10, 70, 20);
        }
    }

    public String executeCommand(String serverId, String command) {
        String commandId = UUID.randomUUID().toString();
        
        // 先发送命令
        boolean sent = sessionManager.sendRemoteCommand(serverId, command, commandId, "CLI");
        if (!sent) {
            return "命令发送失败: 服务器 " + serverId + " 未连接或不在线";
        }
        
        // 等待响应（最多5秒）
        try {
            CompletableFuture<String> future = sessionManager.waitForCommandResponse(commandId, 5000);
            String output = future.get(5, TimeUnit.SECONDS);
            return output.isEmpty() ? "命令执行成功（无输出）" : output;
        } catch (Exception e) {
            return "命令执行超时或无响应: " + e.getMessage();
        }
    }
}
