package com.eduauth.config;

import com.eduauth.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Stateless JWT-based security configuration.
 *
 * Key decisions:
 *  - CSRF is DISABLED  — the app uses JWT Bearer tokens, not cookies.
 *  - Session is STATELESS — no HttpSession is created or used.
 *  - Auth errors return JSON (not HTML redirects).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter    = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    // ── Password Encoder ─────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication Provider & Manager ────────────────────────────────────

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Security Filter Chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ── No CSRF — JWT Bearer tokens, not cookies ──────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Stateless sessions — no HttpSession created ────────────────
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorization rules ───────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Public endpoints (no token required)
                .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/verify-email",
                        "/api/auth/resend-verification"
                ).permitAll()
                .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/verify",
                        "/api/verify/**",
                        "/api/verify-link"
                ).permitAll()
                .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/verify/**"
                ).permitAll()
                // Allow all OPTIONS preflight requests (CORS)
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                // Role-restricted areas
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers("/api/university/**").hasRole("UNIVERSITY")
                .requestMatchers("/api/verifier/**").hasRole("VERIFIER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Everything else under /api/** requires authentication
                .requestMatchers("/api/**").authenticated()

                // Non-API routes (if any) are open
                .anyRequest().permitAll()
            )

            // ── Custom 401 entry point: JSON, no redirect ──────────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(unauthorizedEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
            )

            // ── JWT filter runs before Spring's UsernamePasswordAuthFilter ─
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Wire DaoAuthenticationProvider ────────────────────────────
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ── Custom JSON Auth Error Handlers ──────────────────────────────────────

    /**
     * 401 — missing or invalid JWT.
     * Returns JSON instead of a redirect to a login page.
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.core.AuthenticationException ex) -> {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        };
    }

    /**
     * 403 — authenticated but insufficient role.
     * Returns JSON instead of a redirect.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException ex) -> {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
        };
    }

    private void writeJsonError(HttpServletResponse response,
                                int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "success", false,
                "message", message
        );

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
