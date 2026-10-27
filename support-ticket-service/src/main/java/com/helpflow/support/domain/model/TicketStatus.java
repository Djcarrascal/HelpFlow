package com.helpflow.support.domain.model;

public enum TicketStatus {
    ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO;

    // Máquina de estados: única fuente de verdad de las transiciones válidas
    public boolean puedePasarA(TicketStatus destino) {
        return switch (this) {
            case ABIERTO -> destino == ASIGNADO || destino == CANCELADO;
            case ASIGNADO -> destino == EN_PROGRESO || destino == CANCELADO;
            case EN_PROGRESO -> destino == RESUELTO;
            case RESUELTO -> destino == CERRADO || destino == EN_PROGRESO;
            case CERRADO, CANCELADO -> false;
        };
    }

    // Estados que cuentan como carga del técnico y para el SLA
    public boolean esActivo() {
        return this == ABIERTO || this == ASIGNADO || this == EN_PROGRESO;
    }
}
