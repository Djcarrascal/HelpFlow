package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(min = 5, max = 150) String titulo,
        @NotBlank @Size(min = 10, max = 2000) String descripcion,
        @NotNull Category categoria,
        @NotNull Priority prioridad,
        @NotBlank @Size(max = 60) String sede,
        @NotBlank @Size(max = 120) String activoAfectado
) {}
