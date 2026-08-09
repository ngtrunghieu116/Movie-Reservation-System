package com.moviebooking.crawler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "crawler")
@Getter
@Setter
public class CrawlerProperties {
    private String baseUrl;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private String acceptLanguage = "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7";
    private int timeout = 10000; // in milliseconds
    private int retryCount = 3;
    private String cron = "0 0 2 * * ?";
}
