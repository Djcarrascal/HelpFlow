package com.helpflow.support.infrastructure.security;

import com.helpflow.support.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Adaptador del puerto de hashing sobre el PasswordEncoder de Spring (BCrypt)
@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    private final PasswordEncoder encoder;

    public BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override public String hash(String raw) { return encoder.encode(raw); }
    @Override public boolean matches(String raw, String hash) { return encoder.matches(raw, hash); }
}
