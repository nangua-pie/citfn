package com.forum.common.config;

import com.forum.common.security.JwtAuthenticationFilter;
import com.forum.common.security.RestSecurityErrorHandler;
import com.forum.common.util.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security.
 *
 * <p>Endpoint visibility is declared here; fine grained rules such as "only the
 * owner may edit this post" stay in the service layer because they depend on
 * runtime state.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestSecurityErrorHandler securityErrorHandler;
    private final TraceIdFilter traceIdFilter;

    /** BCrypt hashes for every stored password. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // No cookies are used, so there is no CSRF surface.
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
                .and()
                .authorizeRequests(registry -> registry
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Docs and health probes.
                        .antMatchers("/doc.html", "/webjars/**", "/swagger-resources/**",
                                "/v3/api-docs/**", "/favicon.ico", "/actuator/health/**").permitAll()
                        // Public reads.
                        .antMatchers(HttpMethod.GET,
                                "/api/categories/**",
                                "/api/posts/**",
                                "/api/comments/**",
                                "/api/tags/**",
                                "/files/**").permitAll()
                        // Authentication entry points.
                        .antMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/code",
                                "/api/auth/refresh").permitAll()
                        // WebSocket handshake: the token arrives as a query parameter.
                        .antMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return securityErrorHandler;
    }
}
