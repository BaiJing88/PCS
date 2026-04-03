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

package com.pcs.central.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JwtTokenFilter.class);

    private final JwtTokenProvider tokenProvider;
    
    public JwtTokenFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        logger.debug("Checking path: {}", path);
        boolean shouldSkip = path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/admin.html") ||
               path.startsWith("/static/") ||
               path.startsWith("/h2-console/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/api/cli/login") ||  // CLI登录端点跳过JWT验证
               path.startsWith("/ws/");
        logger.debug("Should skip filter for {}: {}", path, shouldSkip);
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        String path = request.getRequestURI();
        
        logger.debug("Processing request: {} with token present: {}", path, token != null);
        
        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);
            String authority = "ROLE_" + role;

            logger.debug("Token valid - User: {}, Role: {}, Authority: {}", username, role, authority);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(authority))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.debug("Authentication set in SecurityContext with authority: {}", authority);

            // 将用户名设置为请求属性，供控制器使用
            request.setAttribute("username", username);
        } else if (token != null) {
            logger.warn("Invalid token for request: {}", path);
        }

        filterChain.doFilter(request, response);
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 也支持从 query parameter 获取（WebSocket 用）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        
        return null;
    }
}
