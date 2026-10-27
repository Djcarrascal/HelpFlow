package com.helpflow.support.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sla_politicas")
public class SlaPolicyEntity {
    // Llave String: MapStruct la convierte desde/hacia el enum Priority
    @Id
    private String prioridad;
    @Column(name = "horas_limite")
    private int horasLimite;

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
    public int getHorasLimite() { return horasLimite; }
    public void setHorasLimite(int horasLimite) { this.horasLimite = horasLimite; }
}
