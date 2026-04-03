package com.pcs.neoforge.websocket;

import java.util.concurrent.TimeUnit;

/**
 * NeoForge 版重连管理器
 */
public class NeoForgeReconnectManager {
    
    private final com.pcs.neoforge.PCSNeoForgeMod mod;
    private int attempts = 0;
    private int maxAttempts;
    private long baseDelay;
    
    public NeoForgeReconnectManager(com.pcs.neoforge.PCSNeoForgeMod mod) {
        this.mod = mod;
        this.maxAttempts = Integer.parseInt(mod.getConfig().getOrDefault("reconnect.maxRetries", "10"));
        this.baseDelay = Long.parseLong(mod.getConfig().getOrDefault("reconnect.interval", "30"));
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
        
        long delay = baseDelay * (long) Math.pow(2, attempts - 1);
        return Math.min(delay, 300);
    }
    
    public int getAttempts() {
        return attempts;
    }
}
