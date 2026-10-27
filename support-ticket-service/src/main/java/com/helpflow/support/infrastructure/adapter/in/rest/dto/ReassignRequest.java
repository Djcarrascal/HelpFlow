package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReassignRequest(
        @NotBlank @Pattern(regexp = "TEC-\\d{3}", message = "formato TEC-000") String tecnicoCodigo,
        @NotBlank @Size(max = 500) String observacion
) {}
