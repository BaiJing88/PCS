package com.pcs.fabric;

import com.pcs.api.model.PCSConfig;
import com.pcs.fabric.command.PCSCommandFabric;
import com.pcs.fabric.websocket.FabricWebSocketClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * PCS Fabric Mod 主类
 * 为 Fabric 服务端提供跨服玩家信用系统支持
 * 体验与 Spigot 插件一致
 */
public class PCSFabricMod implements ModInitializer {
    
    public static final String MODID = "pcs";
    public static final String NAME = "Player Credit System";
    public static final String VERSION = "1.0.1";
    
    private static PCSFabricMod instance;
    private final Logger logger = LoggerFactory.getLogger(MODID);
    
    private Properties config;
    private File configFile;
    private FabricWebSocketClient webSocketClient;
    private PCSCommandFabric commandHandler;
    private PCSConfig pcsConfig;
    private boolean debug = false;
    private MinecraftServer server;
    
    @Override
    public void onInitialize() {
        instance = this;
        logger.info("==============================================");
        logger.info("PCS Fabric Mod 加载中 - 版本 {}", VERSION);
        logger.info("==============================================");
        
        // 加载配置
        loadConfig();
        
        // 注册命令
        commandHandler = new PCSCommandFabric(this);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            commandHandler.register(dispatcher);
        });
        
        // 注册事件
        registerEvents();
        
        // 启动时连接中控
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);
        
        logger.info("PCS Fabric Mod 加载完成!");
    }
    
    /**
     * 注册事件监听
     */
    private void registerEvents() {
        // 玩家加入事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            logger.info("玩家 {} 加入服务器", player.getName().getString());
            
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.sendPlayerJoin(player);
            }
        });
        
        // 玩家离开事件
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.getPlayer();
            logger.info("玩家 {} 离开服务器", player.getName().getString());
            
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.sendPlayerQuit(player);
            }
        });
    }
    
    /**
     * 服务器启动完成
     */
    private void onServerStarted(MinecraftServer server) {
        this.server = server;
        logger.info("服务器启动完成，连接中控服务器...");
        
        // 延迟连接（等待服务器完全启动）
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                connectToController();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 服务器关闭
     */
    private void onServerStopped(MinecraftServer server) {
        logger.info("服务器关闭，断开中控连接...");
        disconnectFromController();
    }
    
    /**
     * 连接中控服务器
     */
    private void connectToController() {
        try {
            String wsUrl = config.getProperty("wsUrl", "ws://localhost:8080/ws/pcs");
            boolean enabled = config.getProperty("enabled", "true").equalsIgnoreCase("true");
            
            if (!enabled) {
                logger.info("PCS 功能已禁用");
                return;
            }
            
            webSocketClient = new FabricWebSocketClient(this);
            webSocketClient.connect();
            
            logger.info("正在连接到中控服务器: {}", wsUrl);
        } catch (Exception e) {
            logger.error("连接中控服务器失败: {}", e.getMessage());
        }
    }
    
    /**
     * 断开中控连接
     */
    private void disconnectFromController() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
            webSocketClient = null;
        }
    }
    
    // ==================== 配置 ====================
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pcs.properties");
        config = new Properties();
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config.load(reader);
                logger.info("配置文件已加载: {}", configFile.getAbsolutePath());
            } catch (Exception e) {
                logger.error("加载配置文件失败: {}", e.getMessage());
            }
        } else {
            // 创建默认配置
            setDefaultConfig();
            saveConfig();
        }
    }
    
    /**
     * 设置默认配置
     */
    private void setDefaultConfig() {
        config.setProperty("enabled", "true");
        config.setProperty("wsUrl", "ws://localhost:8080/ws/pcs");
        config.setProperty("serverId", "fabric-server-1");
        config.setProperty("serverName", "Fabric Server");
        config.setProperty("apiKey", "");
        config.setProperty("reconnect.enabled", "true");
        config.setProperty("reconnect.interval", "30");
        config.setProperty("reconnect.maxRetries", "10");
    }
    
    /**
     * 保存配置
     */
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            config.store(writer, "PCS Configuration");
            logger.info("配置文件已保存");
        } catch (Exception e) {
            logger.error("保存配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
        logger.info("配置已重载");
    }
    
    /**
     * 获取配置值
     */
    public String getConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
    
    /**
     * 获取配置值（整数）
     */
    public int getConfigInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // ==================== Getter/Setter ====================
    
    public static PCSFabricMod getInstance() {
        return instance;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Properties getConfig() {
        return config;
    }
    
    public FabricWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
    
    public MinecraftServer getServer() {
        return server;
    }
    
    public PCSConfig getPCSConfig() {
        return pcsConfig;
    }
    
    public void setConfig(PCSConfig config) {
        this.pcsConfig = config;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
