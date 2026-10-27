package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import java.time.Duration;

// Métricas de negocio sin acoplar los casos de uso a Micrometer
public interface MetricsPort {
    void ticketCreado(Category categoria, Priority prioridad);
    void primeraRespuesta(Duration tiempo);
    void resolucion(Duration tiempo);
    void ticketEscalado();
}
