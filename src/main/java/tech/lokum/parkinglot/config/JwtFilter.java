package tech.lokum.parkinglot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.lokum.parkinglot.service.JwtService;
import tech.lokum.parkinglot.service.JwtStoreService;
import tech.lokum.parkinglot.service.MyUserDetailService;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    JwtService jwtService;

    @Autowired
    ApplicationContext context;

    @Autowired
    JwtStoreService jwtStoreService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String userName = null;

        // 1. Try Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 2. Try accessToken / jwt cookie
            Cookie accessCookie = getCookie(request, "accessToken");
            if (accessCookie == null) {
                accessCookie = getCookie(request, "jwt");
            }
            if (accessCookie != null) {
                token = accessCookie.getValue();
            }
        }

        // Validate access token
        if (token != null && !jwtService.isTokenExpired(token)) {
            try {
                userName = jwtService.extractUserName(token);
            } catch (Exception e) {
                userName = null;
            }
        }

        // 3. If access token is missing or expired, attempt refresh using refreshToken cookie
        if (userName == null) {
            Cookie refreshCookie = getCookie(request, "refreshToken");
            if (refreshCookie != null) {
                String refreshToken = refreshCookie.getValue();
                if (refreshToken != null && !jwtService.isTokenExpired(refreshToken)
                        && "refresh".equals(jwtService.getTokenType(refreshToken))) {
                    try {
                        String refUserName = jwtService.extractUserName(refreshToken);
                        if (refUserName != null) {
                            // 1. Generate new access token
                            String newAccessToken = jwtService.generateAccessToken(refUserName);
                            // 2. Generate new refresh token (rotation)
                            String newRefreshToken = jwtService.generateRefreshToken(refUserName);
                            // 3. Store new refresh token in DB with expiry
                            jwtStoreService.storeRefreshToken(refUserName, newRefreshToken);
                            // 4. Update cookies
                            response.addCookie(createCookie("accessToken", newAccessToken, 15 * 60));
                            response.addCookie(createCookie("refreshToken", newRefreshToken, 7 * 24 * 60 * 60));
                            token = newAccessToken;
                            userName = refUserName;
                        }
                    } catch (Exception e) {
                        userName = null;
                    }
                }
            }
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = context.getBean(MyUserDetailService.class).loadUserByUsername(userName);

            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private Cookie getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .orElse(null);
    }

    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }
}
