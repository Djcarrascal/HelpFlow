package com.helpflow.support.domain.exception;

// Correo o contraseña incorrectos -> 401
public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() { super("Credenciales inválidas"); }
}
