package com.helpflow.dispatch.service;

import com.helpflow.dispatch.api.DispatchRequest;
import com.helpflow.dispatch.api.DispatchResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Service
public class TechnicianDispatchService {

    // Primer técnico de cada grupo; cada categoría tiene 4 técnicos consecutivos
    private static final Map<String, Integer> PRIMER_TECNICO = Map.of(
            "HARDWARE", 1, "SOFTWARE", 5, "RED", 9, "ACCESOS", 13, "OTRO", 17);

    // Horas máximas simuladas según la prioridad
    private static final Map<String, Integer> HORAS_MAXIMAS = Map.of(
            "CRITICA", 4, "ALTA", 8, "MEDIA", 16, "BAJA", 24);

    public DispatchResponse dispatch(DispatchRequest request) {
        // Misma combinación -> mismo seed -> misma secuencia aleatoria
        long seed = Objects.hash(request.ticketId(), request.categoria(), request.prioridad());
        Random random = new Random(seed);

        int numero = PRIMER_TECNICO.get(request.categoria()) + random.nextInt(4);
        String tecnicoId = String.format("TEC-%03d", numero);

        int horas = random.nextInt(HORAS_MAXIMAS.get(request.prioridad()) + 1);

        return new DispatchResponse(request.ticketId(), tecnicoId, clasificar(horas), horas);
    }

    // Reglas de clasificación del enunciado
    String clasificar(int horas) {
        if (horas <= 2) return "ALTA";
        if (horas <= 8) return "MEDIA";
        return "BAJA";
    }
}
