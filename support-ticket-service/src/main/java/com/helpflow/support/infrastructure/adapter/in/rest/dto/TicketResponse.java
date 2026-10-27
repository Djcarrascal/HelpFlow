package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record TicketResponse(Long id, Long solicitanteId, String titulo, String descripcion,
                             String categoria, String prioridad, String sede, String activoAfectado,
                             String estado, LocalDateTime fechaCreacion, LocalDateTime fechaLimiteSla,
                             String tecnicoCodigo, boolean escalado,
                             LocalDateTime fechaPrimeraRespuesta, LocalDateTime fechaResolucion) {}
