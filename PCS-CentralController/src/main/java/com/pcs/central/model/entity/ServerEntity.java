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

package com.pcs.central.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "servers", indexes = {
    @Index(name = "idx_server_name", columnList = "serverName"),
    @Index(name = "idx_online", columnList = "online")
})
public class ServerEntity {
    
    @Id
    private String serverId;
    
    @Column(nullable = false, unique = true, length = 64)
    private String serverName;
    
    @Column(nullable = false, length = 20)
    private String serverType;
    
    @Column(length = 32)
    private String serverVersion;
    
    @Column(length = 32)
    private String apiVersion;
    
    private String ipAddress;
    
    private int port;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;
    
    private LocalDateTime lastHeartbeat;
    
    @Column(nullable = false)
    private boolean online = false;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String publicKey;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String sharedSecret;
    
    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
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
    
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }
}
