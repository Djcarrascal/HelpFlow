package com.helpflow.support.domain.exception;

// Documento o correo repetido -> 409
public class DuplicateResourceException extends DomainException {
    public DuplicateResourceException(String message) { super(message); }
}
