package com.helpflow.support.infrastructure.error;

import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.DuplicateResourceException;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.exception.InvalidCredentialsException;
import com.helpflow.support.domain.exception.InvalidStatusTransitionException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.TicketStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

// Cada excepción termina en su código HTTP y en el mismo formato ProblemDetail
class GlobalExceptionHandlerTest {

    private SimpleMeterRegistry registry;
    private GlobalExceptionHandler handler;
    private MockHttpServletRequest req;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        handler = new GlobalExceptionHandler(registry);
        req = new MockHttpServletRequest("PATCH", "/api/tickets/1/estado");
        MDC.put(TraceIdFilter.TRACE_ID, "trace-de-prueba");
    }

    @AfterEach
    void limpiarMdc() {
        MDC.remove(TraceIdFilter.TRACE_ID);
    }

    private void assertProblema(ProblemDetail pd, HttpStatus status, String code) {
        assertThat(pd.getStatus()).isEqualTo(status.value());
        assertThat(pd.getType().toString()).isEqualTo("https://api.helpflow.com/errors/" + code);
        assertThat(pd.getInstance().toString()).isEqualTo("/api/tickets/1/estado");
        assertThat(pd.getProperties()).containsEntry("traceId", "trace-de-prueba").containsKey("timestamp");
    }

    @Test
    void erroresDeNegocio() {
        assertProblema(handler.noEncontrado(new ResourceNotFoundException("Ticket", 9), req), HttpStatus.NOT_FOUND, "no-encontrado");
        assertProblema(handler.duplicado(new DuplicateResourceException("dup"), req), HttpStatus.CONFLICT, "conflicto");
        assertProblema(handler.transicion(new InvalidStatusTransitionException(TicketStatus.CERRADO, TicketStatus.ABIERTO), req),
                HttpStatus.CONFLICT, "transicion-invalida");
        assertProblema(handler.reglaNegocio(new BusinessRuleException("inactivo"), req),
                HttpStatus.UNPROCESSABLE_ENTITY, "regla-negocio");
    }

    @Test
    void credencialesYAccesoDenegadoSeCuentanEnMetricas() {
        assertProblema(handler.credenciales(new InvalidCredentialsException(), req), HttpStatus.UNAUTHORIZED, "credenciales-invalidas");
        assertProblema(handler.prohibido(new ForbiddenOperationException("ajeno"), req), HttpStatus.FORBIDDEN, "acceso-denegado");

        assertThat(registry.get("helpflow.auth.fallos").tag("motivo", "credenciales").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("helpflow.auth.accesos.denegados").counter().count()).isEqualTo(1.0);
    }

    @Test
    void jsonIlegible() {
        var ex = new HttpMessageNotReadableException("mal formado", new MockHttpInputMessage(new byte[0]));
        assertProblema(handler.jsonInvalido(ex, req), HttpStatus.BAD_REQUEST, "json-invalido");
    }

    @Test
    void erroresJpaNoExponenElSql() {
        ProblemDetail integridad = handler.integridad(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"), req);
        assertProblema(integridad, HttpStatus.CONFLICT, "conflicto");
        assertThat(integridad.getDetail()).doesNotContain("duplicate key");

        assertProblema(handler.baseDatos(new DataAccessResourceFailureException("conexión perdida"), req),
                HttpStatus.SERVICE_UNAVAILABLE, "base-datos");
    }

    @Test
    void erroresHttpDeSpringConservanSuCodigo() {
        ProblemDetail pd = handler.generico(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED), req);
        assertProblema(pd, HttpStatus.METHOD_NOT_ALLOWED, "http-405");
    }

    @Test
    void cualquierOtroErrorEsUn500SinDetallesInternos() {
        ProblemDetail pd = handler.generico(new IllegalStateException("NullPointer en línea 42"), req);
        assertProblema(pd, HttpStatus.INTERNAL_SERVER_ERROR, "error-interno");
        assertThat(pd.getDetail()).doesNotContain("línea 42").contains("traceId");
    }
}
