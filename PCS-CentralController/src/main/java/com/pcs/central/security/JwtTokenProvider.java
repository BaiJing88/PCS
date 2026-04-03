/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Central Controller
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

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 提供者
 * 
 * @author Bai_Jing88 (QQ: 1782307393)
 * @version 1.0.0
 * @since 2026
 */
@Component
public class JwtTokenProvider {
    
    private final SecretKey jwtSecret;
    private final long jwtExpiration;
    
    public JwtTokenProvider(
            @Value("${pcs.security.jwt-secret:}") String secret,
            @Value("${pcs.security.token-expiry:24}") long expiryHours) {
        if (secret == null || secret.isEmpty()) {
            this.jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        } else {
            this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.jwtExpiration = expiryHours * 60 * 60 * 1000;
    }
    
    /**
     * 生成访问令牌
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 从令牌获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * 从令牌获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDate(String token) {
        return parseToken(token).getExpiration();
    }
}
