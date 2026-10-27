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

    @Test
    void naceAbiertoConFechaLimiteSegunPrioridad() {
        Ticket t = ticket(AHORA);
        assertThat(t.getEstado()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(t.getFechaLimiteSla()).isEqualTo(AHORA.plusHours(8));
    }

    @Test
    void noPermiteSaltarDeAbiertoAResuelto() {
        Ticket t = ticket(AHORA);
        assertThatThrownBy(() -> t.cambiarEstado(TicketStatus.RESUELTO, AHORA))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void asignadoNoSeObtieneConCambioManual() {
        Ticket t = ticket(AHORA);
        assertThatThrownBy(() -> t.cambiarEstado(TicketStatus.ASIGNADO, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void registraPrimeraRespuestaYResolucion() {
        Ticket t = ticket(AHORA);
        Technician tec = new Technician();
        tec.setId(9L);
        tec.setCodigo("TEC-009");
        t.asignar(tec);
        t.cambiarEstado(TicketStatus.EN_PROGRESO, AHORA.plusHours(1));
        t.cambiarEstado(TicketStatus.RESUELTO, AHORA.plusHours(3));
        assertThat(t.getFechaPrimeraRespuesta()).isEqualTo(AHORA.plusHours(1));
        assertThat(t.getFechaResolucion()).isEqualTo(AHORA.plusHours(3));
    }

    @Test
    void detectaProximoAVencerYVencido() {
        Ticket t = ticket(AHORA);
        assertThat(t.estaProximoAVencer(AHORA.plusHours(5))).isFalse();     // quedan 3 h de 8
        assertThat(t.estaProximoAVencer(AHORA.plusHours(7))).isTrue();      // queda 1 h (12 %)
        assertThat(t.estaVencido(AHORA.plusHours(9))).isTrue();
    }

    @Test
    void escalarSubeUnNivelDePrioridad() {
        Ticket t = ticket(AHORA);
        t.escalar();
        assertThat(t.getPrioridad()).isEqualTo(Priority.CRITICA);
        assertThat(t.isEscalado()).isTrue();
    }
}
