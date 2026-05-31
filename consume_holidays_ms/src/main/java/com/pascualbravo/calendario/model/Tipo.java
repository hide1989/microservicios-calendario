package com.pascualbravo.calendario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo")
public class Tipo {

    @Id
    private Integer id;

    @Column(nullable = false, length = 50)
    private String tipo;

    public Tipo() {}

    public Tipo(Integer id, String tipo) {
        this.id = id;
        this.tipo = tipo;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
