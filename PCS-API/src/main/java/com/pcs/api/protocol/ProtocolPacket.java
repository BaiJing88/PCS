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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 协议数据包
 */
public class ProtocolPacket {
    
    private static final Gson GSON = new Gson();
    
    private String type;
    private String requestId;
    private long timestamp;
    private JsonObject payload;

    // Encryption fields
    private boolean encrypted = false;
    private String iv; // Base64 encoded IV for AES-GCM
    private String encryptedPayload; // Base64 encoded encrypted data

    public ProtocolPacket() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ProtocolPacket(PacketType type) {
        this();
        this.type = type.getType();
    }
    
    /**
     * 创建请求包
     */
    public static ProtocolPacket request(PacketType type, Object payload) {
        ProtocolPacket packet = new ProtocolPacket(type);
        packet.requestId = java.util.UUID.randomUUID().toString();
        packet.payload = GSON.toJsonTree(payload).getAsJsonObject();
        return packet;
    }
    
    /**
     * 创建响应包
     */
    public static ProtocolPacket response(String requestId, PacketType type, Object payload) {
        ProtocolPacket packet = new ProtocolPacket(type);
        packet.requestId = requestId;
        packet.payload = GSON.toJsonTree(payload).getAsJsonObject();
        return packet;
    }
    
    /**
     * 解析 JSON
     */
    public static ProtocolPacket fromJson(String json) {
        return GSON.fromJson(json, ProtocolPacket.class);
    }
    
    /**
     * 转换为 JSON
     */
    public String toJson() {
        return GSON.toJson(this);
    }
    
    /**
     * 获取 payload 为指定类型
     */
    public <T> T getPayload(Class<T> clazz) {
        return GSON.fromJson(payload, clazz);
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public JsonObject getPayload() { return payload; }
    public void setPayload(JsonObject payload) { this.payload = payload; }

    // Encryption getters/setters
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getEncryptedPayload() { return encryptedPayload; }
    public void setEncryptedPayload(String encryptedPayload) { this.encryptedPayload = encryptedPayload; }
}
