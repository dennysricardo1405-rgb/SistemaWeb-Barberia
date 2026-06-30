package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PromocionHelper {

    private final PromocionRepository promocionRepository;

    public PromocionHelper(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    /**
     * Calcula el precio de un servicio aplicando el mejor descuento vigente.
     */
    public double calcularPrecioServicio(Servicio servicio) {
        double precioOriginal = servicio.getPrecio().doubleValue();
        List<Promocion> vigentes = promocionRepository.findPromocionesVigentes(LocalDateTime.now());

        double maxDescuento = 0.0;

        for (Promocion p : vigentes) {
            if ("SERVICIO".equalsIgnoreCase(p.getTipoPromocion()) && p.getServicio() != null) {
                if (p.getServicio().getId().equals(servicio.getId())) {
                    double desc = p.getPorcentajeDescuento().doubleValue();
                    if (desc > maxDescuento) {
                        maxDescuento = desc;
                    }
                }
            }
        }

        if (maxDescuento > 0) {
            double descuento = precioOriginal * (maxDescuento / 100.0);
            return BigDecimal.valueOf(precioOriginal - descuento)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return precioOriginal;
    }

    /**
     * Calcula el precio de un producto aplicando la promoción por ID o por Categoría.
     */
    public double calcularPrecioProducto(Producto producto) {
        double precioOriginal = producto.getPrecioVenta();
        List<Promocion> vigentes = promocionRepository.findPromocionesVigentes(LocalDateTime.now());

        double maxDescuento = 0.0;

        for (Promocion p : vigentes) {
            if ("PRODUCTO".equalsIgnoreCase(p.getTipoPromocion())) {
                // Caso 1: Aplica al producto específico
                if (p.getProducto() != null && p.getProducto().getId().equals(producto.getId())) {
                    double desc = p.getPorcentajeDescuento().doubleValue();
                    if (desc > maxDescuento) maxDescuento = desc;
                }
                // Caso 2: Aplica a toda la categoría del producto
                else if (p.getCategoria() != null && producto.getCategoria() != null 
                        && p.getCategoria().getId().equals(producto.getCategoria().getId())) {
                    double desc = p.getPorcentajeDescuento().doubleValue();
                    if (desc > maxDescuento) maxDescuento = desc;
                }
            }
        }

        if (maxDescuento > 0) {
            double descuento = precioOriginal * (maxDescuento / 100.0);
            return BigDecimal.valueOf(precioOriginal - descuento)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return precioOriginal;
    }
}