package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras_proveedor")
@Data
public class CompraProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- CORRECCIÓN AQUÍ: Cambiamos el String por la relación real @ManyToOne ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    private TipoCompra tipoCompra; // PAQUETE o UNIDAD

    private Integer cantidadPaquetes;    // Puede ser null si es unidad suelta
    private Integer unidadesPorPaquete;  // Puede ser null si es unidad suelta
    
    private int totalUnidades;           // Calculado automáticamente
    private double precioCompraPaquete;  // Si es Unidad, aquí va el precio de esa unidad
    private double precioVentaUnidad;
    private double totalInvertido;       // Calculado automáticamente
    
    private LocalDateTime fechaCompra;

    @PrePersist
    protected void onCreate() {
        this.fechaCompra = LocalDateTime.now();
    }

    public enum TipoCompra {
        PAQUETE, UNIDAD
    }

    // --- MÉTODOS MANUALES PARA EVITAR CONFLICTOS CON THYMELEAF ---
    public Proveedor getProveedor() {
        return this.proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Producto getProducto() {
        return this.producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}