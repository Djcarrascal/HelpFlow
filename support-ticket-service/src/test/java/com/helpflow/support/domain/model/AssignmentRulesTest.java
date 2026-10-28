package com.helpflow.support.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Las 5 reglas de asignación, incluidos los bordes de cada una
class AssignmentRulesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 10, 0);

    private Ticket ticketRedAlta() {
        return Ticket.abrir(1L, "VPN caída", "No conecta la VPN", Category.RED, Priority.ALTA,
                "BARRANQUILLA", "ACT-1", new SlaPolicy(Priority.ALTA, 8), AHORA);
    }

    private Technician tecnico(Category especialidad, String sede, RecordStatus estado) {
        Technician t = new Technician();
        t.setId(9L);
        t.setCodigo("TEC-009");
        t.setEspecialidad(especialidad);
        t.setSede(sede);
        t.setCargaMaxima(5);
        t.setEstado(estado);
        return t;
    }

    @Test
    void aceptaTecnicoQueCumpleTodasLasReglas() {
        var t = tecnico(Category.RED, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 2, AHORA)).isEmpty();
    }

    @Test
    void rechazaTecnicoInactivo() {
        var t = tecnico(Category.RED, "BARRANQUILLA", RecordStatus.INACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 2, AHORA)).hasValueSatisfying(
                motivo -> assertThat(motivo).contains("inactivo"));
    }

    @Test
    void rechazaEspecialidadDistinta() {
        var t = tecnico(Category.HARDWARE, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 2, AHORA)).hasValueSatisfying(
                motivo -> assertThat(motivo).contains("no atiende"));
    }

    @Test
    void elGeneralistaOtroAtiendeCualquierCategoria() {
        var t = tecnico(Category.OTRO, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 2, AHORA)).isEmpty();
    }

    @Test
    void cargaJustoDebajoDelMaximoSePermite() {
        var t = tecnico(Category.RED, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 4, 2, AHORA)).isEmpty();
    }

    @Test
    void cargaIgualAlMaximoSeRechaza() {
        var t = tecnico(Category.RED, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 5, 2, AHORA)).hasValueSatisfying(
                motivo -> assertThat(motivo).contains("carga máxima"));
    }

    @Test
    void rechazaTecnicoDeOtraSede() {
        var t = tecnico(Category.RED, "MEDELLIN", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 2, AHORA)).hasValueSatisfying(
                motivo -> assertThat(motivo).contains("otra sede"));
    }

    @Test
    void laSedeNoDistingueMayusculas() {
        var t = tecnico(Category.RED, "barranquilla", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 2, AHORA)).isEmpty();
    }

    @Test
    void terminarJustoEnLaFechaLimiteCumpleElSla() {
        var t = tecnico(Category.RED, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 8, AHORA)).isEmpty();
    }

    @Test
    void unaHoraMasQueElSlaSeRechaza() {
        var t = tecnico(Category.RED, "BARRANQUILLA", RecordStatus.ACTIVO);
        assertThat(AssignmentRules.evaluar(ticketRedAlta(), t, 0, 9, AHORA)).hasValueSatisfying(
                motivo -> assertThat(motivo).contains("incumple el SLA"));
    }
}
