package com.pascualbravo.calendario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de clasificación de un día del calendario")
public class TipoDTO {

    @Schema(description = "Identificador del tipo de día", example = "1")
    private Integer id;

    @Schema(description = "Nombre del tipo de día", example = "Dia laboral")
    private String tipo;

    public TipoDTO() {}

    public TipoDTO(Integer id, String tipo) {
        this.id = id;
        this.tipo = tipo;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
