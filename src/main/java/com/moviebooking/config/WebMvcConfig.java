package com.moviebooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.poster-dir:uploads/posters/}")
    private String posterDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path posterUploadPath = Paths.get(posterDir);
        String posterAbsolutePath = posterUploadPath.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/posters/**")
                .addResourceLocations("file:/" + posterAbsolutePath + "/");
    }
}
