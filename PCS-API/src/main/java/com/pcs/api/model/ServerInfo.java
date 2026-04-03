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

package com.pcs.api.model;

import java.util.Date;
import java.util.UUID;

/**
 * 服务器信息
 */
public class ServerInfo {
    
    private String serverId;
    private String serverName;
    private String serverType; // SPIGOT, FABRIC, FORGE, NEOFORGE
    private String serverVersion;
    private String apiVersion;
    private String ipAddress;
    private int port;
    private Date registeredAt;
    private Date lastHeartbeat;
    private boolean online;
    private int onlinePlayers; // 在线玩家数
    private int maxPlayers;    // 最大玩家数
    private String publicKey;
    private String sharedSecret; // 共享密钥用于HMAC签名
    
    public ServerInfo() {
        this.serverId = UUID.randomUUID().toString();
        this.registeredAt = new Date();
        this.online = false;
    }
    
    // Getters and Setters
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getServerType() { return serverType; }
    public void setServerType(String serverType) { this.serverType = serverType; }
    
    public String getServerVersion() { return serverVersion; }
    public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public Date getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Date registeredAt) { this.registeredAt = registeredAt; }
    
    public Date getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Date lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    
    public int getOnlinePlayers() { return onlinePlayers; }
    public void setOnlinePlayers(int onlinePlayers) { this.onlinePlayers = onlinePlayers; }
    
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }
}
