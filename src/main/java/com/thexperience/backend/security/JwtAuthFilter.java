package com.thexperience.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                String userId = claims.getSubject();
                String role = String.valueOf(claims.get("role"));
                var auth = new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Token parsing failed, continue without authentication
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // First try to get from cookies
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("SESSION".equals(c.getName())) {
                    String token = c.getValue();
                    if (token != null && !token.trim().isEmpty()) {
                        System.out.println("JWT Filter: Found SESSION cookie: " + token.substring(0, Math.min(50, token.length())) + "...");
                        return token.trim();
                    }
                }
            }
        }
        
        // Then try Authorization header
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            System.out.println("JWT Filter: Found Bearer token: " + token.substring(0, Math.min(50, token.length())) + "...");
            return token;
        }
        
        System.out.println("JWT Filter: No token found in cookies or headers");
        return null;
    }
}
