package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import java.util.List;

public interface QueryTicketUseCase {
    List<Ticket> listar(AuthenticatedActor actor);
    Ticket obtener(Long ticketId, AuthenticatedActor actor);
    List<TicketHistory> historial(Long ticketId, AuthenticatedActor actor);
}
