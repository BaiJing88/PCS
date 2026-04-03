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

package com.pcs.central.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final PCSWebSocketHandler webSocketHandler;
    private final AdminWebSocketHandler adminWebSocketHandler;
    
    public WebSocketConfig(PCSWebSocketHandler webSocketHandler,
                          AdminWebSocketHandler adminWebSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        this.adminWebSocketHandler = adminWebSocketHandler;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 服务器通信端点
        registry.addHandler(webSocketHandler, "/ws/pcs", "/ws/server", "/ws/cli")
                .setAllowedOrigins("*");
        
        // 管理面板端点
        registry.addHandler(adminWebSocketHandler, "/ws/admin")
                .setAllowedOrigins("*");
    }
}
