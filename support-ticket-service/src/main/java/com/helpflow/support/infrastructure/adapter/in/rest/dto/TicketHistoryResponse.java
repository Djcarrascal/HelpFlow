package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record TicketHistoryResponse(String accion, String estadoAnterior, String estadoNuevo,
                                    String observacion, String responsable, LocalDateTime fecha) {}
