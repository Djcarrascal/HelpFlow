package com.helpflow.support.infrastructure.adapter.in.scheduler;

import com.helpflow.support.application.port.in.ManageTicketUseCase;
import com.helpflow.support.infrastructure.error.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketEscalationSchedulerTest {

    @Test
    void ejecutaElEscalamientoConUnTraceIdPropioYLoLimpia() {
        ManageTicketUseCase manage = mock(ManageTicketUseCase.class);
        AtomicReference<String> traceDuranteLaTarea = new AtomicReference<>();
        when(manage.escalarProximosAVencer()).thenAnswer(inv -> {
            traceDuranteLaTarea.set(MDC.get(TraceIdFilter.TRACE_ID));
            return 2;
        });

        new TicketEscalationScheduler(manage).escalar();

        verify(manage).escalarProximosAVencer();
        assertThat(traceDuranteLaTarea.get()).startsWith("job-");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }

    @Test
    void limpiaElTraceIdAunqueLaTareaFalle() {
        ManageTicketUseCase manage = mock(ManageTicketUseCase.class);
        when(manage.escalarProximosAVencer()).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> new TicketEscalationScheduler(manage).escalar()).isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }
}
