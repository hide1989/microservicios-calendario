package com.pascualbravo.calendario.repository;

import com.pascualbravo.calendario.model.Calendario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarioRepository extends JpaRepository<Calendario, Long> {

    List<Calendario> findByFechaBetweenOrderByFechaAsc(LocalDate inicio, LocalDate fin);

    long countByFechaBetween(LocalDate inicio, LocalDate fin);
}
