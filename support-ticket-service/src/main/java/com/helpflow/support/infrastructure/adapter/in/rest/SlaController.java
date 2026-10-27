package com.helpflow.support.infrastructure.adapter.in.rest;

import com.helpflow.support.application.port.in.SlaConfigUseCase;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.SlaResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.SlaUpdateRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.mapper.TicketWebMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Configuración de SLA: solo ADMIN (regla en SecurityConfig)
@Tag(name = "SLA", description = "Configuración de tiempos límite (admin)")
@RestController
@RequestMapping("/api/sla")
public class SlaController {

    private final SlaConfigUseCase slaConfig;
    private final TicketWebMapper mapper;

    public SlaController(SlaConfigUseCase slaConfig, TicketWebMapper mapper) {
        this.slaConfig = slaConfig;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SlaResponse> listar() {
        return mapper.toSlaResponses(slaConfig.listar());
    }

    @PutMapping
    public SlaResponse actualizar(@Valid @RequestBody SlaUpdateRequest request) {
        return mapper.toResponse(slaConfig.actualizar(request.prioridad(), request.horasLimite()));
    }
}
