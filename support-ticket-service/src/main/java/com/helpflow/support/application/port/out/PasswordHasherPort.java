package com.helpflow.support.application.port.out;

public interface PasswordHasherPort {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
