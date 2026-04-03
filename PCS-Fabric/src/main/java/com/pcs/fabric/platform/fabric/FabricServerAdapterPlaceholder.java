package com.pcs.fabric.platform.fabric;

import com.pcs.fabric.adapter.ServerAdapter;

import java.util.UUID;

/**
 * No-op placeholder adapter used when server instance is not yet available.
 */
public class FabricServerAdapterPlaceholder implements ServerAdapter {

    @Override
    public void sendMessage(UUID playerUuid, String message) {
        // no-op
    }

    @Override
    public void broadcast(String message) {
        // no-op
    }

    @Override
    public boolean isOp(UUID playerUuid) {
        return false;
    }

    @Override
    public void scheduleSync(Runnable task, long delayMillis) {
        // no-op
    }

    @Override
    public void runSync(Runnable task) {
        // no-op
    }
}
