package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import com.helpflow.support.domain.model.RecordStatus;
import jakarta.validation.constraints.Size;

// Todos opcionales: solo se cambia lo que se envía
public record UpdateUserRequest(@Size(min = 3, max = 120) String nombre,
                                @Size(max = 60) String sede,
                                @Size(max = 60) String area,
                                RecordStatus estado) {}
