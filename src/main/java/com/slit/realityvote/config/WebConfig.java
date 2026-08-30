package com.slit.realityvote.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Uploaded contestant photos are stored on disk under ./uploads (outside
 * the classpath, so they survive application restarts/rebuilds) rather
 * than in src/main/resources/static. This handler maps URL requests to
 * "/uploads/**" onto that folder so <img th:src="@{/uploads/...}"> works.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsPath = Paths.get("uploads").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath);
    }
}
