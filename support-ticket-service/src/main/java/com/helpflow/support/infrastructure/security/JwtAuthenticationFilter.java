package com.helpflow.support.infrastructure.security;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR = "authError";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            try {
                Claims claims = jwtService.validar(header.substring(PREFIX.length()));
                Role rol = Role.valueOf(claims.get("rol", String.class));
                Long uid = ((Number) claims.get("uid")).longValue();
                AuthenticatedActor actor = new AuthenticatedActor(uid, claims.getSubject(), rol);

                var auth = new UsernamePasswordAuthenticationToken(
                        actor, null, List.of(new SimpleGrantedAuthority(rol.name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                // El entry point usará este motivo en el ProblemDetail
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR, "Token inválido o expirado");
            }
        }
        chain.doFilter(request, response);
    }
}
