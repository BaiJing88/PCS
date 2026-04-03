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

package com.pcs.central.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

/**
 * 配置文件导出器
 * 首次启动时将配置文件从jar包导出到外部目录
 */
@Component
public class ConfigExporter {

    private static final Logger logger = LoggerFactory.getLogger(ConfigExporter.class);
    
    private static final String CONFIG_FILE = "application.yml";
    private static final String CONFIG_DIR = "config";
    
    @PostConstruct
    public void init() {
        exportConfigIfNeeded();
    }
    
    /**
     * 如果需要，导出配置文件
     */
    private void exportConfigIfNeeded() {
        try {
            // 创建配置目录
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                logger.info("Created config directory: {}", configDir.toAbsolutePath());
            }
            
            // 检查外部配置文件是否存在
            Path externalConfig = configDir.resolve(CONFIG_FILE);
            
            if (Files.exists(externalConfig)) {
                logger.info("External config file already exists: {}", externalConfig.toAbsolutePath());
                logger.info("Using external configuration.");
                return;
            }
            
            // 从classpath读取默认配置
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
            if (!resource.exists()) {
                logger.warn("Default config not found in classpath: {}", CONFIG_FILE);
                return;
            }
            
            // 复制到外部
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, externalConfig, StandardCopyOption.REPLACE_EXISTING);
                logger.info("========================================");
                logger.info("Configuration file exported successfully!");
                logger.info("Location: {}", externalConfig.toAbsolutePath());
                logger.info("");
                logger.info("You can edit this file to customize settings.");
                logger.info("Restart the server after making changes.");
                logger.info("========================================");
            }
            
        } catch (Exception e) {
            logger.error("Failed to export config file: {}", e.getMessage(), e);
        }
    }
}
