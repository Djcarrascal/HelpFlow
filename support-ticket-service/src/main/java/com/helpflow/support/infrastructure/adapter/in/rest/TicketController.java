package com.helpflow.support.infrastructure.adapter.in.rest;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.CreateTicketUseCase;
import com.helpflow.support.application.port.in.ManageTicketUseCase;
import com.helpflow.support.application.port.in.QueryTicketUseCase;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.ChangeStatusRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.CreateTicketRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.ReassignRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.TicketHistoryResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.TicketResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.mapper.TicketWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Tickets", description = "Registro, consulta y gestión de tickets")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final CreateTicketUseCase createTicket;
    private final ManageTicketUseCase manageTicket;
    private final QueryTicketUseCase queryTicket;
    private final TicketWebMapper mapper;

    public TicketController(CreateTicketUseCase createTicket, ManageTicketUseCase manageTicket,
                            QueryTicketUseCase queryTicket, TicketWebMapper mapper) {
        this.createTicket = createTicket;
        this.manageTicket = manageTicket;
        this.queryTicket = queryTicket;
        this.mapper = mapper;
    }

    @Operation(summary = "Registrar ticket",
               description = "Crea el ticket ABIERTO, calcula el SLA y lo asigna con technician-dispatch-service")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse crear(@Valid @RequestBody CreateTicketRequest request,
                                @AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toResponse(createTicket.crear(mapper.toCommand(request), actor));
    }

    // Usuario: los suyos; técnico: los asignados; admin: todos
    @Operation(summary = "Listar tickets según el rol")
    @GetMapping
    public List<TicketResponse> listar(@AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toResponses(queryTicket.listar(actor));
    }

    @Operation(summary = "Detalle de un ticket")
    @GetMapping("/{id}")
    public TicketResponse obtener(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toResponse(queryTicket.obtener(id, actor));
    }

    @Operation(summary = "Historial de cambios del ticket")
    @GetMapping("/{id}/historial")
    public List<TicketHistoryResponse> historial(@PathVariable Long id,
                                                 @AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toHistoryResponses(queryTicket.historial(id, actor));
    }

    @Operation(summary = "Cambiar estado", description = "Solo el técnico asignado o un admin")
    @PatchMapping("/{id}/estado")
    public TicketResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request,
                                        @AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toResponse(manageTicket.cambiarEstado(id, request.estado(), request.observacion(), actor));
    }

    @Operation(summary = "Reasignar técnico (admin)")
    @PatchMapping("/{id}/reasignar")
    public TicketResponse reasignar(@PathVariable Long id, @Valid @RequestBody ReassignRequest request,
                                    @AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toResponse(manageTicket.reasignar(id, request.tecnicoCodigo(), request.observacion(), actor));
    }

    // Reintento de asignación automática para tickets que quedaron ABIERTO
    @Operation(summary = "Reintentar asignación automática (admin)")
    @PostMapping("/{id}/asignar")
    public TicketResponse asignar(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedActor actor) {
        return mapper.toResponse(manageTicket.asignarAutomaticamente(id, actor.correo()));
    }

    // Ejecuta el escalamiento sin esperar a la tarea programada (útil para evidencias)
    @Operation(summary = "Ejecutar escalamiento por SLA ahora (admin)")
    @PostMapping("/escalamiento")
    public Map<String, Integer> escalar() {
        return Map.of("escalados", manageTicket.escalarProximosAVencer());
    }
}
