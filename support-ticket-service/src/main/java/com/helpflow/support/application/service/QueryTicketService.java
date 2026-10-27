package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.QueryTicketUseCase;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class QueryTicketService implements QueryTicketUseCase {

    private final TicketRepositoryPort tickets;
    private final TicketHistoryRepositoryPort historial;
    private final TicketAccessChecker acceso;

    public QueryTicketService(TicketRepositoryPort tickets, TicketHistoryRepositoryPort historial,
                              TicketAccessChecker acceso) {
        this.tickets = tickets;
        this.historial = historial;
        this.acceso = acceso;
    }

    // Cada rol ve un conjunto distinto de tickets
    @Override
    public List<Ticket> listar(AuthenticatedActor actor) {
        if (actor.esAdmin()) return tickets.findAll();
        if (actor.esTecnico()) {
            Long idTecnico = acceso.idTecnico(actor);
            return idTecnico == null ? List.of() : tickets.findByTecnicoId(idTecnico);
        }
        return tickets.findBySolicitanteId(actor.usuarioId());
    }

    @Override
    public Ticket obtener(Long ticketId, AuthenticatedActor actor) {
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        acceso.verificarLectura(ticket, actor);
        return ticket;
    }

    @Override
    public List<TicketHistory> historial(Long ticketId, AuthenticatedActor actor) {
        obtener(ticketId, actor);     // reutiliza la verificación de acceso
        return historial.findByTicketId(ticketId);
    }
}
