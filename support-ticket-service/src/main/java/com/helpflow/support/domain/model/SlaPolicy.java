package com.helpflow.support.domain.model;

import java.time.LocalDateTime;

// Tiempo límite de atención configurado para una prioridad
public class SlaPolicy {
    private Priority prioridad;
    private int horasLimite;

    public SlaPolicy() {}

    public SlaPolicy(Priority prioridad, int horasLimite) {
        this.prioridad = prioridad;
        this.horasLimite = horasLimite;
    }

    public LocalDateTime calcularFechaLimite(LocalDateTime creacion) {
        return creacion.plusHours(horasLimite);
    }

    public Priority getPrioridad() { return prioridad; }
    public void setPrioridad(Priority prioridad) { this.prioridad = prioridad; }
    public int getHorasLimite() { return horasLimite; }
    public void setHorasLimite(int horasLimite) { this.horasLimite = horasLimite; }
}
