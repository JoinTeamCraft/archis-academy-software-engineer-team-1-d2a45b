package tech.lokum.parkinglot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that intercepts incoming HTTP requests to validate JWT bearer tokens and populate SecurityContext.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<JwtService> jwtServiceProvider;
    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;

    public JwtAuthenticationFilter(
            ObjectProvider<JwtService> jwtServiceProvider,
            ObjectProvider<UserDetailsService> userDetailsServiceProvider
    ) {
        this.jwtServiceProvider = jwtServiceProvider;
        this.userDetailsServiceProvider = userDetailsServiceProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtService jwtService = jwtServiceProvider.getIfAvailable();
        if (jwtService == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetailsService uds = userDetailsServiceProvider.getIfAvailable();
                List<GrantedAuthority> tokenAuthorities = jwtService.extractAuthorities(jwt);

                boolean authenticated = false;
                if (uds != null) {
                    try {
                        UserDetails userDetails = uds.loadUserByUsername(userEmail);
                        if (userDetails != null && jwtService.validateToken(jwt, userDetails)) {
                            var authorities = !tokenAuthorities.isEmpty() ? tokenAuthorities : userDetails.getAuthorities();
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    authorities
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            authenticated = true;
                        }
                    } catch (Exception ignored) {
                        // Fall back to validating directly from token claims
                    }
                }

                if (!authenticated && jwtService.validateToken(jwt)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            tokenAuthorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Invalid / expired token will result in empty SecurityContext
        }

        filterChain.doFilter(request, response);
    }
}