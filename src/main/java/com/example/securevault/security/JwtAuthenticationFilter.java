package com.example.securevault.security;

import com.example.securevault.entity.User;
import com.example.securevault.repository.UserRepository;
import com.example.securevault.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String email = jwtService.extractUsername(token);

            if (email != null &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null &&
                    jwtService.validateToken(token, email)) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

                AuthUser authUser = new AuthUser(
                        user.getId(),
                        user.getEmail()
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                authUser,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                log.debug(
                        "JWT authenticated userId={} path={}",
                        user.getId(),
                        request.getRequestURI()
                );
            }

        } catch (Exception exception) {
            // Never log the raw token value
            log.warn(
                    "JWT authentication failed path={} reason={}",
                    request.getRequestURI(),
                    exception.getClass().getSimpleName()
            );
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
