package com.moviebooking.config;

import com.moviebooking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider; // Lấy từ ApplicationConfig sang

        @Value("${app.cors.allowed-origins:http://localhost:3000}")
        private String allowedOrigins;

        private static final String[] WHITE_LIST_URL = {
                        "/api/auth/**",
                        "/api/public/**",
                        "/api/payments/vnpay/**",
                        "/uploads/**",
                        "/error"
        };



        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(req -> req
                                                .requestMatchers(WHITE_LIST_URL).permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/genres/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/movies/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/theaters/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/rooms/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/seats/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/showtimes/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/articles/**").permitAll()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tickets/*/check-in").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
                java.util.List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(java.util.stream.Collectors.toList());
                configuration.setAllowedOrigins(origins);
                configuration.setAllowedMethods(
                                java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.Arrays.asList("Authorization", "Content-Type",
                                "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));
                configuration.setExposedHeaders(java.util.List.of("Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
