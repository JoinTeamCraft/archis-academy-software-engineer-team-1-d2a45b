package tech.lokum.parkinglot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tech.lokum.parkinglot.config.CorsProperties;
import tech.lokum.parkinglot.dto.ErrorResponse;

/**
 * Spring Security configuration enabling Role-Based Access Control (RBAC) and stateless JWT authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            ObjectProvider<UserDetailsService> userDetailsServiceProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) {
        this.userDetailsServiceProvider = userDetailsServiceProvider;
        ObjectMapper mapper = objectMapperProvider.getIfAvailable();
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
        }
        this.objectMapper = mapper;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            ObjectProvider<JwtService> jwtServiceProvider,
            ObjectProvider<UserDetailsService> userDetailsServiceProvider
    ) {
        return new JwtAuthenticationFilter(jwtServiceProvider, userDetailsServiceProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsProperties corsProperties
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new org.springframework.security.web.context.RequestAttributeSecurityContextRepository()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints: Auth, API documentation, Actuator, Health, Error testing
                        .requestMatchers(
                                "/api/auth/**",
                                "/api-docs/**",
                                "/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/docs/**",
                                "/docs",
                                "/actuator/**",
                                "/api/status",
                                "/test/**"
                        ).permitAll()
                        // Public payment notification webhook from external gateways
                        .requestMatchers(HttpMethod.POST, "/api/payments/notifications", "/api/payments/webhook").permitAll()

                        // Role-based security rules
                        // Only admins can delete users
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        // Admins and managers can view users
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "MANAGER")
                        // Admin-specific endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        // Reports endpoints: accessible only by authorized roles (ADMIN, MANAGER, OPERATOR)
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                        // Data export endpoints: accessible only by authorized roles (ADMIN, MANAGER, OPERATOR)
                        .requestMatchers("/api/data/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                        // Permit other existing endpoints for backwards compatibility with previous tickets
                        .anyRequest().permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = ErrorResponse.of(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    authException.getMessage() != null ? authException.getMessage() : "Authentication required",
                    request.getRequestURI()
            );
            objectMapper.writeValue(response.getOutputStream(), error);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = ErrorResponse.of(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden",
                    "Access is denied: insufficient role privileges",
                    request.getRequestURI()
            );
            objectMapper.writeValue(response.getOutputStream(), error);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        UserDetailsService uds = userDetailsServiceProvider.getIfAvailable();
        if (uds != null) {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(uds);
            authProvider.setPasswordEncoder(passwordEncoder());
            return authProvider;
        }
        return new AuthenticationProvider() {
            @Override
            public org.springframework.security.core.Authentication authenticate(org.springframework.security.core.Authentication authentication) {
                return authentication;
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsProperties.toCorsConfiguration());
        return source;
    }
}
