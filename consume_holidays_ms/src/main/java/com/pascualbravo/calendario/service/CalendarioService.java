package com.pascualbravo.calendario.service;

import com.pascualbravo.calendario.dto.CalendarioDTO;
import com.pascualbravo.calendario.dto.FestivoDTO;
import com.pascualbravo.calendario.dto.TipoDTO;
import com.pascualbravo.calendario.model.Calendario;
import com.pascualbravo.calendario.model.Tipo;
import com.pascualbravo.calendario.repository.CalendarioRepository;
import com.pascualbravo.calendario.repository.TipoRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarioService {

    private static final int ID_TIPO_DIA_LABORAL = 1;
    private static final int ID_TIPO_FIN_SEMANA = 2;
    private static final int ID_TIPO_DIA_FESTIVO = 3;

    private static final Map<DayOfWeek, String> NOMBRES_DIAS = Map.of(
            DayOfWeek.MONDAY, "Lunes",
            DayOfWeek.TUESDAY, "Martes",
            DayOfWeek.WEDNESDAY, "Miércoles",
            DayOfWeek.THURSDAY, "Jueves",
            DayOfWeek.FRIDAY, "Viernes",
            DayOfWeek.SATURDAY, "Sábado",
            DayOfWeek.SUNDAY, "Domingo"
    );

    private final FestivoClientService festivoClientService;
    private final CalendarioRepository calendarioRepository;
    private final TipoRepository tipoRepository;

    public CalendarioService(FestivoClientService festivoClientService,
                             CalendarioRepository calendarioRepository,
                             TipoRepository tipoRepository) {
        this.festivoClientService = festivoClientService;
        this.calendarioRepository = calendarioRepository;
        this.tipoRepository = tipoRepository;
    }

    public boolean generarCalendarioDelAnio(int anio) {
        LocalDate primerDia = LocalDate.of(anio, 1, 1);
        LocalDate ultimoDia = LocalDate.of(anio, 12, 31);

        long diasExistentes = calendarioRepository.countByFechaBetween(primerDia, ultimoDia);
        if (diasExistentes > 0) {
            return true;
        }

        List<FestivoDTO> festivosDelAnio = festivoClientService.obtenerFestivosDelAnio(anio);
        Map<String, String> festivosPorFecha = festivosDelAnio.stream()
                .collect(Collectors.toMap(FestivoDTO::getFecha, FestivoDTO::getFestivo,
                        (existing, replacement) -> existing));

        Tipo tipoDiaLaboral = tipoRepository.getReferenceById(ID_TIPO_DIA_LABORAL);
        Tipo tipoFinSemana = tipoRepository.getReferenceById(ID_TIPO_FIN_SEMANA);
        Tipo tipoDiaFestivo = tipoRepository.getReferenceById(ID_TIPO_DIA_FESTIVO);

        List<Calendario> diasDelAnio = new ArrayList<>();
        LocalDate fechaActual = primerDia;

        while (!fechaActual.isAfter(ultimoDia)) {
            String fechaFormateada = fechaActual.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String nombreDia = NOMBRES_DIAS.get(fechaActual.getDayOfWeek());
            DayOfWeek diaSemana = fechaActual.getDayOfWeek();

            Calendario diaCalendario = new Calendario();
            diaCalendario.setFecha(fechaActual);

            if (festivosPorFecha.containsKey(fechaFormateada)) {
                String nombreFestivo = festivosPorFecha.get(fechaFormateada);
                diaCalendario.setTipo(tipoDiaFestivo);
                diaCalendario.setDescripcion(nombreDia + " - " + nombreFestivo);
            } else if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
                diaCalendario.setTipo(tipoFinSemana);
                diaCalendario.setDescripcion(nombreDia);
            } else {
                diaCalendario.setTipo(tipoDiaLaboral);
                diaCalendario.setDescripcion(nombreDia);
            }

            diasDelAnio.add(diaCalendario);
            fechaActual = fechaActual.plusDays(1);
        }

        calendarioRepository.saveAll(diasDelAnio);
        return true;
    }

    public List<CalendarioDTO> listarCalendarioDelAnio(int anio) {
        LocalDate primerDia = LocalDate.of(anio, 1, 1);
        LocalDate ultimoDia = LocalDate.of(anio, 12, 31);

        return calendarioRepository.findByFechaBetweenOrderByFechaAsc(primerDia, ultimoDia)
                .stream()
                .map(this::convertirACalendarioDTO)
                .collect(Collectors.toList());
    }

    private CalendarioDTO convertirACalendarioDTO(Calendario calendario) {
        TipoDTO tipoDTO = new TipoDTO(calendario.getTipo().getId(), calendario.getTipo().getTipo());
        return new CalendarioDTO(
                calendario.getId(),
                calendario.getFecha().format(DateTimeFormatter.ISO_LOCAL_DATE),
                tipoDTO,
                calendario.getDescripcion()
        );
    }
}
