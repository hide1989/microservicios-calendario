package com.pascualbravo.calendario.repository;

import com.pascualbravo.calendario.model.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoRepository extends JpaRepository<Tipo, Integer> {
}
