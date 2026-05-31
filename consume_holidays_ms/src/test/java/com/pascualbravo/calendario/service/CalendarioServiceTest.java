package com.pascualbravo.calendario.service;

import com.pascualbravo.calendario.dto.FestivoDTO;
import com.pascualbravo.calendario.model.Calendario;
import com.pascualbravo.calendario.model.Tipo;
import com.pascualbravo.calendario.repository.CalendarioRepository;
import com.pascualbravo.calendario.repository.TipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioServiceTest {

    @Mock
    private FestivoClientService festivoClientService;

    @Mock
    private CalendarioRepository calendarioRepository;

    @Mock
    private TipoRepository tipoRepository;

    private CalendarioService calendarioService;

    private final Tipo tipoDiaLaboral = new Tipo(1, "Dia laboral");
    private final Tipo tipoFinSemana = new Tipo(2, "Fin de Semana");
    private final Tipo tipoDiaFestivo = new Tipo(3, "Dia festivo");

    @BeforeEach
    void configurar() {
        calendarioService = new CalendarioService(festivoClientService, calendarioRepository, tipoRepository);
    }

    @Test
    void generaCalendarioConTodosLosDiasDelAnio2023() {
        configurarMocksParaGeneracion();

        boolean resultado = calendarioService.generarCalendarioDelAnio(2023);

        assertThat(resultado).isTrue();
        ArgumentCaptor<List<Calendario>> captor = ArgumentCaptor.forClass(List.class);
        verify(calendarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(365);
    }

    @Test
    void clasifica1Enero2023ComoFestivo() {
        configurarMocksParaGeneracion();

        calendarioService.generarCalendarioDelAnio(2023);

        ArgumentCaptor<List<Calendario>> captor = ArgumentCaptor.forClass(List.class);
        verify(calendarioRepository).saveAll(captor.capture());

        Calendario primerDia = captor.getValue().get(0);
        assertThat(primerDia.getFecha()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(primerDia.getTipo().getId()).isEqualTo(3);
        assertThat(primerDia.getDescripcion()).contains("Año nuevo");
    }

    @Test
    void clasifica2Enero2023ComoDiaLaboral() {
        configurarMocksParaGeneracion();

        calendarioService.generarCalendarioDelAnio(2023);

        ArgumentCaptor<List<Calendario>> captor = ArgumentCaptor.forClass(List.class);
        verify(calendarioRepository).saveAll(captor.capture());

        Calendario segundoDia = captor.getValue().get(1);
        assertThat(segundoDia.getFecha()).isEqualTo(LocalDate.of(2023, 1, 2));
        assertThat(segundoDia.getTipo().getId()).isEqualTo(1);
        assertThat(segundoDia.getDescripcion()).isEqualTo("Lunes");
    }

    @Test
    void clasifica7Enero2023ComoFinDeSemana() {
        configurarMocksParaGeneracion();

        calendarioService.generarCalendarioDelAnio(2023);

        ArgumentCaptor<List<Calendario>> captor = ArgumentCaptor.forClass(List.class);
        verify(calendarioRepository).saveAll(captor.capture());

        Calendario sabado = captor.getValue().get(6);
        assertThat(sabado.getFecha()).isEqualTo(LocalDate.of(2023, 1, 7));
        assertThat(sabado.getTipo().getId()).isEqualTo(2);
        assertThat(sabado.getDescripcion()).isEqualTo("Sábado");
    }

    @Test
    void retornaTrueSinInsertarCuandoAnioYaExiste() {
        when(calendarioRepository.countByFechaBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(365L);

        boolean resultado = calendarioService.generarCalendarioDelAnio(2023);

        assertThat(resultado).isTrue();
        verify(calendarioRepository, never()).saveAll(anyList());
    }

    private void configurarMocksParaGeneracion() {
        when(calendarioRepository.countByFechaBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(festivoClientService.obtenerFestivosDelAnio(2023))
                .thenReturn(List.of(new FestivoDTO("Año nuevo", "2023-01-01")));
        when(tipoRepository.getReferenceById(1)).thenReturn(tipoDiaLaboral);
        when(tipoRepository.getReferenceById(2)).thenReturn(tipoFinSemana);
        when(tipoRepository.getReferenceById(3)).thenReturn(tipoDiaFestivo);
    }
}
