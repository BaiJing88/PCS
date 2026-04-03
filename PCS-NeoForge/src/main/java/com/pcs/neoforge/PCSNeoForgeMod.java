package com.pcs.neoforge;

import com.pcs.api.model.PCSConfig;
import com.pcs.neoforge.command.PCSCommandNeoForge;
import com.pcs.neoforge.websocket.NeoForgeWebSocketClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.common.event.FMLClientSetupEvent;
import net.neoforged.fml.common.event.FMLCommonSetupEvent;
import net.neoforged.fml.common.event.FMLServerStartedEvent;
import net.neoforged.fml.common.event.FMLServerStoppingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * PCS NeoForge Mod 主类
 * 为 NeoForge 服务端提供跨服玩家信用系统支持
 * 体验与 Spigot 插件一致
 */
@Mod(PCSNeoForgeMod.MODID)
public class PCSNeoForgeMod {
    public static final String MODID = "pcs";
    public static final String NAME = "Player Credit System";
    public static final String VERSION = "1.0.1";
    
    private static PCSNeoForgeMod instance;
    public static final Logger LOGGER = LogManager.getLogger();
    
    private Properties config;
    private File configFile;
    private NeoForgeWebSocketClient webSocketClient;
    private PCSCommandNeoForge commandHandler;
    private PCSConfig pcsConfig;
    private boolean debug = false;
    private net.minecraft.server.MinecraftServer server;
    
    public PCSNeoForgeMod(IEventBus bus) {
        instance = this;
        LOGGER.info("==============================================");
        LOGGER.info("PCS NeoForge Mod 加载中 - 版本 {}", VERSION);
        LOGGER.info("==============================================");
        
        // 注册事件
        bus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        // 加载配置
        loadConfig();
        
        // 创建命令处理器
        commandHandler = new PCSCommandNeoForge(this);
        
        LOGGER.info("PCS NeoForge Mod 加载完成!");
    }
    
    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("PCS 通用初始化完成");
    }
    
    private void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        LOGGER.info("服务器启动完成，连接中控服务器...");
        
        // 延迟连接
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                connectToController();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void onServerStopping(FMLServerStoppingEvent event) {
        LOGGER.info("服务器关闭，断开中控连接...");
        disconnectFromController();
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        commandHandler.register(event.getDispatcher());
    }
    
    /**
     * 连接中控服务器
     */
    private void connectToController() {
        try {
            String wsUrl = config.getProperty("wsUrl", "ws://localhost:8080/ws/pcs");
            boolean enabled = config.getProperty("enabled", "true").equalsIgnoreCase("true");
            
            if (!enabled) {
                LOGGER.info("PCS 功能已禁用");
                return;
            }
            
            webSocketClient = new NeoForgeWebSocketClient(this);
            webSocketClient.connect();
            
            LOGGER.info("正在连接到中控服务器: {}", wsUrl);
        } catch (Exception e) {
            LOGGER.error("连接中控服务器失败: {}", e.getMessage());
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
    
    private void loadConfig() {
        File configDir = new File("config/pcs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        configFile = new File(configDir, "pcs.properties");
        config = new Properties();
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config.load(reader);
                LOGGER.info("配置文件已加载: {}", configFile.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("加载配置文件失败: {}", e.getMessage());
            }
        } else {
            setDefaultConfig();
            saveConfig();
        }
    }
    
    private void setDefaultConfig() {
        config.setProperty("enabled", "true");
        config.setProperty("wsUrl", "ws://localhost:8080/ws/pcs");
        config.setProperty("serverId", "neoforge-server-1");
        config.setProperty("serverName", "NeoForge Server");
        config.setProperty("apiKey", "");
        config.setProperty("reconnect.enabled", "true");
        config.setProperty("reconnect.interval", "30");
        config.setProperty("reconnect.maxRetries", "10");
    }
    
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            config.store(writer, "PCS Configuration");
            LOGGER.info("配置文件已保存");
        } catch (Exception e) {
            LOGGER.error("保存配置文件失败: {}", e.getMessage());
        }
    }
    
    public void reloadConfig() {
        loadConfig();
        LOGGER.info("配置已重载");
    }
    
    public Properties getConfig() {
        return config;
    }
    
    // ==================== Getter/Setter ====================
    
    public static PCSNeoForgeMod getInstance() {
        return instance;
    }
    
    public Logger getLogger() {
        return LOGGER;
    }
    
    public NeoForgeWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
    
    public net.minecraft.server.MinecraftServer getServer() {
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
