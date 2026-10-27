package com.helpflow.support.application.port.out;

// Se lanza cuando dispatch no responde; el caso de uso decide qué hacer
public class DispatchUnavailableException extends RuntimeException {
    public DispatchUnavailableException(String message, Throwable cause) { super(message, cause); }
}
