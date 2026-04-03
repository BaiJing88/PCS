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

import com.pcs.api.model.PlayerCredit;
import com.pcs.api.model.VoteHistory;
import com.pcs.central.service.PlayerService;
import com.pcs.central.websocket.WebSocketSessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;
    private final WebSocketSessionManager sessionManager;

    public PlayerController(PlayerService playerService, WebSocketSessionManager sessionManager) {
        this.playerService = playerService;
        this.sessionManager = sessionManager;
    }
    
    @GetMapping("/{uuid}")
    public ResponseEntity<PlayerCredit> getPlayerCredit(@PathVariable UUID uuid) {
        return ResponseEntity.ok(playerService.getPlayerCredit(uuid));
    }
    
    @GetMapping("/name/{playerName}")
    public ResponseEntity<PlayerCredit> getPlayerCreditByName(@PathVariable String playerName) {
        // 先从数据库查询
        Optional<PlayerCredit> credit = playerService.getPlayerCreditByName(playerName);
        if (credit.isPresent()) {
            return ResponseEntity.ok(credit.get());
        }

        // 如果数据库中没有，尝试从在线玩家列表中查找正确的UUID
        UUID onlinePlayerUuid = findOnlinePlayerUuid(playerName);
        if (onlinePlayerUuid != null) {
            // 使用在线玩家的正确UUID创建记录
            return ResponseEntity.ok(playerService.getOrCreatePlayerCredit(onlinePlayerUuid, playerName));
        }

        // 如果在线列表中也没有，返回404（而不是创建随机UUID）
        return ResponseEntity.notFound().build();
    }

    /**
     * 从在线玩家列表中查找UUID
     */
    private UUID findOnlinePlayerUuid(String playerName) {
        List<Map<String, Object>> onlinePlayers = sessionManager.getAllOnlinePlayers();
        for (Map<String, Object> player : onlinePlayers) {
            String name = (String) player.get("name");
            if (playerName.equalsIgnoreCase(name)) {
                Object uuidObj = player.get("uuid");
                if (uuidObj != null) {
                    String uuidStr = uuidObj.toString();
                    try {
                        return UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
    
    @GetMapping("/{uuid}/history")
    public ResponseEntity<List<VoteHistory>> getVoteHistory(@PathVariable UUID uuid) {
        return ResponseEntity.ok(playerService.getVoteHistory(uuid));
    }
    
    @GetMapping("/{uuid}/history/initiated")
    public ResponseEntity<List<VoteHistory>> getInitiatedVotes(@PathVariable UUID uuid) {
        return ResponseEntity.ok(playerService.getInitiatedVotes(uuid));
    }
    
    @GetMapping("/{uuid}/history/received")
    public ResponseEntity<List<VoteHistory>> getReceivedVotes(@PathVariable UUID uuid) {
        return ResponseEntity.ok(playerService.getReceivedVotes(uuid));
    }
    
    /**
     * 修改玩家信用分（管理员接口）
     */
    @PostMapping("/{playerName}/credit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> modifyCredit(
            @PathVariable String playerName,
            @RequestBody Map<String, Object> request,
            @RequestAttribute("username") String modifiedBy) {
        
        String operation = (String) request.get("operation"); // "add" or "set"
        Double value = request.get("value") instanceof Number ? 
                ((Number) request.get("value")).doubleValue() : null;
        
        if (operation == null || value == null || (!"add".equals(operation) && !"set".equals(operation))) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid operation or value. Use 'add' or 'set'"
            ));
        }
        
        // 查找玩家
        Optional<PlayerCredit> creditOpt = playerService.getPlayerCreditByName(playerName);
        if (creditOpt.isEmpty()) {
            // 尝试从在线玩家查找
            UUID onlineUuid = findOnlinePlayerUuid(playerName);
            if (onlineUuid == null) {
                return ResponseEntity.notFound().build();
            }
            // 创建玩家记录
            playerService.getOrCreatePlayerCredit(onlineUuid, playerName);
            creditOpt = playerService.getPlayerCreditByName(playerName);
        }
        
        if (creditOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        PlayerCredit credit = creditOpt.get();
        double oldScore = credit.getCreditScore();
        double newScore;
        
        if ("add".equals(operation)) {
            newScore = Math.max(0, Math.min(10, oldScore + value));
            playerService.updateCreditScore(credit.getPlayerUuid(), value);
        } else { // set
            newScore = Math.max(0, Math.min(10, value));
            playerService.setCreditScore(credit.getPlayerUuid(), newScore);
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "playerName", playerName,
            "operation", operation,
            "oldScore", oldScore,
            "newScore", newScore,
            "modifiedBy", modifiedBy
        ));
    }
}
