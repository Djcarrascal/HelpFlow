package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.RecordStatus;

// Campos null = no se modifican; estado solo lo cambia el admin
public record UpdateUserCommand(String nombre, String sede, String area, RecordStatus estado) {}
