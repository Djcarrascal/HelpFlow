package com.helpflow.support.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.support.infrastructure.error.ProblemDetails;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityProblemHandler.class);

    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    public SecurityProblemHandler(ObjectMapper objectMapper, MeterRegistry registry) {
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    // 401: sin token (no-autenticado), token vencido (token-expirado) o token falso (token-invalido)
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        Object code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE);
        Object motivo = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR);
        String tipo = code != null ? code.toString() : "no-autenticado";
        String detalle = motivo != null ? motivo.toString() : "Se requiere un token Bearer";
        registry.counter("helpflow.auth.fallos", "motivo", tipo).increment();
        log.warn("autenticación fallida en {}: {}", request.getRequestURI(), detalle);
        escribir(response, ProblemDetails.of(HttpStatus.UNAUTHORIZED, tipo,
                "No autenticado", detalle, request.getRequestURI()));
    }

    // 403: rol sin permiso para la ruta
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        registry.counter("helpflow.auth.accesos.denegados").increment();
        log.warn("acceso denegado a {}", request.getRequestURI());
        escribir(response, ProblemDetails.of(HttpStatus.FORBIDDEN, "acceso-denegado",
                "Acceso denegado", "Tu rol no tiene permiso para esta operación", request.getRequestURI()));
    }

    private void escribir(HttpServletResponse response, ProblemDetail pd) throws IOException {
        response.setStatus(pd.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), pd);
    }
}
