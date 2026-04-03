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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 * 
 * @author Bai_Jing88 (QQ: 1782307393)
 * @version 1.0.0
 * @since 2026
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtTokenFilter jwtTokenFilter;
    
    public SecurityConfig(JwtTokenFilter jwtTokenFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // 公开资源 - 静态资源和登录相关
                auth.requestMatchers(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/index.html"),
                    new AntPathRequestMatcher("/admin.html"),
                    new AntPathRequestMatcher("/static/**"),
                    new AntPathRequestMatcher("/*.html"),
                    new AntPathRequestMatcher("/*.css"),
                    new AntPathRequestMatcher("/*.js"),
                    new AntPathRequestMatcher("/*.png"),
                    new AntPathRequestMatcher("/*.ico"),
                    new AntPathRequestMatcher("/assets/**")
                ).permitAll();
                // API认证端点
                auth.requestMatchers(new AntPathRequestMatcher("/api/auth/**", "GET")).permitAll();
                auth.requestMatchers(new AntPathRequestMatcher("/api/auth/**", "POST")).permitAll();
                auth.requestMatchers(new AntPathRequestMatcher("/api/auth/**", "OPTIONS")).permitAll();
                auth.requestMatchers(new AntPathRequestMatcher("/api/public/**")).access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(true));
                auth.requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll();
                auth.requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll();
                auth.requestMatchers(new AntPathRequestMatcher("/ws/**")).permitAll();
                // 管理员API需要角色
                auth.requestMatchers(new AntPathRequestMatcher("/api/admin/**")).hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
                // 其他请求允许访问
                auth.anyRequest().permitAll();
            })
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
