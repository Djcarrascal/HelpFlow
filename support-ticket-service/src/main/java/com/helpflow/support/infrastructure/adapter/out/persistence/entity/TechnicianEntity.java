package com.helpflow.support.infrastructure.adapter.out.persistence.entity;

import com.helpflow.support.domain.model.Category;
import com.helpflow.support.domain.model.RecordStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "tecnicos")
public class TechnicianEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codigo;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UserEntity usuario;
    private String nombre;
    @Enumerated(EnumType.STRING)
    private Category especialidad;
    private String sede;
    @Column(name = "carga_maxima")
    private int cargaMaxima;
    @Enumerated(EnumType.STRING)
    private RecordStatus estado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public UserEntity getUsuario() { return usuario; }
    public void setUsuario(UserEntity usuario) { this.usuario = usuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Category getEspecialidad() { return especialidad; }
    public void setEspecialidad(Category especialidad) { this.especialidad = especialidad; }
    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }
    public int getCargaMaxima() { return cargaMaxima; }
    public void setCargaMaxima(int cargaMaxima) { this.cargaMaxima = cargaMaxima; }
    public RecordStatus getEstado() { return estado; }
    public void setEstado(RecordStatus estado) { this.estado = estado; }
}
