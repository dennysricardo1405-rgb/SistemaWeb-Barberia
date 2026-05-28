package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "productos")
@Data // Genera getters, setters, toString, etc.
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
    
    private String imagen; // Guardará la ruta o nombre del archivo de la foto (Ej: "shampoo.jpg")
    
    @Column(nullable = false)
    private double precioVenta;

    // Inicializa en 0 como especificaste en tus requerimientos
    private int stock = 0; 

    private boolean activo = true;

    // Relación con Categoría: Un producto pertenece a una categoría o subcategoría
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}