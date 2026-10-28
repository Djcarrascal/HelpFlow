package com.helpflow.support.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityTest {

    // CRITICA es el tope: escalar no puede pasarse de ahí
    @ParameterizedTest(name = "{0} escala a {1}")
    @CsvSource({"BAJA, MEDIA", "MEDIA, ALTA", "ALTA, CRITICA", "CRITICA, CRITICA"})
    void siguienteSubeUnNivel(Priority actual, Priority esperada) {
        assertThat(actual.siguiente()).isEqualTo(esperada);
    }
}
