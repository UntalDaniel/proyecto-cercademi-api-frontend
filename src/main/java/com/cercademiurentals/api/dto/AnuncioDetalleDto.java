package com.cercademiurentals.api.dto;

import java.util.List;

// Lombok @Data and @NoArgsConstructor removed for the main class and inner class
public class AnuncioDetalleDto {

    private String uri;
    private String titulo;
    private String descripcionDetallada;
    private Double precioMonto;
    private String terminoContrato;
    private Boolean esAnuncioCompartido;
    private Integer numeroHabitacionesDisponibles;
    private Integer numeroBanosDisponibles;
    private String fechaDisponibleDesde;
    private String fechaCreacionAnuncio;
    private String fechaUltimaActualizacion;
    private String estadoAnuncio;
    private String viviendaUri;
    private String direccion;
    private String barrio;
    private String ciudad;
    private String departamento;
    private Double latitud;
    private Double longitud;
    private String tipoVivienda;
    private Integer numeroTotalHabitaciones;
    private Integer numeroTotalBanos;
    private Integer metrosCuadrados;
    private ProveedorDto proveedor;
    private List<String> comodidades;
    private Boolean permiteMascotas;
    private Boolean permiteFumar;
    private Boolean permiteInvitados;
    private Boolean soloEstudiantes;
    private String preferenciaGenero;
    private Boolean estableceHorasSilencio;
    private String imagenUrl; // Nuevo campo

    // Constructor por defecto
    public AnuncioDetalleDto() {
    }

    // Getters and Setters for AnuncioDetalleDto
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcionDetallada() { return descripcionDetallada; }
    public void setDescripcionDetallada(String descripcionDetallada) { this.descripcionDetallada = descripcionDetallada; }
    public Double getPrecioMonto() { return precioMonto; }
    public void setPrecioMonto(Double precioMonto) { this.precioMonto = precioMonto; }
    public String getTerminoContrato() { return terminoContrato; }
    public void setTerminoContrato(String terminoContrato) { this.terminoContrato = terminoContrato; }
    public Boolean getEsAnuncioCompartido() { return esAnuncioCompartido; }
    public void setEsAnuncioCompartido(Boolean esAnuncioCompartido) { this.esAnuncioCompartido = esAnuncioCompartido; }
    public Integer getNumeroHabitacionesDisponibles() { return numeroHabitacionesDisponibles; }
    public void setNumeroHabitacionesDisponibles(Integer numeroHabitacionesDisponibles) { this.numeroHabitacionesDisponibles = numeroHabitacionesDisponibles; }
    public Integer getNumeroBanosDisponibles() { return numeroBanosDisponibles; }
    public void setNumeroBanosDisponibles(Integer numeroBanosDisponibles) { this.numeroBanosDisponibles = numeroBanosDisponibles; }
    public String getFechaDisponibleDesde() { return fechaDisponibleDesde; }
    public void setFechaDisponibleDesde(String fechaDisponibleDesde) { this.fechaDisponibleDesde = fechaDisponibleDesde; }
    public String getFechaCreacionAnuncio() { return fechaCreacionAnuncio; }
    public void setFechaCreacionAnuncio(String fechaCreacionAnuncio) { this.fechaCreacionAnuncio = fechaCreacionAnuncio; }
    public String getFechaUltimaActualizacion() { return fechaUltimaActualizacion; }
    public void setFechaUltimaActualizacion(String fechaUltimaActualizacion) { this.fechaUltimaActualizacion = fechaUltimaActualizacion; }
    public String getEstadoAnuncio() { return estadoAnuncio; }
    public void setEstadoAnuncio(String estadoAnuncio) { this.estadoAnuncio = estadoAnuncio; }
    public String getViviendaUri() { return viviendaUri; }
    public void setViviendaUri(String viviendaUri) { this.viviendaUri = viviendaUri; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getBarrio() { return barrio; }
    public void setBarrio(String barrio) { this.barrio = barrio; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public String getTipoVivienda() { return tipoVivienda; }
    public void setTipoVivienda(String tipoVivienda) { this.tipoVivienda = tipoVivienda; }
    public Integer getNumeroTotalHabitaciones() { return numeroTotalHabitaciones; }
    public void setNumeroTotalHabitaciones(Integer numeroTotalHabitaciones) { this.numeroTotalHabitaciones = numeroTotalHabitaciones; }
    public Integer getNumeroTotalBanos() { return numeroTotalBanos; }
    public void setNumeroTotalBanos(Integer numeroTotalBanos) { this.numeroTotalBanos = numeroTotalBanos; }
    public Integer getMetrosCuadrados() { return metrosCuadrados; }
    public void setMetrosCuadrados(Integer metrosCuadrados) { this.metrosCuadrados = metrosCuadrados; }
    public ProveedorDto getProveedor() { return proveedor; }
    public void setProveedor(ProveedorDto proveedor) { this.proveedor = proveedor; }
    public List<String> getComodidades() { return comodidades; }
    public void setComodidades(List<String> comodidades) { this.comodidades = comodidades; }
    public Boolean getPermiteMascotas() { return permiteMascotas; }
    public void setPermiteMascotas(Boolean permiteMascotas) { this.permiteMascotas = permiteMascotas; }
    public Boolean getPermiteFumar() { return permiteFumar; }
    public void setPermiteFumar(Boolean permiteFumar) { this.permiteFumar = permiteFumar; }
    public Boolean getPermiteInvitados() { return permiteInvitados; }
    public void setPermiteInvitados(Boolean permiteInvitados) { this.permiteInvitados = permiteInvitados; }
    public Boolean getSoloEstudiantes() { return soloEstudiantes; }
    public void setSoloEstudiantes(Boolean soloEstudiantes) { this.soloEstudiantes = soloEstudiantes; }
    public String getPreferenciaGenero() { return preferenciaGenero; }
    public void setPreferenciaGenero(String preferenciaGenero) { this.preferenciaGenero = preferenciaGenero; }
    public Boolean getEstableceHorasSilencio() { return estableceHorasSilencio; }
    public void setEstableceHorasSilencio(Boolean estableceHorasSilencio) { this.estableceHorasSilencio = estableceHorasSilencio; }
    public String getImagenUrl() { return imagenUrl; } // Getter para imagenUrl
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; } // Setter para imagenUrl

    public static class ProveedorDto {
        private String uri;
        private String nombres;
        private String apellidos;
        private String nombreUsuario;
        private Float calificacionPromedio;

        // Constructor por defecto
        public ProveedorDto() {
        }

        // Getters and Setters for ProveedorDto
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public String getNombres() { return nombres; }
        public void setNombres(String nombres) { this.nombres = nombres; }
        public String getApellidos() { return apellidos; }
        public void setApellidos(String apellidos) { this.apellidos = apellidos; }
        public String getNombreUsuario() { return nombreUsuario; }
        public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
        public Float getCalificacionPromedio() { return calificacionPromedio; }
        public void setCalificacionPromedio(Float calificacionPromedio) { this.calificacionPromedio = calificacionPromedio; }
    }
}