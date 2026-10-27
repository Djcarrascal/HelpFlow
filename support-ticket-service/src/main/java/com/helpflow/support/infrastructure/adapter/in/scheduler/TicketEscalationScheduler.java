package com.helpflow.support.infrastructure.adapter.in.scheduler;

import com.helpflow.support.application.port.in.ManageTicketUseCase;
import com.helpflow.support.infrastructure.error.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TicketEscalationScheduler {

    private final ManageTicketUseCase manageTicket;

    public TicketEscalationScheduler(ManageTicketUseCase manageTicket) {
        this.manageTicket = manageTicket;
    }

    // Cron configurable en application.yml (por defecto cada 5 minutos)
    @Scheduled(cron = "${helpflow.escalamiento.cron}")
    public void escalar() {
        // traceId propio para encontrar en los logs cada ejecución de la tarea
        MDC.put(TraceIdFilter.TRACE_ID, "job-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            manageTicket.escalarProximosAVencer();
        } finally {
            MDC.remove(TraceIdFilter.TRACE_ID);
        }
    }
}
