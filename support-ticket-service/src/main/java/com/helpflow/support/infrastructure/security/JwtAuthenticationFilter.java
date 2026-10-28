package com.helpflow.support.infrastructure.security;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

    // Atributos que el entry point usa para armar el ProblemDetail del 401
    public static final String AUTH_ERROR = "authError";
    public static final String AUTH_ERROR_CODE = "authErrorCode";
    public static final String CODE_EXPIRADO = "token-expirado";
    public static final String CODE_INVALIDO = "token-invalido";
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
            } catch (ExpiredJwtException e) {
                // Token bien firmado pero vencido: el cliente debe volver a iniciar sesión
                rechazar(request, CODE_EXPIRADO, "El token expiró; inicia sesión de nuevo");
            } catch (JwtException | IllegalArgumentException e) {
                // Firma incorrecta, token mal formado o claims inesperados
                rechazar(request, CODE_INVALIDO, "Token inválido");
            }
        }
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletRequest request, String code, String detalle) {
        SecurityContextHolder.clearContext();
        request.setAttribute(AUTH_ERROR_CODE, code);
        request.setAttribute(AUTH_ERROR, detalle);
    }
}
