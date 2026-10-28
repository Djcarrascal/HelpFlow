package com.helpflow.support.application.service;

import com.helpflow.support.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.SlaPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaConfigServiceTest {

    @Mock SlaPolicyRepositoryPort politicas;
    @InjectMocks SlaConfigService service;

    @Test
    void listaLasPoliticas() {
        when(politicas.findAll()).thenReturn(List.of(new SlaPolicy(Priority.ALTA, 8)));
        assertThat(service.listar()).hasSize(1);
    }

    @Test
    void actualizaLasHorasDeUnaPrioridad() {
        when(politicas.findByPrioridad(Priority.ALTA)).thenReturn(Optional.of(new SlaPolicy(Priority.ALTA, 8)));
        when(politicas.save(any(SlaPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(Priority.ALTA, 6);

        ArgumentCaptor<SlaPolicy> captor = ArgumentCaptor.forClass(SlaPolicy.class);
        verify(politicas).save(captor.capture());
        assertThat(captor.getValue().getHorasLimite()).isEqualTo(6);
        assertThat(captor.getValue().getPrioridad()).isEqualTo(Priority.ALTA);
    }

    @Test
    void prioridadSinPolitica() {
        when(politicas.findByPrioridad(Priority.BAJA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(Priority.BAJA, 10)).isInstanceOf(ResourceNotFoundException.class);
        verify(politicas, never()).save(any());
    }
}
