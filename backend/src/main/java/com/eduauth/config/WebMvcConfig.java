package com.eduauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.storage.dir:storage}")
    private String storageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path storagePath = Paths.get(storageDir).toAbsolutePath().normalize();
        String storageLocation = storagePath.toUri().toString();

        // Map /uploads/** to the storage directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(storageLocation);
    }
}
