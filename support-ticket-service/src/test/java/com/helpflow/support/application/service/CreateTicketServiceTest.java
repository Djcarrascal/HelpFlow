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
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTicketServiceTest {

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryRepositoryPort historial;
    @Mock UserRepositoryPort usuarios;
    @Mock SlaPolicyRepositoryPort slaPolicies;
    @Mock ManageTicketUseCase manageTicket;
    @Mock MetricsPort metrics;
    @Mock Clock clock;
    @InjectMocks CreateTicketService service;

    @Test
    void usuarioInactivoNoPuedeRegistrarTickets() {
        User inactivo = new User();
        inactivo.setId(5L);
        inactivo.setEstado(RecordStatus.INACTIVO);
        when(usuarios.findById(5L)).thenReturn(Optional.of(inactivo));
        var actor = new AuthenticatedActor(5L, "x@helpflow.com", Role.ROLE_USUARIO);
        var cmd = new CreateTicketCommand("VPN caída", "No conecta la VPN", Category.RED,
                Priority.ALTA, "BARRANQUILLA", "ACT-1");

        assertThatThrownBy(() -> service.crear(cmd, actor)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(tickets, manageTicket);
    }
}
