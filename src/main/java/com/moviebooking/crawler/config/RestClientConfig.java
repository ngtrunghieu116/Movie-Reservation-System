package com.moviebooking.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private final CrawlerProperties crawlerProperties;

    public RestClientConfig(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Bean
    public RestClient crawlerRestClient() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(crawlerProperties.getTimeout()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", crawlerProperties.getUserAgent())
                .defaultHeader("Accept-Language", crawlerProperties.getAcceptLanguage())
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build();
    }
}

