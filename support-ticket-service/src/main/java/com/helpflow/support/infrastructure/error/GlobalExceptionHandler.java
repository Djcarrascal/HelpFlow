package com.helpflow.support.infrastructure.error;

import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.DuplicateResourceException;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.exception.InvalidCredentialsException;
import com.helpflow.support.domain.exception.InvalidStatusTransitionException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MeterRegistry registry;

    public GlobalExceptionHandler(MeterRegistry registry) {
        this.registry = registry;
    }

    // Bean Validation: lista de errores por campo en la propiedad "errores"
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.put(fe.getField(), fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(ge -> errores.put(ge.getObjectName(), ge.getDefaultMessage()));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacion", "Datos inválidos",
                "La petición tiene " + errores.size() + " error(es) de validación", req);
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return problema(HttpStatus.BAD_REQUEST, "json-invalido", "JSON inválido",
                "El cuerpo no se pudo leer: revisa el formato y los valores permitidos de los enums", req);
    }

    // Parámetro de ruta con tipo incorrecto, por ejemplo /api/tickets/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return problema(HttpStatus.BAD_REQUEST, "parametro-invalido", "Parámetro inválido",
                "El valor '" + ex.getValue() + "' no es válido para '" + ex.getName() + "'", req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail credenciales(InvalidCredentialsException ex, HttpServletRequest req) {
        registry.counter("helpflow.auth.fallos", "motivo", "credenciales").increment();
        return problema(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", "Credenciales inválidas", ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail prohibido(ForbiddenOperationException ex, HttpServletRequest req) {
        registry.counter("helpflow.auth.accesos.denegados").increment();
        return problema(HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado", ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail noEncontrado(ResourceNotFoundException ex, HttpServletRequest req) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado", "Recurso no encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail duplicado(DuplicateResourceException ex, HttpServletRequest req) {
        return problema(HttpStatus.CONFLICT, "conflicto", "Recurso duplicado", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail transicion(InvalidStatusTransitionException ex, HttpServletRequest req) {
        return problema(HttpStatus.CONFLICT, "transicion-invalida", "Transición de estado inválida", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail reglaNegocio(BusinessRuleException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-negocio", "Regla de negocio incumplida", ex.getMessage(), req);
    }

    // Errores JPA personalizados: no se expone el SQL al cliente
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "conflicto", "Conflicto de datos",
                "La operación viola una restricción de datos (valor duplicado o referencia inexistente)", req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail baseDatos(DataAccessException ex, HttpServletRequest req) {
        log.error("error de acceso a datos", ex);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "base-datos", "Error de base de datos",
                "No se pudo completar la operación en la base de datos", req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail generico(Exception ex, HttpServletRequest req) {
        // Errores HTTP propios de Spring (ruta inexistente, método no permitido...) conservan su código
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return problema(status, "http-" + status.value(), status.getReasonPhrase(), ex.getMessage(), req);
        }
        log.error("error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
                "Ocurrió un error inesperado. Usa el traceId para rastrearlo", req);
    }

    // Log de nivel WARN para errores del cliente (4xx)
    private ProblemDetail problema(HttpStatus status, String code, String title, String detail, HttpServletRequest req) {
        if (status.is4xxClientError()) {
            log.warn("{} {} -> {} {}", req.getMethod(), req.getRequestURI(), status.value(), detail);
        }
        return ProblemDetails.of(status, code, title, detail, req.getRequestURI());
    }
}
