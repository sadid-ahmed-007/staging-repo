package com.eduauth.config;

import com.eduauth.service.CustomUserDetailsService;
import com.eduauth.service.JwtService;
import com.eduauth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request.  Reads the JWT from the Authorization header,
 * validates it, checks the blacklist, then populates the SecurityContext
 * so that downstream @PreAuthorize / authorizeHttpRequests rules work.
 *
 * The app uses stateless JWT in localStorage — there are NO cookies,
 * NO sessions, and NO CSRF tokens.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlacklistService blacklistService,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtService       = jwtService;
        this.blacklistService  = blacklistService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // ── Step 1: Read Authorization header ───────────────────────────────
        String authHeader = request.getHeader("Authorization");

        // ── Step 2: Missing or not a Bearer token → skip ────────────────────
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 3: Strip "Bearer " prefix ──────────────────────────────────
        String token = authHeader.substring(7);

        // ── Step 4–9: Validate token and populate SecurityContext ────────────
        try {
            // ── Step 4: Blacklist check ──────────────────────────────────────
            if (blacklistService.isBlacklisted(token)) {
                // Token was explicitly revoked (logout) — do not authenticate
                filterChain.doFilter(request, response);
                return;
            }

            // ── Step 5: JWT validation ───────────────────────────────────────
            if (!jwtService.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // ── Step 6: Extract email from token ────────────────────────────
            String email = jwtService.extractEmail(token);

            // ── Step 7: Load UserDetails (only if not already authenticated) ─
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // ── Step 8: Build authentication token ──────────────────────
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // ── Step 9: Set in SecurityContext ───────────────────────────
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ex) {
            // Token is malformed, expired, or user not found — just skip auth.
            // Let Spring Security's authorization rules return 401 if needed.
        }

        // ── Step 10: Continue filter chain ───────────────────────────────────
        filterChain.doFilter(request, response);
    }
}
