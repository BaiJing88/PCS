package com.pcs.fabric.adapter;

import java.util.UUID;

/**
 * Adapter interface that abstracts server/platform operations.
 *
 * The Fabric-specific implementation will call into Fabric API. The goal is to
 * keep core PCS logic platform-agnostic and allow reuse of many Spigot modules
 * by writing adapter implementations.
 */
public interface ServerAdapter {
    // Send a message to a player
    void sendMessage(UUID playerUuid, String message);

    // Broadcast message to all players
    void broadcast(String message);

    // Check if a player is OP (or has required permission)
    boolean isOp(UUID playerUuid);

    // Schedule a task to run on main thread after delay (ms)
    void scheduleSync(Runnable task, long delayMillis);

    // Execute a synchronous task immediately on main thread
    void runSync(Runnable task);
}
