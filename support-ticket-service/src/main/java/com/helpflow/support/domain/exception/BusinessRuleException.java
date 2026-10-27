package com.helpflow.support.domain.exception;

// Regla de negocio incumplida (usuario inactivo, técnico no habilitado...) -> 422
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) { super(message); }
}
