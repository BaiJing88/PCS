package com.pcs.fabric.websocket;

import java.util.concurrent.TimeUnit;

/**
 * Fabric 版重连管理器
 */
public class FabricReconnectManager {
    
    private final com.pcs.fabric.PCSFabricMod mod;
    private int attempts = 0;
    private final int maxAttempts;
    private final long baseDelay;
    
    public FabricReconnectManager(com.pcs.fabric.PCSFabricMod mod) {
        this.mod = mod;
        this.maxAttempts = mod.getConfig().getInt("reconnect.maxRetries", 10);
        this.baseDelay = mod.getConfig().getInt("reconnect.interval", 30);
    }
    
    public void resetAttempts() {
        attempts = 0;
    }
    
    public void recordFailedReconnect() {
        attempts++;
    }
    
    public long getNextReconnectDelay() {
        if (attempts >= maxAttempts && maxAttempts > 0) {
            mod.getLogger().info("已达到最大重连次数，停止重连");
            return -1;
        }
        
        // 指数退避
        long delay = baseDelay * (long) Math.pow(2, attempts - 1);
        return Math.min(delay, 300); // 最大 5 分钟
    }
    
    public int getAttempts() {
        return attempts;
    }
}
