package com.helpflow.support.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TicketPersistenceMapper {

    @Mapping(target = "solicitanteId", source = "solicitante.id")
    @Mapping(target = "tecnicoId", source = "tecnico.id")
    @Mapping(target = "tecnicoCodigo", source = "tecnico.codigo")
    Ticket toDomain(TicketEntity entity);

    // Las relaciones las resuelve el adaptador con getReferenceById
    @Mapping(target = "solicitante", ignore = true)
    @Mapping(target = "tecnico", ignore = true)
    @Mapping(target = "historial", ignore = true)
    TicketEntity toEntity(Ticket ticket);

    @Mapping(target = "ticketId", source = "ticket.id")
    TicketHistory toDomain(TicketHistoryEntity entity);

    @Mapping(target = "ticket", ignore = true)
    TicketHistoryEntity toEntity(TicketHistory history);
}
