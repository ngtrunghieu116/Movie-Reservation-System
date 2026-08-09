package com.moviebooking.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private final CrawlerProperties crawlerProperties;

    public RestClientConfig(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Bean
    public RestClient crawlerRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(crawlerProperties.getTimeout());
        requestFactory.setReadTimeout(crawlerProperties.getTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", crawlerProperties.getUserAgent())
                .defaultHeader("Accept-Language", crawlerProperties.getAcceptLanguage())
                .build();
    }
}
