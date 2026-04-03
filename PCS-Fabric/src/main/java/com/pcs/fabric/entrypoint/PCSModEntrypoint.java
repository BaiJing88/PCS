package com.pcs.fabric.entrypoint;

import com.pcs.fabric.platform.fabric.FabricServerAdapterImpl;
import com.pcs.fabric.websocket.WebSocketService;
import net.minecraft.server.MinecraftServer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Fabric entrypoint: registers lifecycle hooks and command registration.
 * Requires Fabric API at compile time.
 */
public class PCSModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        // Ensure WebSocketService exists with a placeholder adapter
        if (WebSocketService.getInstance() == null) {
            new WebSocketService(new FabricServerAdapterPlaceholder());
        }

        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.pcs.fabric.commands.PCSCommandFabric.register(dispatcher);
        });
    }

    private void onServerStarted(MinecraftServer server) {
        FabricServerAdapterImpl adapter = new FabricServerAdapterImpl(server);
        WebSocketService.getInstance().setServerAdapter(adapter);
        WebSocketService.getInstance().startIfNeeded();
    }

    private void onServerStopped(MinecraftServer server) {
        WebSocketService.getInstance().disconnect();
    }

    // Optional helper for direct registration from other code
    public static void registerServerAdapter(MinecraftServer server) {
        FabricServerAdapterImpl adapter = new FabricServerAdapterImpl(server);
        WebSocketService.getInstance().setServerAdapter(adapter);
    }
}
