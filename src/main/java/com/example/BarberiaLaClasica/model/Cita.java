package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data; // Importamos Lombok para automatizar el código limpio
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "citas")
@Data
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación corregida y limpia con la entidad Cliente (acepta NULL para clientes
    // libres)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbero_id", nullable = false)
    private Barbero barbero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    // Estados: 1 = PENDIENTE, 2 = EN_SILLA, 3 = COMPLETADA, 0 = CANCELADA
    @Column(nullable = false)
    private int estado = 1;

    @Column(name = "total_precio", nullable = false)
    private BigDecimal totalPrecio;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;
    @Column(name = "comprobante_pago", nullable = true)
    private String comprobantePago;

    // Constructor vacío requerido por JPA
    public Cita() {
    }
}