package com.helpflow.support.infrastructure.adapter.out.persistence.entity;

import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String documento;
    private String nombre;
    private String correo;
    @Column(name = "password_hash")
    private String passwordHash;
    private String sede;
    private String area;
    @Enumerated(EnumType.STRING)
    private RecordStatus estado;
    @Enumerated(EnumType.STRING)
    private Role rol;

    // Usuario 1-N Tickets (lado inverso, sin cascada)
    @OneToMany(mappedBy = "solicitante", fetch = FetchType.LAZY)
    private List<TicketEntity> tickets = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public RecordStatus getEstado() { return estado; }
    public void setEstado(RecordStatus estado) { this.estado = estado; }
    public Role getRol() { return rol; }
    public void setRol(Role rol) { this.rol = rol; }
    public List<TicketEntity> getTickets() { return tickets; }
    public void setTickets(List<TicketEntity> tickets) { this.tickets = tickets; }
}
