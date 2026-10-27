package com.helpflow.support.domain.exception;

import com.helpflow.support.domain.model.TicketStatus;

// Transición fuera de la máquina de estados -> 409
public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(TicketStatus desde, TicketStatus hacia) {
        super("Transición no permitida: " + desde + " -> " + hacia);
    }
}
