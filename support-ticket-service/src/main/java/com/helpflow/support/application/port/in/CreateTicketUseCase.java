package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.Ticket;

public interface CreateTicketUseCase {
    Ticket crear(CreateTicketCommand command, AuthenticatedActor actor);
}
