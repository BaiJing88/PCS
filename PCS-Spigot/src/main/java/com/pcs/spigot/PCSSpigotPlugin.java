/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */

package com.pcs.spigot;

import com.pcs.spigot.command.CommandRedirector;
import com.pcs.spigot.command.PCSCommand;
import com.pcs.spigot.listener.GUIListener;
import com.pcs.spigot.listener.PlayerEventListener;
import com.pcs.spigot.listener.PlayerListener;
import com.pcs.spigot.manager.*;
import com.pcs.spigot.websocket.PCSWebSocketClient;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * PCS-Spigot 插件主类
 * Player Credit System - Spigot Client
 * 
 * @author PCS Team
 * @version 1.0.0
 */
public class PCSSpigotPlugin extends JavaPlugin {
    
    private static PCSSpigotPlugin instance;
    
    // 管理器
    private ConfigManager configManager;
    private GUIManager guiManager;
    private VoteManager voteManager;
    private PlayerDataManager playerDataManager;
    private MuteManager muteManager;
    private PCSWebSocketClient webSocketClient;
    private StatusReporter statusReporter;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("=================================");
        getLogger().info("PCS-Spigot 正在启动...");
        getLogger().info("版本: " + getDescription().getVersion());
        getLogger().info("=================================");
        
        try {
            // 1. 保存默认配置
            saveDefaultConfig();
            
            // 2. 初始化管理器
            initializeManagers();
            
            // 3. 注册命令
            registerCommands();
            
            // 4. 注册监听器
            registerListeners();
            
            // 5. 连接中控服务器
            connectToController();
            
            getLogger().info("=================================");
            getLogger().info("PCS-Spigot 启动成功！");
            getLogger().info("=================================");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "PCS-Spigot 启动失败!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("PCS-Spigot 正在关闭...");
        
        // 断开WebSocket连接
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.close();
        }
        
        // 保存数据
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        
        getLogger().info("PCS-Spigot 已关闭！");
    }
    
    /**
     * 初始化所有管理器
     */
    private void initializeManagers() {
        getLogger().info("正在初始化管理器...");
        
        // 配置管理器
        configManager = new ConfigManager(this);
        
        // GUI管理器
        guiManager = new GUIManager(this);
        
        // 玩家数据管理器
        playerDataManager = new PlayerDataManager(this);
        
        // 投票管理器
        voteManager = new VoteManager(this);

        // 禁言管理器
        muteManager = new MuteManager(this);

        // 状态上报管理器
        statusReporter = new StatusReporter(this);

        getLogger().info("管理器初始化完成！");
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        getLogger().info("正在注册命令...");
        
        // 主命令 /pcs
        getCommand("pcs").setExecutor(new PCSCommand(this));
        getCommand("pcs").setTabCompleter(new PCSCommand(this));
        
        // 独立命令（重定向到 /pcs 子命令）
        CommandRedirector creditCmd = new CommandRedirector(this, "credit", "pcs.credit.query");
        getCommand("credit").setExecutor(creditCmd);
        getCommand("credit").setTabCompleter(creditCmd);
        
        CommandRedirector rateCmd = new CommandRedirector(this, "rate", "pcs.rate");
        getCommand("rate").setExecutor(rateCmd);
        getCommand("rate").setTabCompleter(rateCmd);
        
        CommandRedirector voteCmd = new CommandRedirector(this, "vote", "pcs.vote.participate");
        getCommand("vote").setExecutor(voteCmd);
        getCommand("vote").setTabCompleter(voteCmd);
        
        // pcsadmin 管理员命令
        CommandRedirector adminCmd = new CommandRedirector(this, "admin", "pcs.admin");
        getCommand("pcsadmin").setExecutor(adminCmd);
        getCommand("pcsadmin").setTabCompleter(adminCmd);
        
        getLogger().info("命令注册完成！");
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getLogger().info("正在注册监听器...");
        
        // GUI点击监听
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // 玩家事件监听
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 禁言管理器监听（聊天事件）
        getServer().getPluginManager().registerEvents(muteManager, this);

        // 玩家事件监听（上报到中控）
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);

        getLogger().info("监听器注册完成！");
    }
    
    /**
     * 连接中央控制器
     */
    private void connectToController() {
        getLogger().info("正在连接中控服务器...");
        
        webSocketClient = new PCSWebSocketClient(this);
        webSocketClient.connect();
    }
    
    /**
     * 重连中控服务器
     */
    public void reconnectToController() {
        getLogger().info("正在重新连接中控服务器...");

        // 关闭旧的客户端（如果存在），并替换为新的实例再连接。
        try {
            if (webSocketClient != null) {
                try {
                    webSocketClient.close();
                } catch (Exception ignored) {}
            }

            webSocketClient = new PCSWebSocketClient(this);
            webSocketClient.connect();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "重连到中控服务器时出现错误", e);
        }
    }

    /**
     * 设置/替换当前的 WebSocketClient 实例（线程安全）
     */
    public synchronized void setWebSocketClient(PCSWebSocketClient client) {
        this.webSocketClient = client;
    }
    
    // ==================== Getter Methods ====================
    
    public static PCSSpigotPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public GUIManager getGuiManager() {
        return guiManager;
    }
    
    public VoteManager getVoteManager() {
        return voteManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public PCSWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
}
