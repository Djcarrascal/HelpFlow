package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;

public record CreateTicketCommand(String titulo, String descripcion, Category categoria,
                                  Priority prioridad, String sede, String activoAfectado) {}
