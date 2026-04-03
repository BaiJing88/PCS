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

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密工具类
 * 提供 SHA256、AES-GCM 加密、ECDH 密钥交换
 */
public class CryptoUtils {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * SHA256 哈希
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA256 failed", e);
        }
    }

    /**
     * SHA256 哈希（带密钥，用于 HMAC）
     */
    public static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 failed", e);
        }
    }

    /**
     * 生成 ECDH 密钥对
     */
    public static KeyPair generateECDHKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("ECDH key generation failed", e);
        }
    }

    /**
     * 生成共享密钥
     */
    public static byte[] generateSharedSecret(PrivateKey privateKey, PublicKey publicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            return keyAgreement.generateSecret();
        } catch (Exception e) {
            throw new RuntimeException("Shared secret generation failed", e);
        }
    }

    /**
     * 从字节数组恢复公钥
     */
    public static PublicKey decodePublicKey(byte[] encodedKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Public key decoding failed", e);
        }
    }

    /**
     * AES-GCM 加密
     */
    public static byte[] encryptAES(byte[] plaintext, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * AES-GCM 解密
     */
    public static byte[] decryptAES(byte[] ciphertext, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    /**
     * 生成随机 IV
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 生成 AES 密钥（从共享密钥派生）
     */
    public static SecretKey deriveAESKey(byte[] sharedSecret) {
        // 使用 SHA-256 哈希共享密钥，取前 16 字节作为 AES-128 密钥
        byte[] keyBytes = sha256Bytes(new String(sharedSecret, StandardCharsets.UTF_8));
        byte[] aesKeyBytes = new byte[16];
        System.arraycopy(keyBytes, 0, aesKeyBytes, 0, 16);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * SHA256 返回字节数组
     */
    private static byte[] sha256Bytes(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA256 failed", e);
        }
    }

    /**
     * Base64 编码
     */
    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 解码
     */
    public static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
    }
}
