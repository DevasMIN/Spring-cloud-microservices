package com.example.payment.config;

import com.example.payment.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        log.debug("Processing request to: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Authorization header: {}", authHeader);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No valid authorization header found");
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        log.debug("JWT token: {}", jwt);
        
        try {
            log.debug("Validating token...");
            if (jwtService.validateToken(jwt)) {
                log.debug("Token is valid");

                String username = jwtService.getUsernameFromToken(jwt);
                Long userId = jwtService.getUserIdFromToken(jwt);
                List<String> roles = jwtService.getRolesFromToken(jwt);
                
                log.debug("JWT token validated for user: {}, roles: {}", username, roles);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = new CustomUserDetails(
                            userId,
                            username,
                            "",
                            roles.stream().map(SimpleGrantedAuthority::new).toList()
                    );
                    log.debug("Created UserDetails with authorities: {}", userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    log.debug("Created authentication token with authorities: {}", authToken.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication token set in SecurityContext");
                } else {
                    log.warn("Username is null or authentication already exists in SecurityContext");
                }
            } else {
                log.warn("Token validation failed");
            }
        } catch (Exception e) {
            log.error("Error processing JWT token", e);
        }
        
        filterChain.doFilter(request, response);
    }
}
