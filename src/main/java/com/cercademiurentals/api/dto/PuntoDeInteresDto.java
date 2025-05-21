package com.cercademiurentals.api.dto;

// Lombok annotations removed, explicit getters and setters added
public class PuntoDeInteresDto {
    private String uri;         
    private String nombre;      
    private Double latitud;     
    private Double longitud;    

    // Constructor por defecto
    public PuntoDeInteresDto() {
    }

    // Constructor con todos los campos (opcional, pero lo tenías con @AllArgsConstructor)
    public PuntoDeInteresDto(String uri, String nombre, Double latitud, Double longitud) {
        this.uri = uri;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // Getters
    public String getUri() {
        return uri;
    }

    public String getNombre() {
        return nombre;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    // Setters
    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
}
