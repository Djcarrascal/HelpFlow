package com.helpflow.support.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

// Máquina de estados: cada transición del diagrama, válida o no
class TicketStatusTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            // válidas
            "ABIERTO, ASIGNADO, true",
            "ABIERTO, CANCELADO, true",
            "ASIGNADO, EN_PROGRESO, true",
            "ASIGNADO, CANCELADO, true",
            "EN_PROGRESO, RESUELTO, true",
            "RESUELTO, CERRADO, true",
            "RESUELTO, EN_PROGRESO, true",
            // inválidas
            "ABIERTO, EN_PROGRESO, false",
            "ABIERTO, RESUELTO, false",
            "ASIGNADO, ABIERTO, false",
            "EN_PROGRESO, CANCELADO, false",
            "EN_PROGRESO, ASIGNADO, false",
            "RESUELTO, CANCELADO, false",
            "CERRADO, EN_PROGRESO, false",
            "CERRADO, ABIERTO, false",
            "CANCELADO, ABIERTO, false",
            "CANCELADO, ASIGNADO, false"
    })
    void respetaLaMaquinaDeEstados(TicketStatus desde, TicketStatus hacia, boolean esperado) {
        assertThat(desde.puedePasarA(hacia)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"ABIERTO", "ASIGNADO", "EN_PROGRESO"})
    void estadosActivos(TicketStatus estado) {
        assertThat(estado.esActivo()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESUELTO", "CERRADO", "CANCELADO"})
    void estadosNoActivos(TicketStatus estado) {
        assertThat(estado.esActivo()).isFalse();
    }
}
