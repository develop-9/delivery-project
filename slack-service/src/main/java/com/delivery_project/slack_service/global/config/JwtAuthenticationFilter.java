package com.delivery_project.slack_service.global.config;

import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.security.JwtPrincipal;
import com.delivery_project.slack_service.global.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            authenticate(authorizationHeader);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String authorizationHeader) {
        try {
            String token = jwtProvider.resolveToken(authorizationHeader);
            JwtPrincipal principal = jwtProvider.parse(token);

            var authorities = principal.role() == null
                    ? List.<SimpleGrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority(ROLE_PREFIX + principal.role().name()));

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal.userId(), null, authorities)
            );

        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
        }
    }
}
