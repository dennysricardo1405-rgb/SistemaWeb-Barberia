package com.example.BarberiaLaClasica.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "consumos_silla")
@Data
public class ConsumoSilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SillaSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private int cantidad;
    private double subtotal;
}
