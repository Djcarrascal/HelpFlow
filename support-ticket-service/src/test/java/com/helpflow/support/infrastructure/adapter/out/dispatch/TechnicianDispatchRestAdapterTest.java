package com.helpflow.support.infrastructure.adapter.out.dispatch;

import com.helpflow.support.application.port.out.DispatchUnavailableException;
import com.helpflow.support.application.port.out.TechnicianDispatchPort.DispatchSuggestion;
import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Servidor HTTP del JDK que simula technician-dispatch-service: prueba el adaptador real, con red
class TechnicianDispatchRestAdapterTest {

    private HttpServer server;
    private final AtomicReference<String> bodyRecibido = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String respuesta = """
            {"ticketId":125,"tecnicoId":"TEC-011","disponibilidad":"MEDIA","tiempoEstimadoHoras":5}""";

    @BeforeEach
    void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/technician-dispatch", exchange -> {
            bodyRecibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void detenerServidor() {
        server.stop(0);
    }

    private TechnicianDispatchRestAdapter adapter() {
        return new TechnicianDispatchRestAdapter(RestClient.builder(), "http://localhost:" + server.getAddress().getPort());
    }

    @Test
    void traduceLaRespuestaDeDispatchAlLenguajeDelDominio() {
        DispatchSuggestion s = adapter().sugerir(125L, Category.RED, Priority.ALTA);

        assertThat(s.tecnicoCodigo()).isEqualTo("TEC-011");
        assertThat(s.disponibilidad()).isEqualTo("MEDIA");
        assertThat(s.tiempoEstimadoHoras()).isEqualTo(5);
        // El body enviado respeta el contrato del enunciado
        assertThat(bodyRecibido.get()).contains("\"ticketId\":125", "\"categoria\":\"RED\"", "\"prioridad\":\"ALTA\"");
    }

    @Test
    void unErrorDelServidorSeTraduceADispatchNoDisponible() {
        status = 500;
        respuesta = "{\"error\":\"boom\"}";

        assertThatThrownBy(() -> adapter().sugerir(1L, Category.RED, Priority.ALTA))
                .isInstanceOf(DispatchUnavailableException.class);
    }

    @Test
    void unServicioApagadoSeTraduceADispatchNoDisponible() {
        TechnicianDispatchRestAdapter adapter = adapter();
        server.stop(0);

        assertThatThrownBy(() -> adapter.sugerir(1L, Category.RED, Priority.ALTA))
                .isInstanceOf(DispatchUnavailableException.class);
    }
}
