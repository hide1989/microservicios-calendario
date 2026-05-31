package com.pascualbravo.calendario.controller;

import com.pascualbravo.calendario.dto.FestivoDTO;
import com.pascualbravo.calendario.service.FestivoClientService;
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

@RestController
@RequestMapping("/api/festivos")
@Tag(name = "Festivos", description = "Consulta de festivos colombianos consumiendo la API de Festivos (MS1)")
public class FestivoController {

    private final FestivoClientService festivoClientService;

    public FestivoController(FestivoClientService festivoClientService) {
        this.festivoClientService = festivoClientService;
    }

    @GetMapping("/obtener/{año}")
    @Operation(
            summary = "Obtener festivos de un año",
            description = "Consulta la API de Festivos (MS1 en puerto 8080) y retorna la lista completa de festivos colombianos para el año dado, incluyendo los festivos de puente calculados y los basados en el Domingo de Pascua."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de festivos del año obtenida correctamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FestivoDTO.class)))
    )
    @ApiResponse(responseCode = "503", description = "La API de Festivos (MS1) no está disponible")
    public ResponseEntity<List<FestivoDTO>> obtenerFestivosPorAnio(
            @Parameter(description = "Año para consultar los festivos", example = "2023")
            @PathVariable("año") int anio) {
        List<FestivoDTO> festivos = festivoClientService.obtenerFestivosDelAnio(anio);
        return ResponseEntity.ok(festivos);
    }
}
