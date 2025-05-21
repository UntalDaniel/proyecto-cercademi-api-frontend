package com.cercademiurentals.api.dto;

public class UserProfileDto {

    private String uri;
    private String nombreUsuario;
    private String correo;
    private String nombres;
    private String apellidos;
    private String fechaCreacion;
    private String ultimoAcceso;
    private Float calificacionPromedio;
    private Boolean estaActivo;
    private Integer cantidadAnunciosPublicados; // Nuevo campo
    private Integer cantidadInteresesMarcados; // Nuevo campo
    // private String biografia; 
    // private PreferenciasBusquedaDto preferencias; 

    public UserProfileDto() {
    }

    // Getters
    public String getUri() { return uri; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getCorreo() { return correo; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getFechaCreacion() { return fechaCreacion; }
    public String getUltimoAcceso() { return ultimoAcceso; }
    public Float getCalificacionPromedio() { return calificacionPromedio; }
    public Boolean getEstaActivo() { return estaActivo; }
    public Integer getCantidadAnunciosPublicados() { return cantidadAnunciosPublicados; } // Getter
    public Integer getCantidadInteresesMarcados() { return cantidadInteresesMarcados; } // Getter
    // public String getBiografia() { return biografia; }
    // public PreferenciasBusquedaDto getPreferencias() { return preferencias; }


    // Setters
    public void setUri(String uri) { this.uri = uri; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setUltimoAcceso(String ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    public void setCalificacionPromedio(Float calificacionPromedio) { this.calificacionPromedio = calificacionPromedio; }
    public void setEstaActivo(Boolean estaActivo) { this.estaActivo = estaActivo; }
    public void setCantidadAnunciosPublicados(Integer cantidadAnunciosPublicados) { this.cantidadAnunciosPublicados = cantidadAnunciosPublicados; } // Setter
    public void setCantidadInteresesMarcados(Integer cantidadInteresesMarcados) { this.cantidadInteresesMarcados = cantidadInteresesMarcados; } // Setter
    // public void setBiografia(String biografia) { this.biografia = biografia; }
    // public void setPreferencias(PreferenciasBusquedaDto preferencias) { this.preferencias = preferencias; }
    
}