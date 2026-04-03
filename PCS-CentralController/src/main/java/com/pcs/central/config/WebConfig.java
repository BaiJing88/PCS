/*
 * Copyright (c) 2026 Bai_Jing88
 * PCS - Player Credit System
 */
package com.pcs.central.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 首页重定向到admin
        registry.addRedirectViewController("/", "/admin/");
        
        // /admin 和 /admin/ 都指向静态页面
        registry.addRedirectViewController("/admin", "/admin/");
    }
}
