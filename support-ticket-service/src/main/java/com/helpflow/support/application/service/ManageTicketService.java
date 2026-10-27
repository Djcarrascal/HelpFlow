package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.ManageTicketUseCase;
import com.helpflow.support.application.port.out.DispatchUnavailableException;
import com.helpflow.support.application.port.out.MetricsPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort.DispatchSuggestion;
import com.helpflow.support.application.port.out.TechnicianRepositoryPort;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.AssignmentRules;
import com.helpflow.support.domain.model.HistoryAction;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.domain.model.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ManageTicketService implements ManageTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageTicketService.class);
    private static final String SISTEMA = "SISTEMA";

    private final TicketRepositoryPort tickets;
    private final TicketHistoryRepositoryPort historial;
    private final TechnicianRepositoryPort tecnicos;
    private final TechnicianDispatchPort dispatch;
    private final MetricsPort metrics;
    private final TicketAccessChecker acceso;
    private final Clock clock;

    public ManageTicketService(TicketRepositoryPort tickets, TicketHistoryRepositoryPort historial,
                               TechnicianRepositoryPort tecnicos, TechnicianDispatchPort dispatch,
                               MetricsPort metrics, TicketAccessChecker acceso, Clock clock) {
        this.tickets = tickets;
        this.historial = historial;
        this.tecnicos = tecnicos;
        this.dispatch = dispatch;
        this.metrics = metrics;
        this.acceso = acceso;
        this.clock = clock;
    }

    @Override
    public Ticket asignarAutomaticamente(Long ticketId, String responsable) {
        Ticket ticket = buscar(ticketId);
        LocalDateTime ahora = LocalDateTime.now(clock);
        Technician elegido = null;
        String motivo;

        // Pasos 2-3: consultar a dispatch a través del puerto
        try {
            DispatchSuggestion sugerencia = dispatch.sugerir(ticket.getId(), ticket.getCategoria(), ticket.getPrioridad());
            Optional<Technician> sugerido = tecnicos.findByCodigo(sugerencia.tecnicoCodigo());
            if (sugerido.isEmpty()) {
                motivo = "Técnico sugerido " + sugerencia.tecnicoCodigo() + " no existe";
            } else {
                // Paso 4: reglas internas de asignación
                Technician t = sugerido.get();
                Optional<String> rechazo = AssignmentRules.evaluar(ticket, t,
                        tickets.countActivosByTecnicoId(t.getId()), sugerencia.tiempoEstimadoHoras(), ahora);
                motivo = rechazo.orElse("Sugerido por dispatch (" + sugerencia.disponibilidad()
                        + ", " + sugerencia.tiempoEstimadoHoras() + " h)");
                if (rechazo.isEmpty()) elegido = t;
            }
        } catch (DispatchUnavailableException e) {
            log.warn("dispatch no disponible para ticket {}: {}", ticketId, e.getMessage());
            motivo = "technician-dispatch-service no disponible";
        }

        // Si el sugerido no sirve, buscar un alternativo en la base
        if (elegido == null) {
            elegido = buscarAlternativo(ticket, ahora);
            if (elegido != null) motivo = motivo + "; asignado alternativo " + elegido.getCodigo();
        }

        if (elegido == null) {
            registrar(ticket, HistoryAction.SIN_TECNICO, TicketStatus.ABIERTO, motivo + "; sin técnico disponible", responsable, ahora);
            return ticket;
        }

        // Paso 5: generar la asignación y registrar historial
        ticket.asignar(elegido);
        Ticket guardado = tickets.save(ticket);
        registrar(guardado, HistoryAction.ASIGNADO, TicketStatus.ABIERTO, motivo, responsable, ahora);
        log.info("ticket {} asignado a {}", guardado.getId(), elegido.getCodigo());
        return guardado;
    }

    @Override
    public Ticket cambiarEstado(Long ticketId, TicketStatus nuevo, String observacion, AuthenticatedActor actor) {
        Ticket ticket = buscar(ticketId);
        acceso.verificarGestion(ticket, actor);
        LocalDateTime ahora = LocalDateTime.now(clock);
        TicketStatus anterior = ticket.getEstado();
        boolean sinPrimeraRespuesta = ticket.getFechaPrimeraRespuesta() == null;

        ticket.cambiarEstado(nuevo, ahora);          // el dominio valida la transición
        Ticket guardado = tickets.save(ticket);
        registrar(guardado, HistoryAction.CAMBIO_ESTADO, anterior, observacion, actor.correo(), ahora);

        if (sinPrimeraRespuesta && nuevo == TicketStatus.EN_PROGRESO) {
            metrics.primeraRespuesta(Duration.between(guardado.getFechaCreacion(), ahora));
        }
        if (nuevo == TicketStatus.RESUELTO) {
            metrics.resolucion(Duration.between(guardado.getFechaCreacion(), ahora));
        }
        return guardado;
    }

    @Override
    public Ticket reasignar(Long ticketId, String tecnicoCodigo, String observacion, AuthenticatedActor actor) {
        Ticket ticket = buscar(ticketId);
        Technician tecnico = tecnicos.findByCodigo(tecnicoCodigo)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico", tecnicoCodigo));

        // Validación cruzada: técnico habilitado, compatible y con capacidad
        if (!tecnico.estaHabilitado() || !tecnico.atiende(ticket.getCategoria())) {
            throw new BusinessRuleException("Técnico " + tecnicoCodigo + " no habilitado para " + ticket.getCategoria());
        }
        if (tickets.countActivosByTecnicoId(tecnico.getId()) >= tecnico.getCargaMaxima()) {
            throw new BusinessRuleException("Técnico " + tecnicoCodigo + " alcanzó su carga máxima");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        TicketStatus anterior = ticket.getEstado();
        if (anterior == TicketStatus.ABIERTO) {
            ticket.asignar(tecnico);
        } else {
            ticket.reasignar(tecnico);
        }
        Ticket guardado = tickets.save(ticket);
        registrar(guardado, HistoryAction.REASIGNADO, anterior, observacion, actor.correo(), ahora);
        return guardado;
    }

    @Override
    public int escalarProximosAVencer() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        int escalados = 0;
        for (Ticket ticket : tickets.findActivos()) {
            if (ticket.estaProximoAVencer(ahora)) {
                Priority antes = ticket.getPrioridad();
                ticket.escalar();
                Ticket guardado = tickets.save(ticket);
                registrar(guardado, HistoryAction.ESCALADO, guardado.getEstado(),
                        "Prioridad " + antes + " -> " + guardado.getPrioridad() + " por proximidad al SLA", SISTEMA, ahora);
                metrics.ticketEscalado();
                escalados++;
            }
        }
        if (escalados > 0) log.info("{} tickets escalados por SLA", escalados);
        return escalados;
    }

    // Activos de la sede con especialidad compatible; gana el de menor carga
    private Technician buscarAlternativo(Ticket ticket, LocalDateTime ahora) {
        List<Technician> candidatos = tecnicos.findCandidatos(ticket.getCategoria(), ticket.getSede());
        Technician mejor = null;
        long menorCarga = Long.MAX_VALUE;
        for (Technician t : candidatos) {
            long carga = tickets.countActivosByTecnicoId(t.getId());
            boolean cumple = AssignmentRules.evaluar(ticket, t, carga, 0, ahora).isEmpty();
            if (cumple && carga < menorCarga) {
                mejor = t;
                menorCarga = carga;
            }
        }
        return mejor;
    }

    private Ticket buscar(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    private void registrar(Ticket ticket, HistoryAction accion, TicketStatus anterior,
                           String observacion, String responsable, LocalDateTime ahora) {
        historial.save(TicketHistory.de(ticket, accion, anterior, observacion, responsable, ahora));
    }
}
