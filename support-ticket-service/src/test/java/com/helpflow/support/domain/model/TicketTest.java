package com.helpflow.support.domain.model;

import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 10, 0);

    private Ticket ticket(LocalDateTime creacion) {
        return Ticket.abrir(1L, "VPN caída", "No conecta la VPN", Category.RED, Priority.ALTA,
                "BARRANQUILLA", "ACT-1", new SlaPolicy(Priority.ALTA, 8), creacion);
    }

    private Technician tecnico(long id, String codigo) {
        Technician tec = new Technician();
        tec.setId(id);
        tec.setCodigo(codigo);
        return tec;
    }

    // ---- creación y SLA ----

    @Test
    void naceAbiertoConFechaLimiteSegunPrioridad() {
        Ticket t = ticket(AHORA);
        assertThat(t.getEstado()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(t.getFechaLimiteSla()).isEqualTo(AHORA.plusHours(8));
        assertThat(t.isEscalado()).isFalse();
        assertThat(t.getTecnicoId()).isNull();
    }

    // ---- transiciones ----

    @Test
    void noPermiteSaltarDeAbiertoAResuelto() {
        Ticket t = ticket(AHORA);
        assertThatThrownBy(() -> t.cambiarEstado(TicketStatus.RESUELTO, AHORA))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("ABIERTO -> RESUELTO");
        assertThat(t.getEstado()).isEqualTo(TicketStatus.ABIERTO);
    }

    @Test
    void asignadoNoSeObtieneConCambioManual() {
        Ticket t = ticket(AHORA);
        assertThatThrownBy(() -> t.cambiarEstado(TicketStatus.ASIGNADO, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void asignarGuardaElTecnicoYCambiaElEstado() {
        Ticket t = ticket(AHORA);
        t.asignar(tecnico(9L, "TEC-009"));
        assertThat(t.getEstado()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(t.getTecnicoCodigo()).isEqualTo("TEC-009");
        assertThat(t.estaAsignadoA(9L)).isTrue();
        assertThat(t.estaAsignadoA(10L)).isFalse();
        assertThat(t.estaAsignadoA(null)).isFalse();
    }

    @Test
    void noSePuedeAsignarDosVeces() {
        Ticket t = ticket(AHORA);
        t.asignar(tecnico(9L, "TEC-009"));
        assertThatThrownBy(() -> t.asignar(tecnico(10L, "TEC-010")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void registraPrimeraRespuestaYResolucion() {
        Ticket t = ticket(AHORA);
        t.asignar(tecnico(9L, "TEC-009"));
        t.cambiarEstado(TicketStatus.EN_PROGRESO, AHORA.plusHours(1));
        t.cambiarEstado(TicketStatus.RESUELTO, AHORA.plusHours(3));
        assertThat(t.getFechaPrimeraRespuesta()).isEqualTo(AHORA.plusHours(1));
        assertThat(t.getFechaResolucion()).isEqualTo(AHORA.plusHours(3));
    }

    @Test
    void laReaperturaBorraLaResolucionPeroConservaLaPrimeraRespuesta() {
        Ticket t = ticket(AHORA);
        t.asignar(tecnico(9L, "TEC-009"));
        t.cambiarEstado(TicketStatus.EN_PROGRESO, AHORA.plusHours(1));
        t.cambiarEstado(TicketStatus.RESUELTO, AHORA.plusHours(3));
        t.cambiarEstado(TicketStatus.EN_PROGRESO, AHORA.plusHours(4));
        assertThat(t.getFechaResolucion()).isNull();
        assertThat(t.getFechaPrimeraRespuesta()).isEqualTo(AHORA.plusHours(1));
    }

    // ---- reasignación ----

    @Test
    void reasignarCambiaElTecnicoSinCambiarElEstado() {
        Ticket t = ticket(AHORA);
        t.asignar(tecnico(9L, "TEC-009"));
        t.reasignar(tecnico(18L, "TEC-018"));
        assertThat(t.getTecnicoCodigo()).isEqualTo("TEC-018");
        assertThat(t.getEstado()).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test
    void noSeReasignaUnTicketAbierto() {
        Ticket t = ticket(AHORA);
        assertThatThrownBy(() -> t.reasignar(tecnico(18L, "TEC-018")))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---- vencimiento y escalamiento ----

    @Test
    void detectaProximoAVencerYVencido() {
        Ticket t = ticket(AHORA);
        assertThat(t.estaProximoAVencer(AHORA.plusHours(5))).isFalse();     // quedan 3 h de 8
        assertThat(t.estaProximoAVencer(AHORA.plusHours(7))).isTrue();      // queda 1 h (12 %)
        assertThat(t.estaVencido(AHORA.plusHours(9))).isTrue();
    }

    @Test
    void elUmbralDel25PorCientoEsInclusivo() {
        Ticket t = ticket(AHORA);
        // 8 h = 480 min; 25 % = 120 min restantes
        assertThat(t.estaProximoAVencer(AHORA.plusMinutes(360))).isTrue();   // quedan 120
        assertThat(t.estaProximoAVencer(AHORA.plusMinutes(359))).isFalse();  // quedan 121
    }

    @Test
    void unTicketVencidoYaNoSeConsideraProximoAVencer() {
        Ticket t = ticket(AHORA);
        assertThat(t.estaProximoAVencer(AHORA.plusHours(9))).isFalse();
    }

    @Test
    void unTicketCerradoNuncaEstaVencido() {
        Ticket t = ticket(AHORA);
        t.cambiarEstado(TicketStatus.CANCELADO, AHORA);
        assertThat(t.estaVencido(AHORA.plusDays(10))).isFalse();
        assertThat(t.estaProximoAVencer(AHORA.plusHours(7))).isFalse();
    }

    @Test
    void escalarSubeUnNivelYNoSeRepite() {
        Ticket t = ticket(AHORA);
        t.escalar();
        assertThat(t.getPrioridad()).isEqualTo(Priority.CRITICA);
        assertThat(t.isEscalado()).isTrue();
        assertThat(t.estaProximoAVencer(AHORA.plusHours(7))).isFalse();
    }

    @Test
    void perteneceSoloASuSolicitante() {
        Ticket t = ticket(AHORA);
        assertThat(t.perteneceA(1L)).isTrue();
        assertThat(t.perteneceA(2L)).isFalse();
    }
}
