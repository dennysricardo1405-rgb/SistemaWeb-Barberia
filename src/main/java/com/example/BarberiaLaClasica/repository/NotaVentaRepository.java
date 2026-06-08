package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.NotaVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaVentaRepository extends JpaRepository<NotaVenta, Long> {

    /**
     * Obtiene todas las notas de venta ordenadas por fecha descendente (más reciente primero)
     * con soporte de paginación.
     */
    Page<NotaVenta> findAllByOrderByFechaDesc(Pageable pageable);

}