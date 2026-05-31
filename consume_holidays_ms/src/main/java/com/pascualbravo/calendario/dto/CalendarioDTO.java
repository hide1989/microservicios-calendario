package com.pascualbravo.calendario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representa un día del calendario anual con su clasificación")
public class CalendarioDTO {

    @Schema(description = "Identificador único del registro", example = "1")
    private Long id;

    @Schema(description = "Fecha en formato YYYY-MM-DD", example = "2023-01-01")
    private String fecha;

    @Schema(description = "Tipo de clasificación del día")
    private TipoDTO tipo;

    @Schema(description = "Descripción del día", example = "Domingo - Año nuevo")
    private String descripcion;

    public CalendarioDTO() {}

    public CalendarioDTO(Long id, String fecha, TipoDTO tipo, String descripcion) {
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public TipoDTO getTipo() { return tipo; }
    public void setTipo(TipoDTO tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
