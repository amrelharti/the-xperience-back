package com.thexperience.backend.config;

import com.thexperience.backend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/api/public/**", "/api/health").permitAll()
                        .requestMatchers("/api/auth/google", "/api/auth/email", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/score-cv/extract-text").authenticated() // PDF extraction requires auth (for chat feature)
                        .requestMatchers("/api/score-cv").permitAll() // CV scorer is publicly accessible
                        .requestMatchers(HttpMethod.POST, "/api/cv-scorer/submissions").permitAll() // CV scorer submissions POST is publicly accessible
                        .requestMatchers(HttpMethod.GET, "/api/cv-scorer/submissions").authenticated() // CV scorer submissions GET requires authentication (for admin)
                        .requestMatchers(HttpMethod.POST, "/api/landing-page/leads").permitAll() // Landing page leads POST is publicly accessible
                        .requestMatchers(HttpMethod.GET, "/api/landing-page/leads").hasRole("ADMIN") // Landing page leads GET requires admin role
                        .requestMatchers(HttpMethod.GET, "/api/landing-page/remaining-spots").permitAll() // Public access to remaining spots (for landing page)
                        .requestMatchers(HttpMethod.GET, "/api/admin/landing-page/settings/remaining-spots").hasRole("ADMIN") // Admin GET requires admin role
                        .requestMatchers(HttpMethod.PUT, "/api/admin/landing-page/settings/remaining-spots").hasRole("ADMIN") // Admin only for updating
                        .requestMatchers(HttpMethod.GET, "/api/roulette/daily-status").permitAll() // Public access to roulette daily status
                        .requestMatchers(HttpMethod.POST, "/api/roulette/spin").permitAll() // Public access to spin roulette
                        .requestMatchers(HttpMethod.POST, "/api/roulette/update-winner").permitAll() // Public access to update winner name
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "https://app.the-xperience.com",
                "https://api.the-xperience.com",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}