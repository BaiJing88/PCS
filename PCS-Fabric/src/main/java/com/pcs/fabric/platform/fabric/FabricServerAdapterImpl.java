package com.pcs.fabric.platform.fabric;

import com.pcs.api.model.PlayerCredit;
import com.pcs.fabric.adapter.ServerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * A conservative implementation that only uses Minecraft server classes (mappings)
 * and avoids direct Fabric API compile-time references. This keeps the module
 * buildable without fabric-api available in repositories.
 */
public class FabricServerAdapterImpl implements ServerAdapter {

    private final MinecraftServer server;

    public FabricServerAdapterImpl(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendMessage(UUID playerUuid, String message) {
        Text text = Text.literal(message);
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(playerUuid);
        if (p != null) {
            p.sendMessage(text, false);
        }
    }

    @Override
    public void broadcast(String message) {
        Text text = Text.literal(message);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(text, false);
        }
    }

    @Override
    public boolean isOp(UUID playerUuid) {
        // Best-effort: return true if player is online and has operator flag via player manager.
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(playerUuid);
        if (p == null) return false;
        try {
            return server.getPlayerManager().isOperator(p.getGameProfile());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void scheduleSync(Runnable task, long delayMillis) {
        // crude scheduling: spawn a thread to sleep then execute on server thread
        new Thread(() -> {
            try { Thread.sleep(delayMillis); } catch (InterruptedException ignored) {}
            server.execute(task);
        }).start();
    }

    @Override
    public void runSync(Runnable task) {
        server.execute(task);
    }
}
