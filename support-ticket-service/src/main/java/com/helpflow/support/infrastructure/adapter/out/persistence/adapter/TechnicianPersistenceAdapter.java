package com.helpflow.support.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.support.application.port.out.TechnicianRepositoryPort;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.infrastructure.adapter.out.persistence.mapper.TechnicianPersistenceMapper;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.TechnicianJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TechnicianPersistenceAdapter implements TechnicianRepositoryPort {

    private final TechnicianJpaRepository repo;
    private final TechnicianPersistenceMapper mapper;

    public TechnicianPersistenceAdapter(TechnicianJpaRepository repo, TechnicianPersistenceMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override public Optional<Technician> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<Technician> findByCodigo(String codigo) { return repo.findByCodigo(codigo).map(mapper::toDomain); }
    @Override public Optional<Technician> findByUsuarioId(Long usuarioId) { return repo.findByUsuarioId(usuarioId).map(mapper::toDomain); }

    @Override
    public List<Technician> findCandidatos(Category categoria, String sede) {
        return repo.findCandidatos(categoria, sede, RecordStatus.ACTIVO, Category.OTRO)
                .stream().map(mapper::toDomain).toList();
    }
}
