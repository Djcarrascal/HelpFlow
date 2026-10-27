package com.helpflow.support.infrastructure.adapter.out.persistence.entity;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.domain.model.TicketStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N-1 con el solicitante
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id")
    private UserEntity solicitante;

    private String titulo;
    private String descripcion;
    @Enumerated(EnumType.STRING)
    private Category categoria;
    @Enumerated(EnumType.STRING)
    private Priority prioridad;
    private String sede;
    @Column(name = "activo_afectado")
    private String activoAfectado;
    @Enumerated(EnumType.STRING)
    private TicketStatus estado;
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column(name = "fecha_limite_sla")
    private LocalDateTime fechaLimiteSla;

    // N-1 con el técnico asignado (puede ser null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private TechnicianEntity tecnico;

    private boolean escalado;
    @Column(name = "fecha_primera_respuesta")
    private LocalDateTime fechaPrimeraRespuesta;
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    // Ticket 1-N Historiales (lado inverso)
    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @OrderBy("fecha ASC, id ASC")
    private List<TicketHistoryEntity> historial = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getSolicitante() { return solicitante; }
    public void setSolicitante(UserEntity solicitante) { this.solicitante = solicitante; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Category getCategoria() { return categoria; }
    public void setCategoria(Category categoria) { this.categoria = categoria; }
    public Priority getPrioridad() { return prioridad; }
    public void setPrioridad(Priority prioridad) { this.prioridad = prioridad; }
    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }
    public String getActivoAfectado() { return activoAfectado; }
    public void setActivoAfectado(String activoAfectado) { this.activoAfectado = activoAfectado; }
    public TicketStatus getEstado() { return estado; }
    public void setEstado(TicketStatus estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaLimiteSla() { return fechaLimiteSla; }
    public void setFechaLimiteSla(LocalDateTime fechaLimiteSla) { this.fechaLimiteSla = fechaLimiteSla; }
    public TechnicianEntity getTecnico() { return tecnico; }
    public void setTecnico(TechnicianEntity tecnico) { this.tecnico = tecnico; }
    public boolean isEscalado() { return escalado; }
    public void setEscalado(boolean escalado) { this.escalado = escalado; }
    public LocalDateTime getFechaPrimeraRespuesta() { return fechaPrimeraRespuesta; }
    public void setFechaPrimeraRespuesta(LocalDateTime fechaPrimeraRespuesta) { this.fechaPrimeraRespuesta = fechaPrimeraRespuesta; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public List<TicketHistoryEntity> getHistorial() { return historial; }
    public void setHistorial(List<TicketHistoryEntity> historial) { this.historial = historial; }
}
