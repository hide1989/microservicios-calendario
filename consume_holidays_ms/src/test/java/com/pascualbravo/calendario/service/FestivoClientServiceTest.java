package com.pascualbravo.calendario.service;

import com.pascualbravo.calendario.dto.FestivoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FestivoClientServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private FestivoClientService festivoClientService;

    @BeforeEach
    void configurar() {
        festivoClientService = new FestivoClientService(webClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void retornaListaDeFestivosCorrectamente() {
        List<FestivoDTO> festivosMock = List.of(
                new FestivoDTO("Año nuevo", "2023-01-01"),
                new FestivoDTO("Santos Reyes", "2023-01-09")
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(festivosMock));

        List<FestivoDTO> resultado = festivoClientService.obtenerFestivosDelAnio(2023);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getFestivo()).isEqualTo("Año nuevo");
        assertThat(resultado.get(0).getFecha()).isEqualTo("2023-01-01");
    }

    @SuppressWarnings("unchecked")
    @Test
    void lanzaExcepcionDescriptivaCuandoMS1NoResponde() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> festivoClientService.obtenerFestivosDelAnio(2023))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No fue posible conectarse a la API de Festivos");
    }
}
