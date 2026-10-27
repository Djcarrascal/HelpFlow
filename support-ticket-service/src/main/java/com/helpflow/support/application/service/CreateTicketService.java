package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.CreateTicketCommand;
import com.helpflow.support.application.port.in.CreateTicketUseCase;
import com.helpflow.support.application.port.in.ManageTicketUseCase;
import com.helpflow.support.application.port.out.MetricsPort;
import com.helpflow.support.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.application.port.out.UserRepositoryPort;
import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.HistoryAction;
import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CreateTicketService implements CreateTicketUseCase {

    private final TicketRepositoryPort tickets;
    private final TicketHistoryRepositoryPort historial;
    private final UserRepositoryPort usuarios;
    private final SlaPolicyRepositoryPort slaPolicies;
    private final ManageTicketUseCase manageTicket;
    private final MetricsPort metrics;
    private final Clock clock;

    public CreateTicketService(TicketRepositoryPort tickets, TicketHistoryRepositoryPort historial,
                               UserRepositoryPort usuarios, SlaPolicyRepositoryPort slaPolicies,
                               ManageTicketUseCase manageTicket, MetricsPort metrics, Clock clock) {
        this.tickets = tickets;
        this.historial = historial;
        this.usuarios = usuarios;
        this.slaPolicies = slaPolicies;
        this.manageTicket = manageTicket;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Ticket crear(CreateTicketCommand cmd, AuthenticatedActor actor) {
        User solicitante = usuarios.findById(actor.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", actor.usuarioId()));
        if (!solicitante.estaActivo()) {
            throw new BusinessRuleException("Solo un usuario ACTIVO puede registrar tickets");
        }
        SlaPolicy politica = slaPolicies.findByPrioridad(cmd.prioridad())
                .orElseThrow(() -> new BusinessRuleException("No hay SLA configurado para " + cmd.prioridad()));

        // Paso 1: el ticket nace ABIERTO con su fecha límite (paso 2)
        LocalDateTime ahora = LocalDateTime.now(clock);
        Ticket ticket = Ticket.abrir(solicitante.getId(), cmd.titulo(), cmd.descripcion(), cmd.categoria(),
                cmd.prioridad(), cmd.sede(), cmd.activoAfectado(), politica, ahora);
        Ticket guardado = tickets.save(ticket);
        historial.save(TicketHistory.de(guardado, HistoryAction.CREADO, null, "Ticket registrado", actor.correo(), ahora));
        metrics.ticketCreado(guardado.getCategoria(), guardado.getPrioridad());

        // Pasos 2-5: dispatch + reglas + asignación en la misma transacción
        return manageTicket.asignarAutomaticamente(guardado.getId(), "SISTEMA");
    }
}
