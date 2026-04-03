package com.pcs.fabric.platform.fabric;

import com.pcs.fabric.adapter.ServerAdapter;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Minimal Fabric ServerAdapter implementation.
 *
 * NOTE: This implementation is intentionally lightweight to avoid a hard dependency
 * on Fabric API at compile time. It provides basic behavior that will be replaced
 * by a true Fabric API implementation when Fabric Loom is enabled.
 */
public class FabricServerAdapter implements ServerAdapter {

    private final ScheduledExecutorService syncExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void sendMessage(UUID playerUuid, String message) {
        // No-op placeholder: in a real Fabric environment this should send to player
        System.out.println("[PCS-Fabric] sendMessage to " + playerUuid + ": " + message);
    }

    @Override
    public void broadcast(String message) {
        // Placeholder broadcast
        System.out.println("[PCS-Fabric] broadcast: " + message);
    }

    @Override
    public boolean isOp(UUID playerUuid) {
        // Conservative default: false. Real implementation should check OP/permissions.
        return false;
    }

    @Override
    public void scheduleSync(Runnable task, long delayMillis) {
        syncExecutor.schedule(task, Math.max(0, delayMillis), TimeUnit.MILLISECONDS);
    }

    @Override
    public void runSync(Runnable task) {
        // Execute immediately in this placeholder (not on Minecraft main thread)
        task.run();
    }
}
