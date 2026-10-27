package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;

public interface TechnicianDispatchPort {

    // Respuesta de technician-dispatch-service ya traducida al lenguaje del dominio
    record DispatchSuggestion(String tecnicoCodigo, String disponibilidad, int tiempoEstimadoHoras) {}

    DispatchSuggestion sugerir(Long ticketId, Category categoria, Priority prioridad);
}
