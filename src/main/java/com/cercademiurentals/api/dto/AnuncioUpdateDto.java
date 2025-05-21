package com.cercademiurentals.api.dto;

import java.util.List;

public class AnuncioUpdateDto {

    private String titulo;
    private String descripcionDetallada;
    private Double precioMonto;
    private String terminoContrato;
    private Boolean esAnuncioCompartido;
    private Integer numeroHabitacionesDisponibles;
    private Integer numeroBanosDisponibles;
    private String fechaDisponibleDesde;
    private String direccion;
    private String barrio;
    private Double latitud;
    private Double longitud;
    private String tipoVivienda;
    private Integer numeroTotalHabitaciones;
    private Integer numeroTotalBanos;
    private Integer metrosCuadrados;
    private List<String> comodidadesUris;
    private Boolean permiteMascotas;
    private Boolean permiteFumar;
    private Boolean permiteInvitados;
    private Boolean soloEstudiantes;
    private String preferenciaGenero;
    private Boolean estableceHorasSilencio;
    private String imagenUrl; // Nuevo campo

    // Constructor por defecto
    public AnuncioUpdateDto() {
    }

    // Getters
    public String getTitulo() {
        return titulo;
    }

    public String getDescripcionDetallada() {
        return descripcionDetallada;
    }

    public Double getPrecioMonto() {
        return precioMonto;
    }

    public String getTerminoContrato() {
        return terminoContrato;
    }

    public Boolean getEsAnuncioCompartido() {
        return esAnuncioCompartido;
    }

    public Integer getNumeroHabitacionesDisponibles() {
        return numeroHabitacionesDisponibles;
    }

    public Integer getNumeroBanosDisponibles() {
        return numeroBanosDisponibles;
    }

    public String getFechaDisponibleDesde() {
        return fechaDisponibleDesde;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getBarrio() {
        return barrio;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getTipoVivienda() {
        return tipoVivienda;
    }

    public Integer getNumeroTotalHabitaciones() {
        return numeroTotalHabitaciones;
    }

    public Integer getNumeroTotalBanos() {
        return numeroTotalBanos;
    }

    public Integer getMetrosCuadrados() {
        return metrosCuadrados;
    }

    public List<String> getComodidadesUris() {
        return comodidadesUris;
    }

    public Boolean getPermiteMascotas() {
        return permiteMascotas;
    }

    public Boolean getPermiteFumar() {
        return permiteFumar;
    }

    public Boolean getPermiteInvitados() {
        return permiteInvitados;
    }

    public Boolean getSoloEstudiantes() {
        return soloEstudiantes;
    }

    public String getPreferenciaGenero() {
        return preferenciaGenero;
    }

    public Boolean getEstableceHorasSilencio() {
        return estableceHorasSilencio;
    }

    public String getImagenUrl() { // Getter para el nuevo campo
        return imagenUrl;
    }

    // Setters
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcionDetallada(String descripcionDetallada) {
        this.descripcionDetallada = descripcionDetallada;
    }

    public void setPrecioMonto(Double precioMonto) {
        this.precioMonto = precioMonto;
    }

    public void setTerminoContrato(String terminoContrato) {
        this.terminoContrato = terminoContrato;
    }

    public void setEsAnuncioCompartido(Boolean esAnuncioCompartido) {
        this.esAnuncioCompartido = esAnuncioCompartido;
    }

    public void setNumeroHabitacionesDisponibles(Integer numeroHabitacionesDisponibles) {
        this.numeroHabitacionesDisponibles = numeroHabitacionesDisponibles;
    }

    public void setNumeroBanosDisponibles(Integer numeroBanosDisponibles) {
        this.numeroBanosDisponibles = numeroBanosDisponibles;
    }

    public void setFechaDisponibleDesde(String fechaDisponibleDesde) {
        this.fechaDisponibleDesde = fechaDisponibleDesde;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public void setTipoVivienda(String tipoVivienda) {
        this.tipoVivienda = tipoVivienda;
    }

    public void setNumeroTotalHabitaciones(Integer numeroTotalHabitaciones) {
        this.numeroTotalHabitaciones = numeroTotalHabitaciones;
    }

    public void setNumeroTotalBanos(Integer numeroTotalBanos) {
        this.numeroTotalBanos = numeroTotalBanos;
    }

    public void setMetrosCuadrados(Integer metrosCuadrados) {
        this.metrosCuadrados = metrosCuadrados;
    }

    public void setComodidadesUris(List<String> comodidadesUris) {
        this.comodidadesUris = comodidadesUris;
    }

    public void setPermiteMascotas(Boolean permiteMascotas) {
        this.permiteMascotas = permiteMascotas;
    }

    public void setPermiteFumar(Boolean permiteFumar) {
        this.permiteFumar = permiteFumar;
    }

    public void setPermiteInvitados(Boolean permiteInvitados) {
        this.permiteInvitados = permiteInvitados;
    }

    public void setSoloEstudiantes(Boolean soloEstudiantes) {
        this.soloEstudiantes = soloEstudiantes;
    }

    public void setPreferenciaGenero(String preferenciaGenero) {
        this.preferenciaGenero = preferenciaGenero;
    }

    public void setEstableceHorasSilencio(Boolean estableceHorasSilencio) {
        this.estableceHorasSilencio = estableceHorasSilencio;
    }

    public void setImagenUrl(String imagenUrl) { // Setter para el nuevo campo
        this.imagenUrl = imagenUrl;
    }
}
