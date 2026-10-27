package com.helpflow.support.domain.model;

public class Technician {
    private Long id;
    private String codigo;          // TEC-001: el mismo id que devuelve dispatch
    private Long usuarioId;         // usuario con el que inicia sesión
    private String nombre;
    private Category especialidad;
    private String sede;
    private int cargaMaxima;
    private RecordStatus estado;

    public boolean estaHabilitado() { return estado == RecordStatus.ACTIVO; }

    // OTRO actúa como especialidad generalista
    public boolean atiende(Category categoria) {
        return especialidad == categoria || especialidad == Category.OTRO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
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
