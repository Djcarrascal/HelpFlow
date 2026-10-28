package com.helpflow.dispatch.service;

import com.helpflow.dispatch.api.DispatchRequest;
import com.helpflow.dispatch.api.DispatchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicianDispatchServiceTest {

    private final TechnicianDispatchService service = new TechnicianDispatchService();

    @Test
    void mismaCombinacionDevuelveMismoTecnico() {
        var req = new DispatchRequest(125L, "RED", "ALTA");
        assertThat(service.dispatch(req)).isEqualTo(service.dispatch(req));
    }

    @Test
    void elEjemploDeReferenciaEsEstable() {
        // Valor fijo: si cambia, cambió el algoritmo y los datos de prueba dejan de coincidir
        assertThat(service.dispatch(new DispatchRequest(125L, "RED", "ALTA")))
                .isEqualTo(new DispatchResponse(125L, "TEC-011", "MEDIA", 5));
    }

    // Cada categoría usa su grupo de 4 técnicos y cada prioridad su tope de horas
    @ParameterizedTest(name = "{0}/{1} -> TEC-{2} a TEC-{3}, máx {4} h")
    @CsvSource({
            "HARDWARE, CRITICA, 1, 4, 4",
            "SOFTWARE, ALTA, 5, 8, 8",
            "RED, MEDIA, 9, 12, 16",
            "ACCESOS, BAJA, 13, 16, 24",
            "OTRO, ALTA, 17, 20, 8"
    })
    void respetaElGrupoDeLaCategoriaYElTopeDeLaPrioridad(String categoria, String prioridad,
                                                        int primero, int ultimo, int maxHoras) {
        for (long ticketId = 1; ticketId <= 50; ticketId++) {
            DispatchResponse r = service.dispatch(new DispatchRequest(ticketId, categoria, prioridad));
            int numero = Integer.parseInt(r.tecnicoId().substring(4));
            assertThat(numero).isBetween(primero, ultimo);
            assertThat(r.tiempoEstimadoHoras()).isBetween(0, maxHoras);
            assertThat(r.disponibilidad()).isEqualTo(service.clasificar(r.tiempoEstimadoHoras()));
        }
    }

    @Test
    void combinacionesDistintasPuedenDarTecnicosDistintos() {
        long distintos = java.util.stream.LongStream.rangeClosed(1, 30)
                .mapToObj(id -> service.dispatch(new DispatchRequest(id, "RED", "ALTA")).tecnicoId())
                .distinct().count();
        assertThat(distintos).isGreaterThan(1);
    }

    @ParameterizedTest(name = "{0} h -> {1}")
    @CsvSource({"0, ALTA", "2, ALTA", "3, MEDIA", "8, MEDIA", "9, BAJA", "24, BAJA"})
    void clasificaDisponibilidadEnLosBordes(int horas, String esperado) {
        assertThat(service.clasificar(horas)).isEqualTo(esperado);
    }
}
