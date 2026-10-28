package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.out.DispatchUnavailableException;
import com.helpflow.support.application.port.out.MetricsPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort.DispatchSuggestion;
import com.helpflow.support.application.port.out.TechnicianRepositoryPort;
import com.helpflow.support.application.port.out.TicketHistoryRepositoryPort;
import com.helpflow.support.application.port.out.TicketRepositoryPort;
import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.exception.InvalidStatusTransitionException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.HistoryAction;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.domain.model.Ticket;
import com.helpflow.support.domain.model.TicketHistory;
import com.helpflow.support.domain.model.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageTicketServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(CLOCK);
    private static final AuthenticatedActor ADMIN = new AuthenticatedActor(1L, "admin@helpflow.com", Role.ROLE_ADMIN);

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryRepositoryPort historial;
    @Mock TechnicianRepositoryPort tecnicos;
    @Mock TechnicianDispatchPort dispatch;
    @Mock MetricsPort metrics;
    @Mock TicketAccessChecker acceso;

    // Captura lo que el caso de uso escribe en el historial para revisarlo en detalle
    @Captor ArgumentCaptor<TicketHistory> historialCaptor;

    private ManageTicketService service;

    @BeforeEach
    void setUp() {
        service = new ManageTicketService(tickets, historial, tecnicos, dispatch, metrics, acceso, CLOCK);
    }

    private Ticket ticketAbierto(LocalDateTime creacion) {
        Ticket t = Ticket.abrir(100L, "VPN caída", "No conecta la VPN", Category.RED, Priority.ALTA,
                "BARRANQUILLA", "ACT-1", new SlaPolicy(Priority.ALTA, 8), creacion);
        t.setId(1L);
        return t;
    }

    private Ticket ticketAsignado(LocalDateTime creacion) {
        Ticket t = ticketAbierto(creacion);
        t.asignar(tecnico(9L, "TEC-009", RecordStatus.ACTIVO));
        return t;
    }

    private Technician tecnico(long id, String codigo, RecordStatus estado) {
        return tecnico(id, codigo, estado, Category.RED, "BARRANQUILLA");
    }

    private Technician tecnico(long id, String codigo, RecordStatus estado, Category especialidad, String sede) {
        Technician t = new Technician();
        t.setId(id);
        t.setCodigo(codigo);
        t.setEspecialidad(especialidad);
        t.setSede(sede);
        t.setCargaMaxima(5);
        t.setEstado(estado);
        return t;
    }

    private void guardarDevuelveElMismoTicket() {
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class AsignacionAutomatica {

        @Test
        void asignaAlTecnicoSugeridoCuandoCumpleLasReglas() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            when(dispatch.sugerir(1L, Category.RED, Priority.ALTA)).thenReturn(new DispatchSuggestion("TEC-009", "ALTA", 2));
            when(tecnicos.findByCodigo("TEC-009")).thenReturn(Optional.of(tecnico(9L, "TEC-009", RecordStatus.ACTIVO)));
            guardarDevuelveElMismoTicket();

            Ticket resultado = service.asignarAutomaticamente(1L, "SISTEMA");

            assertThat(resultado.getEstado()).isEqualTo(TicketStatus.ASIGNADO);
            assertThat(resultado.getTecnicoCodigo()).isEqualTo("TEC-009");
            // El ticket se guarda antes de escribir el historial
            InOrder orden = inOrder(tickets, historial);
            orden.verify(tickets).save(resultado);
            orden.verify(historial).save(historialCaptor.capture());
            TicketHistory h = historialCaptor.getValue();
            assertThat(h.getAccion()).isEqualTo(HistoryAction.ASIGNADO);
            assertThat(h.getEstadoAnterior()).isEqualTo(TicketStatus.ABIERTO);
            assertThat(h.getEstadoNuevo()).isEqualTo(TicketStatus.ASIGNADO);
            assertThat(h.getObservacion()).contains("Sugerido por dispatch");
            assertThat(h.getResponsable()).isEqualTo("SISTEMA");
        }

        @Test
        void usaTecnicoAlternativoSiElSugeridoEstaInactivo() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            when(dispatch.sugerir(1L, Category.RED, Priority.ALTA)).thenReturn(new DispatchSuggestion("TEC-009", "ALTA", 2));
            when(tecnicos.findByCodigo("TEC-009")).thenReturn(Optional.of(tecnico(9L, "TEC-009", RecordStatus.INACTIVO)));
            when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA"))
                    .thenReturn(List.of(tecnico(10L, "TEC-010", RecordStatus.ACTIVO)));
            guardarDevuelveElMismoTicket();

            Ticket resultado = service.asignarAutomaticamente(1L, "SISTEMA");

            assertThat(resultado.getTecnicoCodigo()).isEqualTo("TEC-010");
            verify(historial).save(historialCaptor.capture());
            assertThat(historialCaptor.getValue().getObservacion())
                    .contains("inactivo").contains("alternativo TEC-010");
        }

        @Test
        void usaAlternativoSiElSugeridoNoExiste() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            when(dispatch.sugerir(1L, Category.RED, Priority.ALTA)).thenReturn(new DispatchSuggestion("TEC-999", "ALTA", 2));
            when(tecnicos.findByCodigo("TEC-999")).thenReturn(Optional.empty());
            when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA"))
                    .thenReturn(List.of(tecnico(10L, "TEC-010", RecordStatus.ACTIVO)));
            guardarDevuelveElMismoTicket();

            assertThat(service.asignarAutomaticamente(1L, "SISTEMA").getTecnicoCodigo()).isEqualTo("TEC-010");
        }

        @Test
        void usaAlternativoSiElTiempoEstimadoIncumpleElSla() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            // 9 h estimadas con un SLA de 8 h
            when(dispatch.sugerir(1L, Category.RED, Priority.ALTA)).thenReturn(new DispatchSuggestion("TEC-009", "BAJA", 9));
            when(tecnicos.findByCodigo("TEC-009")).thenReturn(Optional.of(tecnico(9L, "TEC-009", RecordStatus.ACTIVO)));
            when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA"))
                    .thenReturn(List.of(tecnico(10L, "TEC-010", RecordStatus.ACTIVO)));
            guardarDevuelveElMismoTicket();

            assertThat(service.asignarAutomaticamente(1L, "SISTEMA").getTecnicoCodigo()).isEqualTo("TEC-010");
        }

        @Test
        void elAlternativoEsElDeMenorCarga() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            when(dispatch.sugerir(anyLong(), any(), any())).thenThrow(new DispatchUnavailableException("caído", null));
            when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA")).thenReturn(List.of(
                    tecnico(10L, "TEC-010", RecordStatus.ACTIVO),
                    tecnico(17L, "TEC-017", RecordStatus.ACTIVO, Category.OTRO, "BARRANQUILLA"),
                    tecnico(18L, "TEC-018", RecordStatus.ACTIVO, Category.OTRO, "BARRANQUILLA")));
            when(tickets.countActivosByTecnicoId(10L)).thenReturn(3L);
            when(tickets.countActivosByTecnicoId(17L)).thenReturn(1L);
            when(tickets.countActivosByTecnicoId(18L)).thenReturn(5L);    // lleno: se descarta
            guardarDevuelveElMismoTicket();

            assertThat(service.asignarAutomaticamente(1L, "SISTEMA").getTecnicoCodigo()).isEqualTo("TEC-017");
        }

        @Test
        void dejaElTicketAbiertoSiDispatchFallaYNoHayAlternativos() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            when(dispatch.sugerir(anyLong(), any(), any())).thenThrow(new DispatchUnavailableException("caído", null));
            when(tecnicos.findCandidatos(Category.RED, "BARRANQUILLA")).thenReturn(List.of());

            Ticket resultado = service.asignarAutomaticamente(1L, "SISTEMA");

            assertThat(resultado.getEstado()).isEqualTo(TicketStatus.ABIERTO);
            verify(tickets, never()).save(any());
            verify(historial).save(historialCaptor.capture());
            assertThat(historialCaptor.getValue().getAccion()).isEqualTo(HistoryAction.SIN_TECNICO);
            assertThat(historialCaptor.getValue().getObservacion()).contains("no disponible");
        }

        @Test
        void ticketInexistenteLanzaNoEncontrado() {
            when(tickets.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.asignarAutomaticamente(99L, "SISTEMA"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(dispatch, historial);
        }
    }

    @Nested
    class CambioDeEstado {

        @Test
        void rechazaTransicionInvalida() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));

            assertThatThrownBy(() -> service.cambiarEstado(1L, TicketStatus.RESUELTO, "listo", ADMIN))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            verify(tickets, never()).save(any());
            verifyNoInteractions(historial, metrics);
        }

        @Test
        void siNoTieneAccesoNoCambiaNada() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAsignado(AHORA)));
            var otroTecnico = new AuthenticatedActor(50L, "tec013@helpflow.com", Role.ROLE_TECNICO);
            doThrow(new ForbiddenOperationException("ajeno")).when(acceso).verificarGestion(any(), any());

            assertThatThrownBy(() -> service.cambiarEstado(1L, TicketStatus.EN_PROGRESO, null, otroTecnico))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(tickets, never()).save(any());
        }

        @Test
        void primerEnProgresoRegistraLaMetricaDePrimeraRespuesta() {
            Ticket ticket = ticketAsignado(AHORA.minusHours(2));
            when(tickets.findById(1L)).thenReturn(Optional.of(ticket));
            guardarDevuelveElMismoTicket();

            service.cambiarEstado(1L, TicketStatus.EN_PROGRESO, null, ADMIN);

            // Orden: guardar, historial y luego métrica
            InOrder orden = inOrder(tickets, historial, metrics);
            orden.verify(tickets).save(ticket);
            orden.verify(historial).save(historialCaptor.capture());
            orden.verify(metrics).primeraRespuesta(Duration.ofHours(2));
            assertThat(historialCaptor.getValue().getAccion()).isEqualTo(HistoryAction.CAMBIO_ESTADO);
            assertThat(historialCaptor.getValue().getResponsable()).isEqualTo("admin@helpflow.com");
            verify(metrics, never()).resolucion(any());
        }

        @Test
        void resolverRegistraLaMetricaDeResolucion() {
            Ticket ticket = ticketAsignado(AHORA.minusHours(5));
            ticket.cambiarEstado(TicketStatus.EN_PROGRESO, AHORA.minusHours(4));
            when(tickets.findById(1L)).thenReturn(Optional.of(ticket));
            guardarDevuelveElMismoTicket();

            service.cambiarEstado(1L, TicketStatus.RESUELTO, "Se reinició el router", ADMIN);

            verify(metrics).resolucion(Duration.ofHours(5));
            verify(metrics, never()).primeraRespuesta(any());
        }

        @Test
        void laReaperturaNoDuplicaLaPrimeraRespuesta() {
            Ticket ticket = ticketAsignado(AHORA.minusHours(5));
            ticket.cambiarEstado(TicketStatus.EN_PROGRESO, AHORA.minusHours(4));
            ticket.cambiarEstado(TicketStatus.RESUELTO, AHORA.minusHours(1));
            when(tickets.findById(1L)).thenReturn(Optional.of(ticket));
            guardarDevuelveElMismoTicket();

            service.cambiarEstado(1L, TicketStatus.EN_PROGRESO, "El usuario reporta que falla de nuevo", ADMIN);

            verify(metrics, never()).primeraRespuesta(any());
        }
    }

    @Nested
    class Reasignacion {

        @Test
        void reasignaUnTicketAsignadoYDejaHistorial() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAsignado(AHORA)));
            when(tecnicos.findByCodigo("TEC-018"))
                    .thenReturn(Optional.of(tecnico(18L, "TEC-018", RecordStatus.ACTIVO, Category.OTRO, "BARRANQUILLA")));
            guardarDevuelveElMismoTicket();

            Ticket resultado = service.reasignar(1L, "TEC-018", "Balanceo de carga", ADMIN);

            assertThat(resultado.getTecnicoCodigo()).isEqualTo("TEC-018");
            assertThat(resultado.getEstado()).isEqualTo(TicketStatus.ASIGNADO);
            verify(historial).save(historialCaptor.capture());
            assertThat(historialCaptor.getValue().getAccion()).isEqualTo(HistoryAction.REASIGNADO);
            assertThat(historialCaptor.getValue().getObservacion()).isEqualTo("Balanceo de carga");
        }

        @Test
        void reasignarUnTicketAbiertoLoAsigna() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAbierto(AHORA)));
            when(tecnicos.findByCodigo("TEC-010")).thenReturn(Optional.of(tecnico(10L, "TEC-010", RecordStatus.ACTIVO)));
            guardarDevuelveElMismoTicket();

            assertThat(service.reasignar(1L, "TEC-010", "Asignación manual", ADMIN).getEstado())
                    .isEqualTo(TicketStatus.ASIGNADO);
        }

        @Test
        void tecnicoInexistente() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAsignado(AHORA)));
            when(tecnicos.findByCodigo("TEC-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reasignar(1L, "TEC-999", "x", ADMIN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void tecnicoInactivoNoEstaHabilitado() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAsignado(AHORA)));
            when(tecnicos.findByCodigo("TEC-004"))
                    .thenReturn(Optional.of(tecnico(4L, "TEC-004", RecordStatus.INACTIVO, Category.HARDWARE, "MEDELLIN")));

            assertThatThrownBy(() -> service.reasignar(1L, "TEC-004", "x", ADMIN))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no habilitado");
            verify(tickets, never()).save(any());
        }

        @Test
        void especialidadIncompatibleNoEstaHabilitada() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAsignado(AHORA)));
            when(tecnicos.findByCodigo("TEC-001"))
                    .thenReturn(Optional.of(tecnico(1L, "TEC-001", RecordStatus.ACTIVO, Category.HARDWARE, "BARRANQUILLA")));

            assertThatThrownBy(() -> service.reasignar(1L, "TEC-001", "x", ADMIN))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void tecnicoEnCargaMaxima() {
            when(tickets.findById(1L)).thenReturn(Optional.of(ticketAsignado(AHORA)));
            when(tecnicos.findByCodigo("TEC-010")).thenReturn(Optional.of(tecnico(10L, "TEC-010", RecordStatus.ACTIVO)));
            when(tickets.countActivosByTecnicoId(10L)).thenReturn(5L);

            assertThatThrownBy(() -> service.reasignar(1L, "TEC-010", "x", ADMIN))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("carga máxima");
        }
    }

    @Nested
    class Escalamiento {

        @Test
        void escalaTicketsProximosAVencer() {
            Ticket casiVencido = ticketAbierto(AHORA.minusMinutes(450));   // le quedan 30 min de 8 h
            when(tickets.findActivos()).thenReturn(List.of(casiVencido));
            guardarDevuelveElMismoTicket();

            int escalados = service.escalarProximosAVencer();

            assertThat(escalados).isEqualTo(1);
            assertThat(casiVencido.getPrioridad()).isEqualTo(Priority.CRITICA);
            verify(historial).save(historialCaptor.capture());
            assertThat(historialCaptor.getValue().getObservacion()).isEqualTo("Prioridad ALTA -> CRITICA por proximidad al SLA");
            assertThat(historialCaptor.getValue().getResponsable()).isEqualTo("SISTEMA");
            verify(metrics).ticketEscalado();
        }

        @Test
        void noEscalaTicketsConTiempoSuficienteNiVencidos() {
            when(tickets.findActivos()).thenReturn(List.of(
                    ticketAbierto(AHORA.minusHours(1)),      // con tiempo
                    ticketAbierto(AHORA.minusHours(9))));    // ya vencido

            assertThat(service.escalarProximosAVencer()).isZero();
            verify(tickets, never()).save(any());
            verifyNoInteractions(metrics);
        }
    }
}
