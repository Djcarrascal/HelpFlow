package com.helpflow.support.domain.exception;

// Ticket ajeno al usuario o técnico autenticado -> 403
public class ForbiddenOperationException extends DomainException {
    public ForbiddenOperationException(String message) { super(message); }
}
