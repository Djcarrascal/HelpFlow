package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.infrastructure.adapter.in.rest.validation.ValidSlaHours;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ValidSlaHours
public record SlaUpdateRequest(@NotNull Priority prioridad, @NotNull @Min(1) Integer horasLimite) {}
