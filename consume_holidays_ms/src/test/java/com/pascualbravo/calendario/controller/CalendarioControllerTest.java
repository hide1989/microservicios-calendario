package com.pascualbravo.calendario.controller;

import com.pascualbravo.calendario.dto.CalendarioDTO;
import com.pascualbravo.calendario.dto.FestivoDTO;
import com.pascualbravo.calendario.dto.TipoDTO;
import com.pascualbravo.calendario.service.CalendarioService;
import com.pascualbravo.calendario.service.FestivoClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CalendarioController.class, FestivoController.class})
class CalendarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalendarioService calendarioService;

    @MockBean
    private FestivoClientService festivoClientService;

    @Test
    void generarCalendarioRetorna200ConTrue() throws Exception {
        when(calendarioService.generarCalendarioDelAnio(2023)).thenReturn(true);

        mockMvc.perform(get("/api/calendario/generar/2023").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void listarCalendarioRetorna200ConListaDeDias() throws Exception {
        List<CalendarioDTO> diasMock = List.of(
                new CalendarioDTO(1L, "2023-01-01", new TipoDTO(3, "Dia festivo"), "Domingo - Año nuevo"),
                new CalendarioDTO(2L, "2023-01-02", new TipoDTO(1, "Dia laboral"), "Lunes")
        );
        when(calendarioService.listarCalendarioDelAnio(2023)).thenReturn(diasMock);

        mockMvc.perform(get("/api/calendario/listar/2023").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fecha", is("2023-01-01")))
                .andExpect(jsonPath("$[0].tipo.id", is(3)))
                .andExpect(jsonPath("$[0].descripcion", is("Domingo - Año nuevo")))
                .andExpect(jsonPath("$[1].tipo.id", is(1)));
    }

    @Test
    void listarCalendarioRetorna404CuandoAnioNoEstaGenerado() throws Exception {
        when(calendarioService.listarCalendarioDelAnio(2099)).thenReturn(List.of());

        mockMvc.perform(get("/api/calendario/listar/2099").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void obtenerFestivosRetorna200ConListaDeFestivos() throws Exception {
        List<FestivoDTO> festivosMock = List.of(
                new FestivoDTO("Año nuevo", "2023-01-01"),
                new FestivoDTO("Santos Reyes", "2023-01-09")
        );
        when(festivoClientService.obtenerFestivosDelAnio(2023)).thenReturn(festivosMock);

        mockMvc.perform(get("/api/festivos/obtener/2023").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].festivo", is("Año nuevo")))
                .andExpect(jsonPath("$[0].fecha", is("2023-01-01")));
    }
}
