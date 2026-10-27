package com.helpflow.support.application.port.in;

public record LoginResult(String token, String tipo, long expiraEnSegundos, String rol) {}
