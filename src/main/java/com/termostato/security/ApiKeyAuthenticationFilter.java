package com.termostato.security;

import com.termostato.config.ConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Key";

    private final ConfigurationService configuration;

    public ApiKeyAuthenticationFilter(ConfigurationService configuration) {
        this.configuration = configuration;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Evita un secondo errore quando il container inoltra internamente a /error.
        return "/error".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedKey = request.getHeader(HEADER_NAME);
        if (isConfiguredKey(suppliedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "ApiKey");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"API key mancante o non valida\"}");
    }

    private boolean isConfiguredKey(String suppliedKey) {
        if (suppliedKey == null || suppliedKey.isBlank()) {
            return false;
        }
        byte[] supplied = suppliedKey.getBytes(StandardCharsets.UTF_8);
        return configuration.current().apiKeys().stream()
                .anyMatch(configured -> MessageDigest.isEqual(
                        supplied, configured.getBytes(StandardCharsets.UTF_8)));
    }
}
