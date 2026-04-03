package com.pcs.fabric.websocket;

import com.pcs.fabric.adapter.ServerAdapter;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple singleton service to manage FabricWebSocketClient lifecycle.
 */
public class WebSocketService {

    private static final Logger logger = Logger.getLogger("PCS-Fabric-WS");
    private static WebSocketService instance;

    private FabricWebSocketClient client;
    private ServerAdapter adapter;

    public WebSocketService(ServerAdapter adapter) {
        this.adapter = adapter;
        instance = this;
    }

    public void setServerAdapter(ServerAdapter adapter) {
        this.adapter = adapter;
    }

    public void startIfNeeded() {
        // placeholder: start connection if configuration provides a URI
    }

    public static WebSocketService getInstance() {
        return instance;
    }

    public synchronized void connect(String uri) {
        try {
            if (client != null) {
                try { client.close(); } catch (Exception ignored) {}
            }
            client = new FabricWebSocketClient(new URI(uri), adapter);
            client.connect();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect WebSocket", e);
        }
    }

    public synchronized void disconnect() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error closing WebSocket", e);
        }
    }

    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    public boolean isAuthenticated() {
        return client != null && client.isAuthenticated();
    }

    public void requestPlayerData(UUID uuid) {
        if (client != null) client.requestPlayerData(uuid);
    }

    public void sendRating(UUID raterUuid, String raterName, UUID targetUuid, String targetName, int score, String comment) {
        if (client != null) client.sendRating(raterUuid, raterName, targetUuid, targetName, score, comment);
    }
}
