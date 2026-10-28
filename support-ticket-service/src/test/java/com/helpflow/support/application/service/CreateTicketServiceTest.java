package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.CreateTicketCommand;
import com.helpflow.support.application.port.in.ManageTicketUseCase;
import com.helpflow.support.application.port.out.MetricsPort;
import com.helpflow.support.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.application.port.out.UserRepositoryPort;
import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.HistoryAction;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.domain.model.TicketStatus;
import com.helpflow.support.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTicketServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(CLOCK);
    private static final AuthenticatedActor ANA = new AuthenticatedActor(2L, "ana@helpflow.com", Role.ROLE_USUARIO);
    private static final CreateTicketCommand CMD = new CreateTicketCommand("VPN caída", "No conecta la VPN",
            Category.RED, Priority.ALTA, "BARRANQUILLA", "ACT-1");

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryRepositoryPort historial;
    @Mock UserRepositoryPort usuarios;
    @Mock SlaPolicyRepositoryPort slaPolicies;
    @Mock ManageTicketUseCase manageTicket;
    @Mock MetricsPort metrics;

    @Captor ArgumentCaptor<Ticket> ticketCaptor;
    @Captor ArgumentCaptor<TicketHistory> historialCaptor;

    private CreateTicketService service;

    @BeforeEach
    void setUp() {
        service = new CreateTicketService(tickets, historial, usuarios, slaPolicies, manageTicket, metrics, CLOCK);
    }

    private User usuario(RecordStatus estado) {
        User u = new User();
        u.setId(2L);
        u.setEstado(estado);
        return u;
    }

    @Test
    void creaElTicketAbiertoConSlaYLuegoLoAsigna() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario(RecordStatus.ACTIVO)));
        when(slaPolicies.findByPrioridad(Priority.ALTA)).thenReturn(Optional.of(new SlaPolicy(Priority.ALTA, 8)));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });
        Ticket asignado = new Ticket();
        when(manageTicket.asignarAutomaticamente(10L, "SISTEMA")).thenReturn(asignado);

        Ticket resultado = service.crear(CMD, ANA);

        assertThat(resultado).isSameAs(asignado);
        // Orden del flujo obligatorio: guardar, historial, métrica y asignación
        InOrder orden = inOrder(tickets, historial, metrics, manageTicket);
        orden.verify(tickets).save(ticketCaptor.capture());
        orden.verify(historial).save(historialCaptor.capture());
        orden.verify(metrics).ticketCreado(Category.RED, Priority.ALTA);
        orden.verify(manageTicket).asignarAutomaticamente(10L, "SISTEMA");

        Ticket guardado = ticketCaptor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(guardado.getSolicitanteId()).isEqualTo(2L);
        assertThat(guardado.getFechaCreacion()).isEqualTo(AHORA);
        assertThat(guardado.getFechaLimiteSla()).isEqualTo(AHORA.plusHours(8));
        assertThat(historialCaptor.getValue().getAccion()).isEqualTo(HistoryAction.CREADO);
        assertThat(historialCaptor.getValue().getResponsable()).isEqualTo("ana@helpflow.com");
    }

    @Test
    void usuarioInactivoNoPuedeRegistrarTickets() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario(RecordStatus.INACTIVO)));

        assertThatThrownBy(() -> service.crear(CMD, ANA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ACTIVO");
        verifyNoInteractions(tickets, historial, metrics, manageTicket);
    }

    @Test
    void usuarioInexistente() {
        when(usuarios.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(CMD, ANA)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sinPoliticaDeSlaNoSeCreaElTicket() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario(RecordStatus.ACTIVO)));
        when(slaPolicies.findByPrioridad(Priority.ALTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(CMD, ANA)).isInstanceOf(BusinessRuleException.class);
        verify(tickets, never()).save(any());
    }
}
