package com.helpflow.support.infrastructure.adapter.out.persistence.entity;

import com.helpflow.support.domain.model.HistoryAction;
import com.helpflow.support.domain.model.TicketStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_historial")
public class TicketHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private TicketEntity ticket;
    @Enumerated(EnumType.STRING)
    private HistoryAction accion;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private TicketStatus estadoAnterior;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo")
    private TicketStatus estadoNuevo;
    private String observacion;
    private String responsable;
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TicketEntity getTicket() { return ticket; }
    public void setTicket(TicketEntity ticket) { this.ticket = ticket; }
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
