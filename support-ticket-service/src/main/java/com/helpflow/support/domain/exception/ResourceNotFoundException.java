package com.helpflow.support.domain.exception;

// Recurso inexistente -> 404
public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String recurso, Object id) {
        super(recurso + " no encontrado: " + id);
    }
}
