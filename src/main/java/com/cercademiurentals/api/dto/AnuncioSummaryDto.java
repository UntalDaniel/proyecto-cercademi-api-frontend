package com.cercademiurentals.api.dto;

// Lombok annotations removed, explicit getters and setters added
public class AnuncioSummaryDto {

    private String uri;
    private String titulo;
    private Double precio; 
    private Double latitud;
    private Double longitud;
    private String barrio;
    private String tipoVivienda;
    private String estadoAnuncio; 
    private String fechaCreacionAnuncio; 
    private String imagenUrl; // Nuevo campo

    // Constructor por defecto
    public AnuncioSummaryDto() {
    }

    // Constructor con todos los campos (opcional, pero útil si @AllArgsConstructor estaba antes)
    public AnuncioSummaryDto(String uri, String titulo, Double precio, Double latitud, Double longitud, String barrio, String tipoVivienda, String estadoAnuncio, String fechaCreacionAnuncio, String imagenUrl) {
        this.uri = uri;
        this.titulo = titulo;
        this.precio = precio;
        this.latitud = latitud;
        this.longitud = longitud;
        this.barrio = barrio;
        this.tipoVivienda = tipoVivienda;
        this.estadoAnuncio = estadoAnuncio;
        this.fechaCreacionAnuncio = fechaCreacionAnuncio;
        this.imagenUrl = imagenUrl; // Asignar nuevo campo
    }

    // Getters
    public String getUri() {
        return uri;
    }

    public String getTitulo() {
        return titulo;
    }

    public Double getPrecio() {
        return precio;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getBarrio() {
        return barrio;
    }

    public String getTipoVivienda() {
        return tipoVivienda;
    }

    public String getEstadoAnuncio() {
        return estadoAnuncio;
    }

    public String getFechaCreacionAnuncio() {
        return fechaCreacionAnuncio;
    }

    public String getImagenUrl() { // Getter para imagenUrl
        return imagenUrl;
    }

    // Setters
    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public void setTipoVivienda(String tipoVivienda) {
        this.tipoVivienda = tipoVivienda;
    }

    public void setEstadoAnuncio(String estadoAnuncio) {
        this.estadoAnuncio = estadoAnuncio;
    }

    public void setFechaCreacionAnuncio(String fechaCreacionAnuncio) {
        this.fechaCreacionAnuncio = fechaCreacionAnuncio;
    }

    public void setImagenUrl(String imagenUrl) { // Setter para imagenUrl
        this.imagenUrl = imagenUrl;
    }
}
