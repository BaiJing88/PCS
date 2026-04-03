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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

/**
 * YAML配置读取器
 * 直接读取配置文件，不受环境变量影响
 */
@Component
public class YamlConfigReader {

    private static final Logger logger = LoggerFactory.getLogger(YamlConfigReader.class);
    
    private static final String CONFIG_FILE = "application.yml";
    private static final String EXTERNAL_CONFIG_DIR = "config";
    
    private Map<String, Object> config;
    private String apiKey;
    
    @PostConstruct
    public void init() {
        loadConfig();
        extractKeys();
        
        logger.info("========================================");
        logger.info("Configuration loaded from YAML file only");
        logger.info("Environment variables are IGNORED");
        logger.info("API Key: {}...", maskKey(apiKey));
        logger.info("========================================");
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            // 首先尝试外部配置文件
            Path externalPath = Paths.get(EXTERNAL_CONFIG_DIR, CONFIG_FILE);
            Resource resource;
            
            if (Files.exists(externalPath)) {
                logger.info("Loading external config: {}", externalPath.toAbsolutePath());
                resource = new FileSystemResource(externalPath.toFile());
            } else {
                // 使用classpath中的默认配置
                logger.info("Loading default config from classpath");
                resource = new ClassPathResource(CONFIG_FILE);
            }
            
            try (InputStream is = resource.getInputStream()) {
                Yaml yaml = new Yaml();
                config = yaml.load(is);
            }
            
        } catch (Exception e) {
            logger.error("Failed to load config: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot load configuration", e);
        }
    }
    
    /**
     * 提取关键配置
     */
    private void extractKeys() {
        try {
            // 导航到 pcs -> server -> api-key
            Map<String, Object> pcs = (Map<String, Object>) config.get("pcs");
            if (pcs != null) {
                Map<String, Object> server = (Map<String, Object>) pcs.get("server");
                if (server != null) {
                    apiKey = (String) server.get("api-key");
                }
            }
            
            // 如果为空，使用默认值
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = "a354rgas35er4gaerg354adfhjr";
                logger.warn("API Key not found in config, using default");
            }
            
        } catch (Exception e) {
            logger.error("Failed to extract keys: {}", e.getMessage());
            // 使用默认值
            apiKey = "a354rgas35er4gaerg354adfhjr";
        }
    }
    
    /**
     * 隐藏密钥显示
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    
    public String getApiKey() {
        return apiKey;
    }
}
