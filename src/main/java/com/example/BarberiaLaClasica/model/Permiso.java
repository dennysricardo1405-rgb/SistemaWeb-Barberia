package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "permisos")
@Data
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_permiso") // Según tu imagen
    private String nombre;

    private String descripcion;

    // CONSTRUCTOR VACÍO (Obligatorio para evitar el error 500)
    public Permiso() {}

}