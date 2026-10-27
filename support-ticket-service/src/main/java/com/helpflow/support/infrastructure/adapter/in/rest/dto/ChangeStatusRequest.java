package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import com.helpflow.support.domain.model.TicketStatus;
import com.helpflow.support.infrastructure.adapter.in.rest.validation.ValidStatusChange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidStatusChange
public record ChangeStatusRequest(@NotNull TicketStatus estado, @Size(max = 500) String observacion) {}
