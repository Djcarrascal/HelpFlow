package com.helpflow.support.domain.model;

import java.time.LocalDateTime;
import java.util.Optional;

public final class AssignmentRules {

    private AssignmentRules() {}

    // Vacío = el técnico cumple todas las reglas; si no, el motivo para el historial
    public static Optional<String> evaluar(Ticket ticket, Technician tecnico, long cargaActiva,
                                           int horasEstimadas, LocalDateTime ahora) {
        if (!tecnico.estaHabilitado()) {
            return Optional.of("Técnico " + tecnico.getCodigo() + " inactivo");
        }
        if (!tecnico.atiende(ticket.getCategoria())) {
            return Optional.of("Especialidad " + tecnico.getEspecialidad() + " no atiende " + ticket.getCategoria());
        }
        if (cargaActiva >= tecnico.getCargaMaxima()) {
            return Optional.of("Técnico " + tecnico.getCodigo() + " alcanzó su carga máxima");
        }
        if (!tecnico.getSede().equalsIgnoreCase(ticket.getSede())) {
            return Optional.of("Técnico " + tecnico.getCodigo() + " está en otra sede");
        }
        if (ahora.plusHours(horasEstimadas).isAfter(ticket.getFechaLimiteSla())) {
            return Optional.of("Tiempo estimado de " + horasEstimadas + " h incumple el SLA");
        }
        return Optional.empty();
    }
}
