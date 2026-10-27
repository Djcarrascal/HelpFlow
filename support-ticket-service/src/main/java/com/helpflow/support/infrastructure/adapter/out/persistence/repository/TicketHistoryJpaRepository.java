package com.helpflow.support.infrastructure.adapter.out.persistence.repository;

import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryJpaRepository extends JpaRepository<TicketHistoryEntity, Long> {
    // id como desempate: dos eventos pueden tener la misma fecha
    List<TicketHistoryEntity> findByTicketIdOrderByFechaAscIdAsc(Long ticketId);
}
