package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByCorreo(String correo);
    boolean existsByDocumento(String documento);
    boolean existsByCorreo(String correo);
}
