package com.buyone.mediaservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
                // .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no auth needed)
                        .requestMatchers(HttpMethod.GET, "/media/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/media/**").permitAll()
                        // Actuator health for startup script
                        .requestMatchers("/actuator/health").permitAll()

                        // All writes require authentication (we can refine to ROLE_SELLER later)
                        .requestMatchers(HttpMethod.POST, "/media/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/media/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/media/**").authenticated()
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // Tell Spring this is a JWT resource server
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        
        return http.build();
    }
    
}
