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

package com.pcs.central.service;

import com.google.gson.Gson;
import com.pcs.api.model.PCSConfig;
import com.pcs.central.database.SystemConfigRepository;
import com.pcs.central.model.entity.SystemConfigEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfigService {

    private static final String CONFIG_KEY = "pcs_main_config";
    private static final Gson GSON = new Gson();

    private final SystemConfigRepository configRepository;
    private volatile PCSConfig cachedConfig;

    public ConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 获取配置
     */
    public PCSConfig getConfig() {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        // 从数据库获取
        Optional<SystemConfigEntity> configOpt = configRepository.findByConfigKey(CONFIG_KEY);
        if (configOpt.isPresent()) {
            String configJson = configOpt.get().getConfigValue();
            cachedConfig = GSON.fromJson(configJson, PCSConfig.class);
            return cachedConfig;
        }

        // 返回默认配置并保存
        PCSConfig defaultConfig = new PCSConfig();
        saveConfig(defaultConfig);
        return defaultConfig;
    }

    /**
     * 保存配置
     */
    public void saveConfig(PCSConfig config) {
        cachedConfig = config;

        // 保存到数据库
        String json = GSON.toJson(config);
        SystemConfigEntity entity = configRepository.findByConfigKey(CONFIG_KEY)
                .orElse(new SystemConfigEntity());
        
        entity.setConfigKey(CONFIG_KEY);
        entity.setConfigValue(json);
        entity.setDescription("PCS主配置");
        
        configRepository.save(entity);
    }
    
    /**
     * 更新配置项
     */
    public void updateConfig(java.util.function.Consumer<PCSConfig> updater) {
        PCSConfig config = getConfig();
        updater.accept(config);
        saveConfig(config);
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedConfig = null;
    }
}
