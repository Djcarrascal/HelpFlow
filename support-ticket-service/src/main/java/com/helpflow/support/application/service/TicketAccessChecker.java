package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.out.TechnicianRepositoryPort;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.domain.model.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketAccessChecker {

    private final TechnicianRepositoryPort tecnicos;

    public TicketAccessChecker(TechnicianRepositoryPort tecnicos) {
        this.tecnicos = tecnicos;
    }

    // Lectura: dueño, técnico asignado o admin
    public void verificarLectura(Ticket ticket, AuthenticatedActor actor) {
        if (actor.esAdmin()) return;
        if (actor.esTecnico() && ticket.estaAsignadoA(idTecnico(actor))) return;
        if (ticket.perteneceA(actor.usuarioId())) return;
        throw new ForbiddenOperationException("No tienes acceso al ticket " + ticket.getId());
    }

    // Gestión de estado: técnico asignado o admin
    public void verificarGestion(Ticket ticket, AuthenticatedActor actor) {
        if (actor.esAdmin()) return;
        if (actor.esTecnico() && ticket.estaAsignadoA(idTecnico(actor))) return;
        throw new ForbiddenOperationException("Solo el técnico asignado o un admin gestiona el ticket " + ticket.getId());
    }

    public Long idTecnico(AuthenticatedActor actor) {
        return tecnicos.findByUsuarioId(actor.usuarioId()).map(Technician::getId).orElse(null);
    }
}
