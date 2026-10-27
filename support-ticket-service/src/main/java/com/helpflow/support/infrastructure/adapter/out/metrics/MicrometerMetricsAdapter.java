package com.helpflow.support.infrastructure.adapter.out.metrics;

import com.helpflow.support.application.port.out.MetricsPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private final MeterRegistry registry;
    private final Timer primeraRespuesta;
    private final Timer resolucion;

    public MicrometerMetricsAdapter(MeterRegistry registry, TicketRepositoryPort tickets, Clock clock) {
        this.registry = registry;
        this.primeraRespuesta = Timer.builder("helpflow.tickets.primera.respuesta")
                .description("Tiempo desde la creación hasta que el técnico inicia")
                .register(registry);
        this.resolucion = Timer.builder("helpflow.tickets.resolucion")
                .description("Tiempo desde la creación hasta RESUELTO")
                .register(registry);
        // Gauge: consulta la base cada vez que alguien lee la métrica
        Gauge.builder("helpflow.tickets.vencidos", tickets, repo -> repo.countVencidos(LocalDateTime.now(clock)))
                .description("Tickets activos con SLA vencido")
                .register(registry);
    }

    @Override
    public void ticketCreado(Category categoria, Priority prioridad) {
        registry.counter("helpflow.tickets.creados",
                "categoria", categoria.name(), "prioridad", prioridad.name()).increment();
    }

    @Override
    public void primeraRespuesta(Duration tiempo) { primeraRespuesta.record(tiempo); }

    @Override
    public void resolucion(Duration tiempo) { resolucion.record(tiempo); }

    @Override
    public void ticketEscalado() { registry.counter("helpflow.tickets.escalados").increment(); }
}
