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

package com.pcs.api.crypto;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * 安全数据包封装
 * 使用 AES 加密数据，RSA 加密 AES 密钥
 */
public class SecurePacket {
    
    private static final Gson GSON = new Gson();
    
    private String packetId;
    private String timestamp;
    private String serverId;
    private String encryptedData;
    private String encryptedKey;
    private String iv;
    private String signature;
    private String packetType;
    
    public SecurePacket() {
        this.packetId = UUID.randomUUID().toString();
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }
    
    /**
     * 创建加密数据包
     */
    public static SecurePacket create(String serverId, Object data, String serverPublicKey, 
                                       SecretKeySpec aesKey, byte[] sharedSecret) throws Exception {
        SecurePacket packet = new SecurePacket();
        packet.serverId = serverId;
        packet.packetType = data.getClass().getSimpleName();
        
        // 序列化数据
        String jsonData = GSON.toJson(data);
        byte[] dataBytes = jsonData.getBytes(StandardCharsets.UTF_8);
        
        // 生成 IV
        byte[] ivBytes = EncryptionUtil.generateIV();
        packet.iv = Base64.getEncoder().encodeToString(ivBytes);
        
        // AES 加密数据
        byte[] encryptedBytes = EncryptionUtil.aesEncrypt(dataBytes, aesKey, ivBytes);
        packet.encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
        
        // RSA 加密 AES 密钥
        java.security.PublicKey rsaKey = EncryptionUtil.loadPublicKeyFromBase64(serverPublicKey);
        byte[] encryptedKeyBytes = EncryptionUtil.rsaEncrypt(aesKey.getEncoded(), rsaKey);
        packet.encryptedKey = Base64.getEncoder().encodeToString(encryptedKeyBytes);
        
        // 生成签名 (HMAC-SHA256)
        String signContent = packet.packetId + packet.timestamp + packet.serverId + packet.encryptedData;
        byte[] signatureBytes = EncryptionUtil.hmacSHA256(signContent.getBytes(StandardCharsets.UTF_8), sharedSecret);
        packet.signature = Base64.getEncoder().encodeToString(signatureBytes);
        
        return packet;
    }
    
    /**
     * 解密数据包
     */
    public <T> T decrypt(String privateKeyBase64, byte[] sharedSecret, Class<T> clazz) throws Exception {
        // 验证签名
        String signContent = packetId + timestamp + serverId + encryptedData;
        byte[] computedSig = EncryptionUtil.hmacSHA256(signContent.getBytes(StandardCharsets.UTF_8), sharedSecret);
        byte[] providedSig = Base64.getDecoder().decode(signature);
        
        if (!java.security.MessageDigest.isEqual(computedSig, providedSig)) {
            throw new SecurityException("Packet signature verification failed");
        }
        
        // 检查时间戳（防止重放攻击）
        long packetTime = Long.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - packetTime) > 300000) { // 5分钟有效期
            throw new SecurityException("Packet expired or timestamp invalid");
        }
        
        // RSA 解密 AES 密钥
        java.security.PrivateKey privateKey = EncryptionUtil.loadPrivateKeyFromBase64(privateKeyBase64);
        byte[] aesKeyBytes = EncryptionUtil.rsaDecrypt(Base64.getDecoder().decode(encryptedKey), privateKey);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        
        // AES 解密数据
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        byte[] decryptedBytes = EncryptionUtil.aesDecrypt(Base64.getDecoder().decode(encryptedData), aesKey, ivBytes);
        
        String jsonData = new String(decryptedBytes, StandardCharsets.UTF_8);
        return GSON.fromJson(jsonData, clazz);
    }
    
    /**
     * 验证数据包完整性（不解密）
     */
    public boolean verifyIntegrity(byte[] sharedSecret) throws Exception {
        try {
            String signContent = packetId + timestamp + serverId + encryptedData;
            byte[] computedSig = EncryptionUtil.hmacSHA256(signContent.getBytes(StandardCharsets.UTF_8), sharedSecret);
            byte[] providedSig = Base64.getDecoder().decode(signature);
            return java.security.MessageDigest.isEqual(computedSig, providedSig);
        } catch (Exception e) {
            return false;
        }
    }
    
    // Getters and Setters
    public String getPacketId() { return packetId; }
    public void setPacketId(String packetId) { this.packetId = packetId; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
    
    public String getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(String encryptedKey) { this.encryptedKey = encryptedKey; }
    
    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    public String getPacketType() { return packetType; }
    public void setPacketType(String packetType) { this.packetType = packetType; }
}
