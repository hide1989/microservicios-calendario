package com.pascualbravo.calendario.controller;

import com.pascualbravo.calendario.dto.CalendarioDTO;
import com.pascualbravo.calendario.service.CalendarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendario")
@Tag(name = "Calendario", description = "Generación y consulta del calendario anual clasificado")
public class CalendarioController {

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping("/generar/{año}")
    @Operation(
            summary = "Generar y persistir el calendario de un año",
            description = "Obtiene los festivos del año desde la API de Festivos (MS1), clasifica cada día del año como " +
                    "'Dia laboral' (lunes a viernes no festivo), 'Fin de Semana' (sábado o domingo no festivo) o " +
                    "'Dia festivo', y persiste todos los registros en PostgreSQL. " +
                    "Si el calendario del año ya existe, retorna true sin reinsertar."
    )
    @ApiResponse(responseCode = "200", description = "Calendario generado o ya existente",
            content = @Content(schema = @Schema(type = "boolean", example = "true")))
    @ApiResponse(responseCode = "503", description = "La API de Festivos (MS1) no está disponible")
    public ResponseEntity<Boolean> generarCalendario(
            @Parameter(description = "Año para el cual generar el calendario", example = "2023")
            @PathVariable("año") int anio) {
        boolean resultado = calendarioService.generarCalendarioDelAnio(anio);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/listar/{año}")
    @Operation(
            summary = "Listar el calendario completo de un año",
            description = "Retorna todos los días del año con su clasificación (laboral, fin de semana o festivo) " +
                    "y su descripción. El calendario debe haberse generado previamente con /api/calendario/generar/{año}."
    )
    @ApiResponse(responseCode = "200", description = "Calendario del año",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CalendarioDTO.class))))
    @ApiResponse(responseCode = "404", description = "No se ha generado el calendario para el año indicado")
    public ResponseEntity<?> listarCalendario(
            @Parameter(description = "Año a listar", example = "2023")
            @PathVariable("año") int anio) {
        List<CalendarioDTO> diasDelAnio = calendarioService.listarCalendarioDelAnio(anio);

        if (diasDelAnio.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "mensaje", "No existe calendario generado para el año " + anio +
                               ". Use /api/calendario/generar/" + anio
            ));
        }

        return ResponseEntity.ok(diasDelAnio);
    }
}
