package com.rubypaper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir; // 예: C:/Users/username/my-app-uploads/images/

    @Value("${file.upload-url}")
    private String uploadUrl; // 예: /images/upload/

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(uploadUrl + "**") // 예: /images/upload/**
                .addResourceLocations("file:" + uploadDir); // ★ file: 접두사 + 동적 경로 ★
        
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}