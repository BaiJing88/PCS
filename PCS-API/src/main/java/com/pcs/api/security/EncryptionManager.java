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
import com.pcs.api.protocol.PacketType;
import com.pcs.api.protocol.ProtocolPacket;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encryption Manager
 * Handles ECDH key exchange and AES-GCM encryption for packets
 */
public class EncryptionManager {

    private static final Gson GSON = new Gson();

    // Client-side: single key pair
    private KeyPair localKeyPair;
    private SecretKey sharedSecretKey;
    private boolean encryptionEnabled = false;

    // Server-side: map of session ID to encryption keys
    private final Map<String, SessionKeys> sessionKeys = new ConcurrentHashMap<>();

    /**
     * Initialize client-side encryption
     */
    public void initializeClient() {
        localKeyPair = CryptoUtils.generateECDHKeyPair();
    }

    /**
     * Initialize server-side for a session
     */
    public void initializeSession(String sessionId) {
        KeyPair keyPair = CryptoUtils.generateECDHKeyPair();
        SessionKeys keys = new SessionKeys();
        keys.localKeyPair = keyPair;
        sessionKeys.put(sessionId, keys);
    }

    /**
     * Get client's public key (Base64 encoded)
     */
    public String getClientPublicKeyBase64() {
        if (localKeyPair == null) {
            throw new IllegalStateException("Encryption not initialized");
        }
        return CryptoUtils.base64Encode(localKeyPair.getPublic().getEncoded());
    }

    /**
     * Get server's public key for a session
     */
    public String getServerPublicKeyBase64(String sessionId) {
        SessionKeys keys = sessionKeys.get(sessionId);
        if (keys == null) {
            throw new IllegalStateException("Session not initialized: " + sessionId);
        }
        return CryptoUtils.base64Encode(keys.localKeyPair.getPublic().getEncoded());
    }

    /**
     * Client: Complete key exchange with server's public key
     */
    public void completeClientKeyExchange(String serverPublicKeyBase64) {
        if (localKeyPair == null) {
            throw new IllegalStateException("Client encryption not initialized");
        }

        byte[] serverPublicKeyBytes = CryptoUtils.base64Decode(serverPublicKeyBase64);
        PublicKey serverPublicKey = CryptoUtils.decodePublicKey(serverPublicKeyBytes);

        byte[] sharedSecret = CryptoUtils.generateSharedSecret(localKeyPair.getPrivate(), serverPublicKey);
        sharedSecretKey = CryptoUtils.deriveAESKey(sharedSecret);
        encryptionEnabled = true;
    }

    /**
     * Server: Complete key exchange with client's public key
     */
    public void completeServerKeyExchange(String sessionId, String clientPublicKeyBase64) {
        SessionKeys keys = sessionKeys.get(sessionId);
        if (keys == null) {
            throw new IllegalStateException("Session not initialized: " + sessionId);
        }

        byte[] clientPublicKeyBytes = CryptoUtils.base64Decode(clientPublicKeyBase64);
        PublicKey clientPublicKey = CryptoUtils.decodePublicKey(clientPublicKeyBytes);

        byte[] sharedSecret = CryptoUtils.generateSharedSecret(keys.localKeyPair.getPrivate(), clientPublicKey);
        keys.sharedSecretKey = CryptoUtils.deriveAESKey(sharedSecret);
        keys.encryptionEnabled = true;
    }

    /**
     * Encrypt a packet (client-side)
     */
    public ProtocolPacket encryptPacket(ProtocolPacket packet) {
        if (!encryptionEnabled || sharedSecretKey == null) {
            throw new IllegalStateException("Encryption not enabled");
        }

        return encryptPacketInternal(packet, sharedSecretKey);
    }

    /**
     * Encrypt a packet (server-side)
     */
    public ProtocolPacket encryptPacket(String sessionId, ProtocolPacket packet) {
        SessionKeys keys = sessionKeys.get(sessionId);
        if (keys == null || !keys.encryptionEnabled) {
            throw new IllegalStateException("Encryption not enabled for session: " + sessionId);
        }

        return encryptPacketInternal(packet, keys.sharedSecretKey);
    }

    private ProtocolPacket encryptPacketInternal(ProtocolPacket packet, SecretKey key) {
        // Generate random IV
        byte[] iv = CryptoUtils.generateIV();

        // Convert packet to JSON
        String packetJson = packet.toJson();
        byte[] packetBytes = packetJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Encrypt
        byte[] encrypted = CryptoUtils.encryptAES(packetBytes, key, iv);

        // Create encrypted packet wrapper
        ProtocolPacket encryptedPacket = new ProtocolPacket();
        encryptedPacket.setType(PacketType.ENCRYPTED.getType());
        encryptedPacket.setRequestId(packet.getRequestId());
        encryptedPacket.setTimestamp(System.currentTimeMillis());
        encryptedPacket.setEncrypted(true);
        encryptedPacket.setIv(CryptoUtils.base64Encode(iv));
        encryptedPacket.setEncryptedPayload(CryptoUtils.base64Encode(encrypted));

        return encryptedPacket;
    }

    /**
     * Decrypt a packet (client-side)
     */
    public ProtocolPacket decryptPacket(ProtocolPacket encryptedPacket) {
        if (sharedSecretKey == null) {
            throw new IllegalStateException("Encryption not initialized");
        }

        return decryptPacketInternal(encryptedPacket, sharedSecretKey);
    }

    /**
     * Decrypt a packet (server-side)
     */
    public ProtocolPacket decryptPacket(String sessionId, ProtocolPacket encryptedPacket) {
        SessionKeys keys = sessionKeys.get(sessionId);
        if (keys == null || keys.sharedSecretKey == null) {
            throw new IllegalStateException("Encryption not initialized for session: " + sessionId);
        }

        return decryptPacketInternal(encryptedPacket, keys.sharedSecretKey);
    }

    private ProtocolPacket decryptPacketInternal(ProtocolPacket encryptedPacket, SecretKey key) {
        byte[] iv = CryptoUtils.base64Decode(encryptedPacket.getIv());
        byte[] encryptedData = CryptoUtils.base64Decode(encryptedPacket.getEncryptedPayload());

        byte[] decrypted = CryptoUtils.decryptAES(encryptedData, key, iv);
        String packetJson = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);

        return ProtocolPacket.fromJson(packetJson);
    }

    /**
     * Check if encryption is enabled (client-side)
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * Check if encryption is enabled for a session (server-side)
     */
    public boolean isEncryptionEnabled(String sessionId) {
        SessionKeys keys = sessionKeys.get(sessionId);
        return keys != null && keys.encryptionEnabled;
    }

    /**
     * Cleanup session keys (server-side)
     */
    public void removeSession(String sessionId) {
        sessionKeys.remove(sessionId);
    }

    /**
     * Create key exchange request packet (client -> server)
     */
    public ProtocolPacket createKeyExchangeRequest() {
        if (localKeyPair == null) {
            initializeClient();
        }

        ProtocolPacket packet = new ProtocolPacket();
        packet.setType(PacketType.KEY_EXCHANGE.getType());
        packet.setRequestId(java.util.UUID.randomUUID().toString());
        packet.setTimestamp(System.currentTimeMillis());

        JsonObject payload = new JsonObject();
        payload.addProperty("publicKey", getClientPublicKeyBase64());
        payload.addProperty("step", "request");
        packet.setPayload(payload);

        return packet;
    }

    /**
     * Create key exchange response packet (server -> client)
     */
    public ProtocolPacket createKeyExchangeResponse(String sessionId, String requestId) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setType(PacketType.KEY_EXCHANGE.getType());
        packet.setRequestId(requestId);
        packet.setTimestamp(System.currentTimeMillis());

        JsonObject payload = new JsonObject();
        payload.addProperty("publicKey", getServerPublicKeyBase64(sessionId));
        payload.addProperty("step", "response");
        packet.setPayload(payload);

        return packet;
    }

    /**
     * Session keys holder
     */
    private static class SessionKeys {
        KeyPair localKeyPair;
        SecretKey sharedSecretKey;
        boolean encryptionEnabled = false;
    }
}
