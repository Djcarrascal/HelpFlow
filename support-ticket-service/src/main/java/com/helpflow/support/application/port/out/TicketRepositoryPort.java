package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(Long id);
    List<Ticket> findAll();
    List<Ticket> findBySolicitanteId(Long solicitanteId);
    List<Ticket> findByTecnicoId(Long tecnicoId);
    List<Ticket> findActivos();
    long countActivosByTecnicoId(Long tecnicoId);
    long countVencidos(LocalDateTime ahora);
}
