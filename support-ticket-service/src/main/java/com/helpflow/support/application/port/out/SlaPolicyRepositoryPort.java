package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.SlaPolicy;
import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepositoryPort {
    Optional<SlaPolicy> findByPrioridad(Priority prioridad);
    List<SlaPolicy> findAll();
    SlaPolicy save(SlaPolicy policy);
}
