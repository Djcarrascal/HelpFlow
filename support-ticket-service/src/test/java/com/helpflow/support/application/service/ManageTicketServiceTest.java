package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.out.DispatchUnavailableException;
import com.helpflow.support.application.port.out.MetricsPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort.DispatchSuggestion;
import com.helpflow.support.application.port.out.TechnicianRepositoryPort;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.exception.InvalidStatusTransitionException;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.HistoryAction;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageTicketServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(CLOCK);

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryRepositoryPort historial;
    @Mock TechnicianRepositoryPort tecnicos;
    @Mock TechnicianDispatchPort dispatch;
    @Mock MetricsPort metrics;
    @Mock TicketAccessChecker acceso;

    private ManageTicketService service;

    @BeforeEach
    void setUp() {
        service = new ManageTicketService(tickets, historial, tecnicos, dispatch, metrics, acceso, CLOCK);
    }

    private Ticket ticketAbierto(LocalDateTime creacion) {
        Ticket t = Ticket.abrir(100L, "VPN caída", "No conecta la VPN", Category.RED, Priority.ALTA,
                "BARRANQUILLA", "ACT-1", new SlaPolicy(Priority.ALTA, 8), creacion);
        t.setId(1L);
        return t;
    }

    private Technician tecnico(long id, String codigo, RecordStatus estado) {
        Technician t = new Technician();
        t.setId(id);
        t.setCodigo(codigo);
        t.setEspecialidad(Category.RED);
        t.setSede("BARRANQUILLA");
        t.setCargaMaxima(5);
        t.setEstado(estado);
        return t;
    }

    @Test
    void asignaAlTecnicoSugeridoCuandoCumpleLasReglas() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
        when(dispatch.sugerir(1L, Category.RED, Priority.ALTA)).thenReturn(new DispatchSuggestion("TEC-009", "ALTA", 2));
        when(tecnicos.findByCodigo("TEC-009")).thenReturn(Optional.of(tecnico(9L, "TEC-009", RecordStatus.ACTIVO)));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket resultado = service.asignarAutomaticamente(1L, "SISTEMA");

        assertThat(resultado.getEstado()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(resultado.getTecnicoCodigo()).isEqualTo("TEC-009");
        verify(historial).save(argThat(h -> h.getAccion() == HistoryAction.ASIGNADO));
    }

    @Test
    void usaTecnicoAlternativoSiElSugeridoEstaInactivo() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
        when(dispatch.sugerir(1L, Category.RED, Priority.ALTA)).thenReturn(new DispatchSuggestion("TEC-009", "ALTA", 2));
        when(tecnicos.findByCodigo("TEC-009")).thenReturn(Optional.of(tecnico(9L, "TEC-009", RecordStatus.INACTIVO)));
        when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA"))
                .thenReturn(List.of(tecnico(10L, "TEC-010", RecordStatus.ACTIVO)));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket resultado = service.asignarAutomaticamente(1L, "SISTEMA");

        assertThat(resultado.getTecnicoCodigo()).isEqualTo("TEC-010");
    }

    @Test
    void dejaElTicketAbiertoSiDispatchFallaYNoHayAlternativos() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
        when(dispatch.sugerir(anyLong(), any(), any())).thenThrow(new DispatchUnavailableException("caído", null));
        when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA")).thenReturn(List.of());

        Ticket resultado = service.asignarAutomaticamente(1L, "SISTEMA");

        assertThat(resultado.getEstado()).isEqualTo(TicketStatus.ABIERTO);
        verify(tickets, never()).save(any());
        verify(historial).save(argThat(h -> h.getAccion() == HistoryAction.SIN_TECNICO));
    }

    @Test
    void rechazaTransicionInvalida() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
        AuthenticatedActor admin = new AuthenticatedActor(1L, "admin@helpflow.com", Role.ROLE_ADMIN);

        assertThatThrownBy(() -> service.cambiarEstado(1L, TicketStatus.RESUELTO, "listo", admin))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(tickets, never()).save(any());
    }

    @Test
    void escalaTicketsProximosAVencer() {
        Ticket casiVencido = ticketAbierto(AHORA.minusMinutes(450));   // le quedan 30 min de 8 h
        when(tickets.findActivos()).thenReturn(List.of(casiVencido));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        int escalados = service.escalarProximosAVencer();

        assertThat(escalados).isEqualTo(1);
        assertThat(casiVencido.getPrioridad()).isEqualTo(Priority.CRITICA);
        verify(metrics).ticketEscalado();
    }

    @Test
    void noEscalaTicketsConTiempoSuficiente() {
        when(tickets.findActivos()).thenReturn(List.of(ticketAbierto(AHORA.minusHours(1))));

        assertThat(service.escalarProximosAVencer()).isZero();
        verify(tickets, never()).save(any());
    }
}
