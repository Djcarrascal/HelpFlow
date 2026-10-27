package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.SlaConfigUseCase;
import com.helpflow.support.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.SlaPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SlaConfigService implements SlaConfigUseCase {

    private final SlaPolicyRepositoryPort politicas;

    public SlaConfigService(SlaPolicyRepositoryPort politicas) {
        this.politicas = politicas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicy> listar() { return politicas.findAll(); }

    // Aplica a tickets nuevos; los existentes conservan su fecha límite
    @Override
    public SlaPolicy actualizar(Priority prioridad, int horasLimite) {
        SlaPolicy p = politicas.findByPrioridad(prioridad)
                .orElseThrow(() -> new ResourceNotFoundException("Política SLA", prioridad));
        p.setHorasLimite(horasLimite);
        return politicas.save(p);
    }
}
