package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketStatus;

// Caso de uso "Gestionar Ticket" del enunciado
public interface ManageTicketUseCase {
    Ticket asignarAutomaticamente(Long ticketId, String responsable);
    Ticket cambiarEstado(Long ticketId, TicketStatus nuevo, String observacion, AuthenticatedActor actor);
    Ticket reasignar(Long ticketId, String tecnicoCodigo, String observacion, AuthenticatedActor actor);
    int escalarProximosAVencer();
}
