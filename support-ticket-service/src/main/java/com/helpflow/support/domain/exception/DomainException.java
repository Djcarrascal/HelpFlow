package com.helpflow.support.domain.exception;

// Base de todas las excepciones del dominio
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
}
