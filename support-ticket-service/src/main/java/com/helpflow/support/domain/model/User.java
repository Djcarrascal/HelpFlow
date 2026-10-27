package com.helpflow.support.domain.model;

public class User {
    private Long id;
    private String documento;
    private String nombre;
    private String correo;
    private String passwordHash;
    private String sede;
    private String area;
    private RecordStatus estado;
    private Role rol;

    public boolean estaActivo() { return estado == RecordStatus.ACTIVO; }

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
}
