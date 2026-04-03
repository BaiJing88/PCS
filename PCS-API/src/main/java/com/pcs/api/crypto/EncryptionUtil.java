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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * PCS 加密工具类
 * 提供 RSA + AES 混合加密、HMAC-SHA256 签名等功能
 */
public class EncryptionUtil {
    
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int AES_KEY_SIZE = 256;
    private static final int RSA_KEY_SIZE = 2048;
    
    /**
     * 生成 RSA 密钥对
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        generator.initialize(RSA_KEY_SIZE);
        return generator.generateKeyPair();
    }
    
    /**
     * 生成 AES 密钥
     */
    public static SecretKeySpec generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(AES_ALGORITHM);
        generator.init(AES_KEY_SIZE);
        return new SecretKeySpec(generator.generateKey().getEncoded(), AES_ALGORITHM);
    }
    
    /**
     * RSA 加密
     */
    public static byte[] rsaEncrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }
    
    /**
     * RSA 解密
     */
    public static byte[] rsaDecrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }
    
    /**
     * AES 加密
     */
    public static byte[] aesEncrypt(byte[] data, SecretKeySpec key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }
    
    /**
     * AES 解密
     */
    public static byte[] aesDecrypt(byte[] data, SecretKeySpec key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }
    
    /**
     * HMAC-SHA256 签名
     */
    public static byte[] hmacSHA256(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }
    
    /**
     * SHA-256 哈希
     */
    public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }
    
    /**
     * SHA-256 哈希（字符串）
     */
    public static String sha256String(String input) throws NoSuchAlgorithmException {
        byte[] hash = sha256(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    /**
     * 生成随机 IV
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
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
     * 十六进制字符串转字节数组
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 从 Base64 编码加载公钥
     */
    public static PublicKey loadPublicKeyFromBase64(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
        return factory.generatePublic(spec);
    }
    
    /**
     * 从 Base64 编码加载私钥
     */
    public static PrivateKey loadPrivateKeyFromBase64(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
        return factory.generatePrivate(spec);
    }
    
    /**
     * 密钥转 Base64
     */
    public static String keyToBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * 生成安全令牌
     */
    public static String generateSecureToken() {
        byte[] token = new byte[32];
        new SecureRandom().nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
    
    /**
     * 验证 HMAC 签名
     */
    public static boolean verifyHmac(byte[] data, byte[] signature, byte[] key) throws Exception {
        byte[] computed = hmacSHA256(data, key);
        return MessageDigest.isEqual(computed, signature);
    }
}
