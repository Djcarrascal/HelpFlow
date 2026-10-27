package com.helpflow.dispatch.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Body de entrada; los patrones limitan los valores a los enums del enunciado
public record DispatchRequest(
        @NotNull @Positive Long ticketId,
        @NotBlank @Pattern(regexp = "HARDWARE|SOFTWARE|RED|ACCESOS|OTRO") String categoria,
        @NotBlank @Pattern(regexp = "BAJA|MEDIA|ALTA|CRITICA") String prioridad
) {}
