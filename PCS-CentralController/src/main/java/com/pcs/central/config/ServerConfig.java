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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 服务器配置
 * 直接读取配置文件，不受环境变量影响
 */
@Component
public class ServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    @Autowired
    private Environment env;

    private String apiKey;

    @PostConstruct
    public void init() {
        // 直接从配置读取，Spring会优先使用application.yml中的值
        // 但如果环境变量存在，仍然会覆盖
        // 这里我们通过设置默认值来确保配置有效
        
        String configuredApiKey = env.getProperty("pcs.server.api-key");
        
        // 如果环境变量设置了空字符串或无效值，使用默认值
        if (configuredApiKey == null || configuredApiKey.trim().isEmpty()) {
            this.apiKey = "a354rgas35er4gaerg354adfhjr";
            logger.warn("API Key not configured in application.yml, using default value.");
        } else {
            this.apiKey = configuredApiKey;
        }
        
        logger.info("========================================");
        logger.info("Server Configuration Loaded:");
        logger.info("API Key: {}... (length: {})", 
            apiKey.substring(0, Math.min(10, apiKey.length())), apiKey.length());
        logger.info("========================================");
    }

    public String getApiKey() {
        return apiKey;
    }
}
