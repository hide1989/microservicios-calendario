package com.pascualbravo.calendario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representa un festivo colombiano con su nombre y fecha calculada")
public class FestivoDTO {

    @Schema(description = "Nombre del festivo", example = "Año nuevo")
    private String festivo;

    @Schema(description = "Fecha del festivo en formato YYYY-MM-DD", example = "2023-01-01")
    private String fecha;

    public FestivoDTO() {}

    public FestivoDTO(String festivo, String fecha) {
        this.festivo = festivo;
        this.fecha = fecha;
    }

    public String getFestivo() { return festivo; }
    public void setFestivo(String festivo) { this.festivo = festivo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}
