package com.helpflow.support.infrastructure.adapter.in.rest.mapper;

import com.helpflow.support.application.port.in.CreateTicketCommand;
import com.helpflow.support.application.port.in.UpdateUserCommand;
import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.CreateTicketRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.SlaResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.TicketHistoryResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.TicketResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.UpdateUserRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface TicketWebMapper {
    CreateTicketCommand toCommand(CreateTicketRequest request);
    UpdateUserCommand toCommand(UpdateUserRequest request);
    TicketResponse toResponse(Ticket ticket);
    List<TicketResponse> toResponses(List<Ticket> tickets);
    TicketHistoryResponse toResponse(TicketHistory history);
    List<TicketHistoryResponse> toHistoryResponses(List<TicketHistory> history);
    SlaResponse toResponse(SlaPolicy policy);
    List<SlaResponse> toSlaResponses(List<SlaPolicy> policies);
}
