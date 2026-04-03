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

package com.pcs.api.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pcs.api.protocol.ProtocolPacket;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;

/**
 * 安全数据包处理器
 * 提供数据包的加密、解密和签名验证
 */
public class SecurePacket {

    private static final Gson GSON = new Gson();

    private final KeyPair keyPair;
    private SecretKey aesKey;
    private boolean encryptionEnabled = false;

    public SecurePacket() {
        // 生成 ECDH 密钥对
        this.keyPair = CryptoUtils.generateECDHKeyPair();
    }

    /**
     * 获取公钥（用于发送给对端）
     */
    public String getPublicKeyBase64() {
        return CryptoUtils.base64Encode(keyPair.getPublic().getEncoded());
    }

    /**
     * 完成对密钥交换
     */
    public void completeKeyExchange(String remotePublicKeyBase64) {
        byte[] remotePublicKeyBytes = CryptoUtils.base64Decode(remotePublicKeyBase64);
        PublicKey remotePublicKey = CryptoUtils.decodePublicKey(remotePublicKeyBytes);

        // 生成共享密钥
        byte[] sharedSecret = CryptoUtils.generateSharedSecret(keyPair.getPrivate(), remotePublicKey);

        // 派生 AES 密钥
        this.aesKey = CryptoUtils.deriveAESKey(sharedSecret);
        this.encryptionEnabled = true;
    }

    /**
     * 加密并签名数据包
     */
    public String encryptPacket(ProtocolPacket packet) {
        if (!encryptionEnabled) {
            return packet.toJson();
        }

        try {
            String json = packet.toJson();
            byte[] plaintext = json.getBytes(StandardCharsets.UTF_8);

            // 生成随机 IV
            byte[] iv = CryptoUtils.generateIV();

            // 加密数据
            byte[] ciphertext = CryptoUtils.encryptAES(plaintext, aesKey, iv);

            // 计算签名（HMAC-SHA256）
            String signature = CryptoUtils.hmacSha256(json, CryptoUtils.base64Encode(aesKey.getEncoded()));

            // 构建加密包
            JsonObject encryptedPacket = new JsonObject();
            encryptedPacket.addProperty("encrypted", true);
            encryptedPacket.addProperty("data", CryptoUtils.base64Encode(ciphertext));
            encryptedPacket.addProperty("iv", CryptoUtils.base64Encode(iv));
            encryptedPacket.addProperty("signature", signature);

            return GSON.toJson(encryptedPacket);
        } catch (Exception e) {
            throw new RuntimeException("Packet encryption failed", e);
        }
    }

    /**
     * 解密并验证数据包
     */
    public ProtocolPacket decryptPacket(String encryptedJson) {
        if (!encryptionEnabled) {
            return ProtocolPacket.fromJson(encryptedJson);
        }

        try {
            JsonObject wrapper = GSON.fromJson(encryptedJson, JsonObject.class);

            // 检查是否加密
            if (!wrapper.has("encrypted") || !wrapper.get("encrypted").getAsBoolean()) {
                return ProtocolPacket.fromJson(encryptedJson);
            }

            // 解密数据
            byte[] ciphertext = CryptoUtils.base64Decode(wrapper.get("data").getAsString());
            byte[] iv = CryptoUtils.base64Decode(wrapper.get("iv").getAsString());
            String signature = wrapper.get("signature").getAsString();

            byte[] plaintext = CryptoUtils.decryptAES(ciphertext, aesKey, iv);
            String json = new String(plaintext, StandardCharsets.UTF_8);

            // 验证签名
            String computedSignature = CryptoUtils.hmacSha256(json, CryptoUtils.base64Encode(aesKey.getEncoded()));
            if (!computedSignature.equals(signature)) {
                throw new SecurityException("Packet signature verification failed");
            }

            return ProtocolPacket.fromJson(json);
        } catch (Exception e) {
            throw new RuntimeException("Packet decryption failed", e);
        }
    }

    /**
     * 检查是否启用了加密
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * 禁用加密（用于调试）
     */
    public void disableEncryption() {
        this.encryptionEnabled = false;
    }
}
