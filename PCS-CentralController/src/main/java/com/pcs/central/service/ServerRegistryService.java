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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器注册服务
 * 负责管理服务器ID分配和注册
 */
@Service
public class ServerRegistryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerRegistryService.class);
    
    // 已注册的服务器：serverId -> serverInfo
    private final Map<String, RegisteredServer> registeredServers = new ConcurrentHashMap<>();
    
    // API Key到服务器ID列表的映射（一个API Key可以对应多个服务器）
    private final Map<String, Set<String>> apiKeyToServerIds = new ConcurrentHashMap<>();
    
    /**
     * 注册新服务器或获取已注册服务器ID
     * 
     * @param requestedId 客户端请求的服务器ID（首次连接时可能为null或临时ID）
     * @param apiKey API密钥
     * @param serverName 服务器名称
     * @param serverType 服务器类型
     * @return 分配的服务器ID
     */
    public String registerOrGetServerId(String requestedId, String apiKey, 
                                         String serverName, String serverType) {
        String typePrefix = serverType != null ? serverType.toLowerCase() : "unknown";
        
        // 如果客户端提供了有效的服务器ID且已存在，检查类型是否匹配
        if (requestedId != null && !requestedId.isEmpty() && 
            registeredServers.containsKey(requestedId)) {
            RegisteredServer existing = registeredServers.get(requestedId);
            // 验证API Key是否匹配
            if (existing != null && existing.getApiKey().equals(apiKey)) {
                // 检查类型是否匹配（防止Fabric使用Spigot的ID）
                if (existing.getType() != null && serverType != null && 
                    !existing.getType().equalsIgnoreCase(serverType)) {
                    logger.warn("服务器类型不匹配: 现有类型={}, 请求类型={}, 生成新ID", 
                               existing.getType(), serverType);
                    // 类型不匹配，生成新的ID
                    String serverId = generateUniqueServerId(apiKey, serverType);
                    registerServer(serverId, apiKey, serverName, serverType);
                    logger.info("新服务器注册(类型冲突)，中控分配ID: {}", serverId);
                    return serverId;
                }
                logger.info("服务器重新连接: {}", requestedId);
                return requestedId;
            }
        }
        
        // 如果客户端提供了新的服务器ID且未被使用，接受它（但强制添加类型前缀）
        if (requestedId != null && !requestedId.isEmpty() && 
            !registeredServers.containsKey(requestedId)) {
            // 确保ID包含正确的类型前缀
            String serverId = requestedId;
            if (!requestedId.toLowerCase().startsWith(typePrefix + "-")) {
                serverId = typePrefix + "-" + requestedId;
                logger.info("服务器ID添加类型前缀: {} -> {}", requestedId, serverId);
            }
            registerServer(serverId, apiKey, serverName, serverType);
            logger.info("新服务器注册，使用客户端提供的ID: {}", serverId);
            return serverId;
        }
        
        // 生成新的唯一服务器ID（基于API Key + 类型 + 随机数）
        String serverId = generateUniqueServerId(apiKey, serverType);
        registerServer(serverId, apiKey, serverName, serverType);
        logger.info("新服务器注册，中控分配ID: {}", serverId);
        return serverId;
    }
    
    /**
     * 获取服务器信息
     */
    public RegisteredServer getServer(String serverId) {
        return registeredServers.get(serverId);
    }
    
    /**
     * 检查服务器是否已注册
     */
    public boolean isRegistered(String serverId) {
        return registeredServers.containsKey(serverId);
    }
    
    /**
     * 注册服务器
     */
    private void registerServer(String serverId, String apiKey, 
                                String serverName, String serverType) {
        RegisteredServer server = new RegisteredServer();
        server.setId(serverId);
        server.setApiKey(apiKey);
        server.setName(serverName);
        server.setType(serverType);
        server.setRegisteredAt(System.currentTimeMillis());
        server.setLastSeenAt(System.currentTimeMillis());
        
        registeredServers.put(serverId, server);
        
        // 将服务器ID添加到该API Key对应的集合中
        apiKeyToServerIds.computeIfAbsent(apiKey, k -> ConcurrentHashMap.newKeySet()).add(serverId);
    }
    
    /**
     * 更新服务器信息（用于已存在的服务器重新连接时更新信息）
     */
    public void updateServerInfo(String serverId, String serverName, String serverType, String serverVersion) {
        RegisteredServer server = registeredServers.get(serverId);
        if (server != null) {
            if (serverName != null && !serverName.isEmpty()) {
                server.setName(serverName);
            }
            if (serverType != null && !serverType.isEmpty()) {
                server.setType(serverType);
            }
            if (serverVersion != null && !serverVersion.isEmpty()) {
                server.setVersion(serverVersion);
            }
            server.setLastSeenAt(System.currentTimeMillis());
            logger.info("服务器信息更新: {} (name={}, type={}, version={})", 
                       serverId, server.getName(), server.getType(), server.getVersion());
        }
    }
    
    /**
     * 生成唯一服务器ID
     */
    private String generateServerId(String serverType) {
        String prefix = serverType != null ? serverType.toLowerCase() : "server";
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + uuid;
    }
    
    /**
     * 生成基于API Key的唯一服务器ID
     * 确保相同API Key的不同服务器获得不同ID
     */
    private String generateUniqueServerId(String apiKey, String serverType) {
        String prefix = serverType != null ? serverType.toLowerCase() : "server";
        // 使用API Key的部分内容生成唯一标识，确保相同API Key的不同服务器有不同的ID
        String apiKeyHash = Integer.toHexString(apiKey.hashCode()).substring(0, 4);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + apiKeyHash + "-" + uuid;
    }
    
    /**
     * 获取所有已注册服务器
     */
    public Map<String, RegisteredServer> getAllRegisteredServers() {
        return new ConcurrentHashMap<>(registeredServers);
    }
    
    /**
     * 注销服务器
     */
    public void unregisterServer(String serverId) {
        RegisteredServer server = registeredServers.remove(serverId);
        if (server != null) {
            // 从API Key对应的集合中移除
            Set<String> serverIds = apiKeyToServerIds.get(server.getApiKey());
            if (serverIds != null) {
                serverIds.remove(serverId);
                // 如果集合为空，移除该API Key的条目
                if (serverIds.isEmpty()) {
                    apiKeyToServerIds.remove(server.getApiKey());
                }
            }
            logger.info("服务器注销: {}", serverId);
        }
    }
    
    /**
     * 注册的服务器信息
     */
    public static class RegisteredServer {
        private String id;
        private String apiKey;
        private String name;
        private String type;
        private String version;
        private long registeredAt;
        private long lastSeenAt;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public long getRegisteredAt() { return registeredAt; }
        public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }
        
        public long getLastSeenAt() { return lastSeenAt; }
        public void setLastSeenAt(long lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    }
}
