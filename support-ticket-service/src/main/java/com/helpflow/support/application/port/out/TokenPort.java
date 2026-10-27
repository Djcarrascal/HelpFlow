package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.User;

public interface TokenPort {
    String generar(User user);
    long expiracionSegundos();
}
