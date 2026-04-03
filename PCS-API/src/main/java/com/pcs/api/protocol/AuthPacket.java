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

package com.pcs.api.protocol;

/**
 * 认证数据包
 */
public class AuthPacket {
    
    private String serverId;
    private String serverName;
    private String serverType;
    private String serverVersion;
    private String apiVersion;
    private String publicKey;
    private String challenge; // 挑战码
    private String response; // 响应（对挑战码的签名）
    private String sharedSecret; // 协商的共享密钥
    private boolean success;
    private String message;
    private String token; // 认证成功后的访问令牌
    private String apiKey; // API密钥
    private boolean assignedId; // 标记服务器ID是否由中控新分配
    
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
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public String getChallenge() { return challenge; }
    public void setChallenge(String challenge) { this.challenge = challenge; }
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    
    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public boolean isAssignedId() { return assignedId; }
    public void setAssignedId(boolean assignedId) { this.assignedId = assignedId; }
}
