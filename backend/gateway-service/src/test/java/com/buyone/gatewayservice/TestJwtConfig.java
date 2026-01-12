package com.buyone.gatewayservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.spec.SecretKeySpec;

@TestConfiguration
public class TestJwtConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.secret}")
    private String jwtSecret;
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256"))
                .build();
    }
}
