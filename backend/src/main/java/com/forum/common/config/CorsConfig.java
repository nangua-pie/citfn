package com.forum.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS for the Vue dev server and the Nginx front end.
 *
 * <p>Allowed origins are configuration, never {@code *}, because the API
 * accepts a bearer token and must not be reachable from arbitrary sites.
 */
@Configuration
public class CorsConfig {

    /**
     * Both loopback spellings are allowed by default.
     *
     * <p>Browsers treat {@code localhost} and {@code 127.0.0.1} as different
     * origins, so allowing only one makes the other fail the preflight with a
     * bare 403 that looks nothing like a CORS problem.
     */
    @Bean
    public CorsFilter corsFilter(@Value("${forum.cors.allowed-origins:"
            + "http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        // A pattern such as http://127.0.0.1:[*] has to go through
        // setAllowedOriginPatterns; setAllowedOrigins rejects wildcards outright.
        boolean wildcard = origins.stream().anyMatch(origin -> origin.contains("*"));
        if (wildcard) {
            configuration.setAllowedOriginPatterns(origins);
        } else {
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Trace-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}
