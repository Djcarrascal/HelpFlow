package com.helpflow.support.domain.model;

import java.time.LocalDateTime;

// Entrada inmutable del historial: quién cambió qué y cuándo
public class TicketHistory {
    private Long id;
    private Long ticketId;
    private HistoryAction accion;
    private TicketStatus estadoAnterior;
    private TicketStatus estadoNuevo;
    private String observacion;
    private String responsable;     // correo de quien hizo el cambio o "SISTEMA"
    private LocalDateTime fecha;

    public TicketHistory() {}

    public static TicketHistory de(Ticket ticket, HistoryAction accion, TicketStatus anterior,
                                   String observacion, String responsable, LocalDateTime fecha) {
        TicketHistory h = new TicketHistory();
        h.ticketId = ticket.getId();
        h.accion = accion;
        h.estadoAnterior = anterior;
        h.estadoNuevo = ticket.getEstado();
        h.observacion = observacion;
        h.responsable = responsable;
        h.fecha = fecha;
        return h;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public HistoryAction getAccion() { return accion; }
    public void setAccion(HistoryAction accion) { this.accion = accion; }
    public TicketStatus getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(TicketStatus estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public TicketStatus getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(TicketStatus estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
