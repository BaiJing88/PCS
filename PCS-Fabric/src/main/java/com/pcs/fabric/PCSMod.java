package com.pcs.fabric;

/**
 * PCS Fabric scaffold main class.
 *
 * Note: this class is intentionally lightweight and does not depend on Fabric API classes
 * so the repository can compile the module without Fabric Loom. Later we will add a Fabric
 * specific implementation that wires this scaffold to the Fabric lifecycle.
 */
public class PCSMod {

    private static PCSMod instance;

    public PCSMod() {
        instance = this;
    }

    public static PCSMod getInstance() {
        return instance;
    }

    /**
     * Startup hook (to be called from Fabric entrypoint once Loom is added).
     */
    public void onInitialize() {
        // TODO: implement initialization: create websocket client, managers, register commands/events
    }
}
