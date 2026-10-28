package com.helpflow.support.infrastructure.adapter.out.metrics;

import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// SimpleMeterRegistry guarda las métricas en memoria: se pueden leer sin Actuator
class MicrometerMetricsAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);

    private SimpleMeterRegistry registry;
    private TicketRepositoryPort tickets;
    private MicrometerMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tickets = mock(TicketRepositoryPort.class);
        adapter = new MicrometerMetricsAdapter(registry, tickets, CLOCK);
    }

    @Test
    void cuentaTicketsCreadosPorCategoriaYPrioridad() {
        adapter.ticketCreado(Category.RED, Priority.ALTA);
        adapter.ticketCreado(Category.RED, Priority.ALTA);
        adapter.ticketCreado(Category.HARDWARE, Priority.BAJA);

        assertThat(registry.get("helpflow.tickets.creados").tags("categoria", "RED", "prioridad", "ALTA").counter().count())
                .isEqualTo(2.0);
        assertThat(registry.get("helpflow.tickets.creados").tags("categoria", "HARDWARE").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void losTimersCalculanElPromedio() {
        adapter.primeraRespuesta(Duration.ofHours(1));
        adapter.primeraRespuesta(Duration.ofHours(3));
        adapter.resolucion(Duration.ofHours(5));

        var primera = registry.get("helpflow.tickets.primera.respuesta").timer();
        assertThat(primera.count()).isEqualTo(2);
        assertThat(primera.mean(TimeUnit.HOURS)).isEqualTo(2.0);
        assertThat(registry.get("helpflow.tickets.resolucion").timer().totalTime(TimeUnit.HOURS)).isEqualTo(5.0);
    }

    @Test
    void elGaugeDeVencidosConsultaElRepositorioAlLeerse() {
        when(tickets.countVencidos(LocalDateTime.now(CLOCK))).thenReturn(3L);

        assertThat(registry.get("helpflow.tickets.vencidos").gauge().value()).isEqualTo(3.0);
    }

    @Test
    void cuentaEscalamientos() {
        adapter.ticketEscalado();
        assertThat(registry.get("helpflow.tickets.escalados").counter().count()).isEqualTo(1.0);
    }
}
