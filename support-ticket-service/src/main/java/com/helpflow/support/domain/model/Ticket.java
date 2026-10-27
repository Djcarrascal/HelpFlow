package com.helpflow.support.domain.model;

import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.InvalidStatusTransitionException;

import java.time.Duration;
import java.time.LocalDateTime;

public class Ticket {
    // Fracción del SLA restante a partir de la cual se escala
    public static final double UMBRAL_ESCALAMIENTO = 0.25;

    private Long id;
    private Long solicitanteId;
    private String titulo;
    private String descripcion;
    private Category categoria;
    private Priority prioridad;
    private String sede;
    private String activoAfectado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimiteSla;
    private TicketStatus estado;
    private Long tecnicoId;
    private String tecnicoCodigo;
    private boolean escalado;
    private LocalDateTime fechaPrimeraRespuesta;
    private LocalDateTime fechaResolucion;

    // Fábrica: todo ticket nace ABIERTO y con su fecha límite calculada
    public static Ticket abrir(Long solicitanteId, String titulo, String descripcion, Category categoria,
                               Priority prioridad, String sede, String activoAfectado,
                               SlaPolicy politica, LocalDateTime ahora) {
        Ticket t = new Ticket();
        t.solicitanteId = solicitanteId;
        t.titulo = titulo;
        t.descripcion = descripcion;
        t.categoria = categoria;
        t.prioridad = prioridad;
        t.sede = sede;
        t.activoAfectado = activoAfectado;
        t.fechaCreacion = ahora;
        t.fechaLimiteSla = politica.calcularFechaLimite(ahora);
        t.estado = TicketStatus.ABIERTO;
        t.escalado = false;
        return t;
    }

    // Asignación inicial: solo desde ABIERTO
    public void asignar(Technician tecnico) {
        validarTransicion(TicketStatus.ASIGNADO);
        this.tecnicoId = tecnico.getId();
        this.tecnicoCodigo = tecnico.getCodigo();
        this.estado = TicketStatus.ASIGNADO;
    }

    // Reasignación del admin: cambia el técnico sin cambiar el estado
    public void reasignar(Technician tecnico) {
        if (estado != TicketStatus.ASIGNADO && estado != TicketStatus.EN_PROGRESO) {
            throw new BusinessRuleException("Solo se reasignan tickets ASIGNADO o EN_PROGRESO");
        }
        this.tecnicoId = tecnico.getId();
        this.tecnicoCodigo = tecnico.getCodigo();
    }

    // Cambio manual de estado (técnico o admin)
    public void cambiarEstado(TicketStatus nuevo, LocalDateTime ahora) {
        if (nuevo == TicketStatus.ASIGNADO) {
            throw new BusinessRuleException("El estado ASIGNADO se obtiene con la operación de asignación");
        }
        validarTransicion(nuevo);
        if (nuevo == TicketStatus.EN_PROGRESO && fechaPrimeraRespuesta == null) {
            fechaPrimeraRespuesta = ahora;      // primera respuesta = el técnico empieza a trabajar
        }
        if (nuevo == TicketStatus.RESUELTO) {
            fechaResolucion = ahora;
        }
        if (estado == TicketStatus.RESUELTO && nuevo == TicketStatus.EN_PROGRESO) {
            fechaResolucion = null;             // reapertura
        }
        this.estado = nuevo;
    }

    public boolean estaVencido(LocalDateTime ahora) {
        return estado.esActivo() && ahora.isAfter(fechaLimiteSla);
    }

    // Activo, sin escalar, sin vencer y con <= 25 % del tiempo restante
    public boolean estaProximoAVencer(LocalDateTime ahora) {
        if (!estado.esActivo() || escalado || estaVencido(ahora)) return false;
        long total = Duration.between(fechaCreacion, fechaLimiteSla).toMinutes();
        long restante = Duration.between(ahora, fechaLimiteSla).toMinutes();
        return restante <= total * UMBRAL_ESCALAMIENTO;
    }

    public void escalar() {
        this.prioridad = prioridad.siguiente();
        this.escalado = true;
    }

    public boolean perteneceA(Long usuarioId) { return solicitanteId.equals(usuarioId); }

    public boolean estaAsignadoA(Long idTecnico) { return idTecnico != null && idTecnico.equals(tecnicoId); }

    private void validarTransicion(TicketStatus nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new InvalidStatusTransitionException(estado, nuevo);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(Long solicitanteId) { this.solicitanteId = solicitanteId; }
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
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaLimiteSla() { return fechaLimiteSla; }
    public void setFechaLimiteSla(LocalDateTime fechaLimiteSla) { this.fechaLimiteSla = fechaLimiteSla; }
    public TicketStatus getEstado() { return estado; }
    public void setEstado(TicketStatus estado) { this.estado = estado; }
    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }
    public String getTecnicoCodigo() { return tecnicoCodigo; }
    public void setTecnicoCodigo(String tecnicoCodigo) { this.tecnicoCodigo = tecnicoCodigo; }
    public boolean isEscalado() { return escalado; }
    public void setEscalado(boolean escalado) { this.escalado = escalado; }
    public LocalDateTime getFechaPrimeraRespuesta() { return fechaPrimeraRespuesta; }
    public void setFechaPrimeraRespuesta(LocalDateTime fechaPrimeraRespuesta) { this.fechaPrimeraRespuesta = fechaPrimeraRespuesta; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
}
