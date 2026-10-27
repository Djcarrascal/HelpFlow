package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.SlaPolicy;
import java.util.List;

public interface SlaConfigUseCase {
    List<SlaPolicy> listar();
    SlaPolicy actualizar(Priority prioridad, int horasLimite);
}
