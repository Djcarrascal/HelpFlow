package com.helpflow.support.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import com.helpflow.support.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.TicketHistoryJpaRepository;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketHistoryPersistenceAdapter implements TicketHistoryRepositoryPort {

    private final TicketHistoryJpaRepository historyRepo;
    private final TicketJpaRepository ticketRepo;
    private final TicketPersistenceMapper mapper;

    public TicketHistoryPersistenceAdapter(TicketHistoryJpaRepository historyRepo, TicketJpaRepository ticketRepo,
                                           TicketPersistenceMapper mapper) {
        this.historyRepo = historyRepo;
        this.ticketRepo = ticketRepo;
        this.mapper = mapper;
    }

    @Override
    public TicketHistory save(TicketHistory history) {
        TicketHistoryEntity entity = mapper.toEntity(history);
        entity.setTicket(ticketRepo.getReferenceById(history.getTicketId()));
        return mapper.toDomain(historyRepo.save(entity));
    }

    @Override
    public List<TicketHistory> findByTicketId(Long ticketId) {
        return historyRepo.findByTicketIdOrderByFechaAscIdAsc(ticketId).stream().map(mapper::toDomain).toList();
    }
}
