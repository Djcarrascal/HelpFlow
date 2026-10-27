package com.helpflow.support.infrastructure.adapter.out.persistence.adapter;

import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketStatus;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.helpflow.support.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.TechnicianJpaRepository;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import com.helpflow.support.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class TicketPersistenceAdapter implements TicketRepositoryPort {

    private static final List<TicketStatus> ACTIVOS =
            List.of(TicketStatus.ABIERTO, TicketStatus.ASIGNADO, TicketStatus.EN_PROGRESO);

    private final TicketJpaRepository ticketRepo;
    private final UserJpaRepository userRepo;
    private final TechnicianJpaRepository technicianRepo;
    private final TicketPersistenceMapper mapper;

    public TicketPersistenceAdapter(TicketJpaRepository ticketRepo, UserJpaRepository userRepo,
                                    TechnicianJpaRepository technicianRepo, TicketPersistenceMapper mapper) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.technicianRepo = technicianRepo;
        this.mapper = mapper;
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = mapper.toEntity(ticket);
        entity.setSolicitante(userRepo.getReferenceById(ticket.getSolicitanteId()));
        entity.setTecnico(ticket.getTecnicoId() == null ? null : technicianRepo.getReferenceById(ticket.getTecnicoId()));
        return mapper.toDomain(ticketRepo.save(entity));
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return ticketRepo.findWithTecnicoById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return ticketRepo.findAllByOrderByFechaCreacionDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findBySolicitanteId(Long solicitanteId) {
        return ticketRepo.findBySolicitanteIdOrderByFechaCreacionDesc(solicitanteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByTecnicoId(Long tecnicoId) {
        return ticketRepo.findByTecnicoIdOrderByFechaCreacionDesc(tecnicoId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findActivos() {
        return ticketRepo.findByEstadoInFetchTecnico(ACTIVOS).stream().map(mapper::toDomain).toList();
    }

    // Carga = tickets en curso del técnico
    @Override
    public long countActivosByTecnicoId(Long tecnicoId) {
        return ticketRepo.countByTecnicoIdAndEstadoIn(tecnicoId, List.of(TicketStatus.ASIGNADO, TicketStatus.EN_PROGRESO));
    }

    @Override
    public long countVencidos(LocalDateTime ahora) {
        return ticketRepo.countByEstadoInAndFechaLimiteSlaBefore(ACTIVOS, ahora);
    }
}
