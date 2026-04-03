package com.pcs.forge;

import com.pcs.api.model.PCSConfig;
import com.pcs.forge.command.PCSCommandForge;
import com.pcs.forge.websocket.ForgeWebSocketClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * PCS Forge Mod 主类
 * 为 Forge 服务端提供跨服玩家信用系统支持
 * 体验与 Spigot 插件一致
 */
@Mod(PCSForgeMod.MODID)
public class PCSForgeMod {
    public static final String MODID = "pcs";
    public static final String NAME = "Player Credit System";
    public static final String VERSION = "1.0.1";
    
    public static final Logger LOGGER = LogManager.getLogger();
    
    private static PCSForgeMod instance;
    private Properties config;
    private File configFile;
    private ForgeWebSocketClient webSocketClient;
    private PCSCommandForge commandHandler;
    private PCSConfig pcsConfig;
    private boolean debug = false;
    private net.minecraft.server.MinecraftServer server;
    
    public PCSForgeMod() {
        instance = this;
        LOGGER.info("==============================================");
        LOGGER.info("PCS Forge Mod 加载中 - 版本 {}", VERSION);
        LOGGER.info("==============================================");
        
        // 注册事件
        MinecraftForge.EVENT_BUS.register(this);
        
        // 加载配置
        loadConfig();
        
        // 创建命令处理器
        commandHandler = new PCSCommandForge(this);
        
        LOGGER.info("PCS Forge Mod 加载完成!");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commandHandler.register(event.getDispatcher());
    }
    
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
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
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("服务器关闭，断开中控连接...");
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
                LOGGER.info("PCS 功能已禁用");
                return;
            }
            
            webSocketClient = new ForgeWebSocketClient(this);
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
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "pcs");
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
        config.setProperty("serverId", "forge-server-1");
        config.setProperty("serverName", "Forge Server");
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
    
    public static PCSForgeMod getInstance() {
        return instance;
    }
    
    public Logger getLogger() {
        return LOGGER;
    }
    
    public ForgeWebSocketClient getWebSocketClient() {
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
