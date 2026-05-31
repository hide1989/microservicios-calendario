package com.pascualbravo.calendario.service;

import com.pascualbravo.calendario.dto.FestivoDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
public class FestivoClientService {

    private final WebClient festivosApiWebClient;

    public FestivoClientService(@Qualifier("festivosApiWebClient") WebClient festivosApiWebClient) {
        this.festivosApiWebClient = festivosApiWebClient;
    }

    public List<FestivoDTO> obtenerFestivosDelAnio(int anio) {
        try {
            return festivosApiWebClient.get()
                    .uri("/api/festivos/obtener/{anio}", anio)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<FestivoDTO>>() {})
                    .block();
        } catch (WebClientResponseException excepcion) {
            throw new RuntimeException(
                    "Error al consultar festivos del año " + anio + " en la API de Festivos: " + excepcion.getMessage(),
                    excepcion
            );
        } catch (Exception excepcion) {
            throw new RuntimeException(
                    "No fue posible conectarse a la API de Festivos para el año " + anio + ". Verifique que el servicio esté activo.",
                    excepcion
            );
        }
    }
}
