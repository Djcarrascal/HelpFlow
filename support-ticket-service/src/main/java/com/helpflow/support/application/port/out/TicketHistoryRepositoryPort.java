package com.helpflow.support.application.port.out;

import com.helpflow.support.domain.model.TicketHistory;
import java.util.List;

public interface TicketHistoryRepositoryPort {
    TicketHistory save(TicketHistory history);
    List<TicketHistory> findByTicketId(Long ticketId);
}
