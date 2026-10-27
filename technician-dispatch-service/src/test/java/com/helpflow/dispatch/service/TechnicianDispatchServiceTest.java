package com.helpflow.dispatch.service;

import com.helpflow.dispatch.api.DispatchRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicianDispatchServiceTest {

    private final TechnicianDispatchService service = new TechnicianDispatchService();

    @Test
    void mismaCombinacionDevuelveMismoTecnico() {
        var req = new DispatchRequest(125L, "RED", "ALTA");
        assertThat(service.dispatch(req)).isEqualTo(service.dispatch(req));
    }

    @Test
    void tecnicoPerteneceAlGrupoDeLaCategoria() {
        var res = service.dispatch(new DispatchRequest(125L, "RED", "ALTA"));
        assertThat(res.tecnicoId()).isIn("TEC-009", "TEC-010", "TEC-011", "TEC-012");
    }

    @Test
    void clasificaDisponibilidadSegunHoras() {
        assertThat(service.clasificar(2)).isEqualTo("ALTA");
        assertThat(service.clasificar(3)).isEqualTo("MEDIA");
        assertThat(service.clasificar(9)).isEqualTo("BAJA");
    }
}
