package com.collaborative_code_editor.config;

import com.collaborative_code_editor.model.User;
import com.collaborative_code_editor.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.info("No valid Bearer token found in Authorization header");
            System.out.println("jwtauthfilter: No valid Bearer token found in Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            logger.debug("JWT Claims: {}", claims);

            // Extract githubLogin and accessToken
            String githubLogin = claims.get("githubLogin", String.class);
            String accessToken = claims.get("accessToken", String.class);

            if (githubLogin == null) {
                logger.error("githubLogin is null in JWT claims");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid JWT: githubLogin is missing");
                return;
            }

            // Fetch user details from MongoDB
            User user = userRepository.findByGithubLogin(githubLogin)
                    .orElseThrow(() -> {
                        logger.error("User not found in MongoDB for githubLogin: {}", githubLogin);
                        return new RuntimeException("User not found for githubLogin: " + githubLogin);
                    });


            logger.info("Authenticated User: {}", user);

            // Set access token in request
            request.setAttribute("githubAccessToken", accessToken);

            // Set authentication in SecurityContext
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (JwtException e) {
            logger.error("Invalid or expired JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        } catch (RuntimeException e) {
            logger.error("Authentication error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
}