package com.helpflow.support.infrastructure.adapter.in.rest.dto;

// Nunca se expone el hash de la contraseña
public record UserResponse(Long id, String documento, String nombre, String correo,
                           String sede, String area, String estado, String rol) {}
