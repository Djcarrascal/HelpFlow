package com.helpflow.support.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "\\d{6,15}", message = "debe tener entre 6 y 15 dígitos")
        String documento,
        @NotBlank @Size(max = 120)
        String nombre,
        @NotBlank @Email
        @Pattern(regexp = "^[\\w.+-]+@helpflow\\.com$", message = "debe ser un correo corporativo @helpflow.com")
        String correo,
        @NotBlank @Size(min = 8, max = 64, message = "debe tener entre 8 y 64 caracteres")
        String password,
        @NotBlank @Size(max = 60)
        String sede,
        @NotBlank @Size(max = 60)
        String area
) {}
