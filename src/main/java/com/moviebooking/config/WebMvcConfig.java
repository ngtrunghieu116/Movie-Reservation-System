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

    @Value("${app.upload.banner-dir:uploads/banners/}")
    private String bannerDir;

    @Value("${app.upload.product-dir:uploads/products/}")
    private String productDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path posterUploadPath = Paths.get(posterDir);
        String posterAbsolutePath = posterUploadPath.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/posters/**")
                .addResourceLocations("file:/" + posterAbsolutePath + "/");

        Path bannerUploadPath = Paths.get(bannerDir);
        String bannerAbsolutePath = bannerUploadPath.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/banners/**")
                .addResourceLocations("file:/" + bannerAbsolutePath + "/");

        Path productUploadPath = Paths.get(productDir);
        String productAbsolutePath = productUploadPath.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:/" + productAbsolutePath + "/");

        Path rootUploadPath = Paths.get("uploads");
        String rootAbsolutePath = rootUploadPath.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + rootAbsolutePath + "/");
    }
}
