package com.helpflow.support.infrastructure.adapter.out.persistence.repository;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TechnicianJpaRepository extends JpaRepository<TechnicianEntity, Long> {
    Optional<TechnicianEntity> findByCodigo(String codigo);

    // Spring Data navega usuario.id a partir del nombre del método
    Optional<TechnicianEntity> findByUsuarioId(Long usuarioId);

    @Query("""
           select t from TechnicianEntity t
           where t.estado = :activo
             and lower(t.sede) = lower(:sede)
             and (t.especialidad = :categoria or t.especialidad = :generalista)
           """)
    List<TechnicianEntity> findCandidatos(@Param("categoria") Category categoria,
                                          @Param("sede") String sede,
                                          @Param("activo") RecordStatus activo,
                                          @Param("generalista") Category generalista);
}
