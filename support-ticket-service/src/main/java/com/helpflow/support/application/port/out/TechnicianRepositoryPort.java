package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Technician;
import java.util.List;
import java.util.Optional;

public interface TechnicianRepositoryPort {
    Optional<Technician> findById(Long id);
    Optional<Technician> findByCodigo(String codigo);
    Optional<Technician> findByUsuarioId(Long usuarioId);
    // Activos de la sede cuya especialidad es la categoría u OTRO
    List<Technician> findCandidatos(Category categoria, String sede);
}
