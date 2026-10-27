package com.helpflow.support.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.support.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.infrastructure.adapter.out.persistence.mapper.SlaPolicyPersistenceMapper;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.SlaPolicyJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SlaPolicyPersistenceAdapter implements SlaPolicyRepositoryPort {

    private final SlaPolicyJpaRepository repo;
    private final SlaPolicyPersistenceMapper mapper;

    public SlaPolicyPersistenceAdapter(SlaPolicyJpaRepository repo, SlaPolicyPersistenceMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override public Optional<SlaPolicy> findByPrioridad(Priority p) { return repo.findById(p.name()).map(mapper::toDomain); }
    @Override public List<SlaPolicy> findAll() { return repo.findAll().stream().map(mapper::toDomain).toList(); }
    @Override public SlaPolicy save(SlaPolicy policy) { return mapper.toDomain(repo.save(mapper.toEntity(policy))); }
}
