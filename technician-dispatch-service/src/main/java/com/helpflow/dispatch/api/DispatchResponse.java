package com.helpflow.dispatch.api;

// Respuesta con el mismo formato del enunciado
public record DispatchResponse(
        Long ticketId,
        String tecnicoId,
        String disponibilidad,
        int tiempoEstimadoHoras
) {}
