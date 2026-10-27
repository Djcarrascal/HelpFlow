package com.helpflow.support.infrastructure.adapter.out.dispatch;

import com.helpflow.support.application.port.out.DispatchUnavailableException;
import com.helpflow.support.application.port.out.TechnicianDispatchPort;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class TechnicianDispatchRestAdapter implements TechnicianDispatchPort {

    // Contrato JSON del otro microservicio (solo vive en este adaptador)
    record DispatchRequest(Long ticketId, String categoria, String prioridad) {}
    record DispatchResponse(Long ticketId, String tecnicoId, String disponibilidad, int tiempoEstimadoHoras) {}

    private final RestClient restClient;

    public TechnicianDispatchRestAdapter(RestClient.Builder builder,
                                         @Value("${helpflow.dispatch.base-url}") String baseUrl) {
        // Timeouts cortos: un servicio caído no debe bloquear la creación de tickets
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public DispatchSuggestion sugerir(Long ticketId, Category categoria, Priority prioridad) {
        try {
            DispatchResponse respuesta = restClient.post()
                    .uri("/technician-dispatch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DispatchRequest(ticketId, categoria.name(), prioridad.name()))
                    .retrieve()
                    .body(DispatchResponse.class);
            if (respuesta == null) {
                throw new DispatchUnavailableException("Respuesta vacía de dispatch", null);
            }
            return new DispatchSuggestion(respuesta.tecnicoId(), respuesta.disponibilidad(),
                    respuesta.tiempoEstimadoHoras());
        } catch (RestClientException e) {
            throw new DispatchUnavailableException(e.getMessage(), e);
        }
    }
}
