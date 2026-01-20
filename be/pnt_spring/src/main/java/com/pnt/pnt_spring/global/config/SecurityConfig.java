package com.pnt.pnt_spring.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .cors((cors) -> cors.disable());

        http
                .authorizeHttpRequests((req) -> req
                        .requestMatchers("/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").permitAll());

        return http.build();
    }
}
