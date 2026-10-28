package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.out.TechnicianRepositoryPort;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Usa el TicketAccessChecker real: así se prueba también el control de acceso por propiedad
@ExtendWith(MockitoExtension.class)
class QueryTicketServiceTest {

    private static final AuthenticatedActor ADMIN = new AuthenticatedActor(1L, "admin@helpflow.com", Role.ROLE_ADMIN);
    private static final AuthenticatedActor ANA = new AuthenticatedActor(2L, "ana@helpflow.com", Role.ROLE_USUARIO);
    private static final AuthenticatedActor LUIS = new AuthenticatedActor(3L, "luis@helpflow.com", Role.ROLE_USUARIO);
    private static final AuthenticatedActor TEC9 = new AuthenticatedActor(109L, "tec009@helpflow.com", Role.ROLE_TECNICO);

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryRepositoryPort historial;
    @Mock TechnicianRepositoryPort tecnicos;

    private QueryTicketService service;

    @BeforeEach
    void setUp() {
        service = new QueryTicketService(tickets, historial, new TicketAccessChecker(tecnicos));
    }

    private Ticket ticketDeAnaAsignadoATec9() {
        Ticket t = new Ticket();
        t.setId(1L);
        t.setSolicitanteId(2L);
        t.setTecnicoId(9L);
        return t;
    }

    private void tec9TienePerfil() {
        Technician perfil = new Technician();
        perfil.setId(9L);
        when(tecnicos.findByUsuarioId(109L)).thenReturn(Optional.of(perfil));
    }

    @Test
    void elAdminListaTodos() {
        when(tickets.findAll()).thenReturn(List.of(ticketDeAnaAsignadoATec9()));
        assertThat(service.listar(ADMIN)).hasSize(1);
    }

    @Test
    void elUsuarioListaSoloLosSuyos() {
        when(tickets.findBySolicitanteId(2L)).thenReturn(List.of(ticketDeAnaAsignadoATec9()));
        assertThat(service.listar(ANA)).hasSize(1);
        verify(tickets, never()).findAll();
    }

    @Test
    void elTecnicoListaSusAsignados() {
        tec9TienePerfil();
        when(tickets.findByTecnicoId(9L)).thenReturn(List.of(ticketDeAnaAsignadoATec9()));
        assertThat(service.listar(TEC9)).hasSize(1);
    }

    @Test
    void unTecnicoSinPerfilNoVeTickets() {
        when(tecnicos.findByUsuarioId(109L)).thenReturn(Optional.empty());
        assertThat(service.listar(TEC9)).isEmpty();
        verify(tickets, never()).findByTecnicoId(anyLong());
    }

    @Test
    void elDuenoYElTecnicoAsignadoPuedenVerElDetalle() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketDeAnaAsignadoATec9()));
        tec9TienePerfil();
        assertThat(service.obtener(1L, ANA).getId()).isEqualTo(1L);
        assertThat(service.obtener(1L, TEC9).getId()).isEqualTo(1L);
    }

    @Test
    void otroUsuarioNoPuedeVerElTicket() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketDeAnaAsignadoATec9()));
        assertThatThrownBy(() -> service.obtener(1L, LUIS)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void ticketInexistente() {
        when(tickets.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(99L, ADMIN)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elHistorialVerificaElAccesoAntesDeConsultar() {
        when(tickets.findById(1L)).thenReturn(Optional.of(ticketDeAnaAsignadoATec9()));

        assertThatThrownBy(() -> service.historial(1L, LUIS)).isInstanceOf(ForbiddenOperationException.class);
        verify(historial, never()).findByTicketId(anyLong());

        when(historial.findByTicketId(1L)).thenReturn(List.of(new TicketHistory()));
        assertThat(service.historial(1L, ANA)).hasSize(1);
    }
}
