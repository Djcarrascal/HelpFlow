package com.helpflow.support.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.support.application.port.out.UserRepositoryPort;
import com.helpflow.support.domain.model.User;
import com.helpflow.support.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repo;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repo, UserPersistenceMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override public User save(User user) { return mapper.toDomain(repo.save(mapper.toEntity(user))); }
    @Override public Optional<User> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<User> findByCorreo(String correo) { return repo.findByCorreo(correo).map(mapper::toDomain); }
    @Override public boolean existsByDocumento(String documento) { return repo.existsByDocumento(documento); }
    @Override public boolean existsByCorreo(String correo) { return repo.existsByCorreo(correo); }
}
