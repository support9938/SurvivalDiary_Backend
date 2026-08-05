package com.survivaldiary.global.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String profileResourceLocation;

    public StaticResourceConfig(
            @Value("${app.upload.profile-directory:uploads/profile}")
            String profileDirectory) {
        String location = Path.of(profileDirectory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        this.profileResourceLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(profileResourceLocation);
    }
}
