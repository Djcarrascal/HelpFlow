package com.helpflow.support.infrastructure.adapter.in.rest.validation;

import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.TicketStatus;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.ChangeStatusRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.RegisterRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.SlaUpdateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Bean Validation real (Hibernate Validator), sin levantar Spring
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    private Set<String> camposConError(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    // ---- @ValidStatusChange ----

    @Test
    void resolverSinObservacionEsInvalido() {
        assertThat(camposConError(new ChangeStatusRequest(TicketStatus.RESUELTO, " "))).containsExactly("observacion");
    }

    @Test
    void cancelarSinObservacionEsInvalido() {
        assertThat(camposConError(new ChangeStatusRequest(TicketStatus.CANCELADO, null))).containsExactly("observacion");
    }

    @Test
    void asignadoNoSePuedePedirPorEsteEndpoint() {
        assertThat(camposConError(new ChangeStatusRequest(TicketStatus.ASIGNADO, "x"))).containsExactly("estado");
    }

    @Test
    void enProgresoNoExigeObservacion() {
        assertThat(camposConError(new ChangeStatusRequest(TicketStatus.EN_PROGRESO, null))).isEmpty();
    }

    @Test
    void estadoNuloLoReportaNotNull() {
        assertThat(camposConError(new ChangeStatusRequest(null, null))).containsExactly("estado");
    }

    // ---- @ValidSlaHours (bordes por prioridad) ----

    @ParameterizedTest(name = "{0} con {1} h -> válido = {2}")
    @CsvSource({
            "CRITICA, 8, true", "CRITICA, 9, false",
            "ALTA, 24, true", "ALTA, 25, false",
            "MEDIA, 72, true", "MEDIA, 73, false",
            "BAJA, 168, true", "BAJA, 169, false",
            "BAJA, 0, false"
    })
    void horasDentroDelRangoDeLaPrioridad(Priority prioridad, int horas, boolean valido) {
        assertThat(camposConError(new SlaUpdateRequest(prioridad, horas)).isEmpty()).isEqualTo(valido);
    }

    // ---- registro ----

    @Test
    void elCorreoDebeSerCorporativo() {
        var req = new RegisterRequest("55667788", "Marta", "marta@gmail.com", "Segura2026*", "MEDELLIN", "COMPRAS");
        assertThat(camposConError(req)).containsExactly("correo");
    }

    @Test
    void documentoYClaveCortosSonInvalidos() {
        var req = new RegisterRequest("12a", "Marta", "marta@helpflow.com", "corta", "MEDELLIN", "COMPRAS");
        assertThat(camposConError(req)).containsExactlyInAnyOrder("documento", "password");
    }
}
