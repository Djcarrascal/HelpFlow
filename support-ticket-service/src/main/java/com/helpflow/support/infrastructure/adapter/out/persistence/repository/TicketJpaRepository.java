package com.helpflow.support.infrastructure.adapter.out.persistence.repository;

import com.helpflow.support.domain.model.TicketStatus;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {

    // @EntityGraph: el técnico llega en la misma consulta (sin N+1)
    @EntityGraph(attributePaths = "tecnico")
    List<TicketEntity> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = "tecnico")
    List<TicketEntity> findBySolicitanteIdOrderByFechaCreacionDesc(Long solicitanteId);

    @EntityGraph(attributePaths = "tecnico")
    List<TicketEntity> findByTecnicoIdOrderByFechaCreacionDesc(Long tecnicoId);

    @EntityGraph(attributePaths = "tecnico")
    Optional<TicketEntity> findWithTecnicoById(Long id);

    // join fetch: alternativa en JPQL para la tarea de escalamiento
    @Query("select t from TicketEntity t left join fetch t.tecnico where t.estado in :estados")
    List<TicketEntity> findByEstadoInFetchTecnico(@Param("estados") Collection<TicketStatus> estados);

    long countByTecnicoIdAndEstadoIn(Long tecnicoId, Collection<TicketStatus> estados);

    long countByEstadoInAndFechaLimiteSlaBefore(Collection<TicketStatus> estados, LocalDateTime fecha);
}
