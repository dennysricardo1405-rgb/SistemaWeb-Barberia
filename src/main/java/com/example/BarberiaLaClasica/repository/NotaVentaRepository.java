package com.example.BarberiaLaClasica.repository;


import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.NotaVenta;


@Repository
public interface NotaVentaRepository extends JpaRepository<NotaVenta, Long> {

    @Query("""
    SELECT n FROM NotaVenta n
    WHERE n.barbero = :barbero
    AND YEAR(n.fecha) = :anio
    AND MONTH(n.fecha) = :mes
    ORDER BY n.fecha ASC
""")
List<NotaVenta> findByBarberoAndPeriodo(
    @Param("barbero") Barbero barbero,
    @Param("anio") int anio,
    @Param("mes") int mes);
    Page<NotaVenta> findAllByOrderByFechaDesc(Pageable pageable);


    @Query("SELECT COALESCE(SUM(n.total), 0) FROM NotaVenta n " +
           "WHERE n.fecha >= :inicio AND n.fecha < :fin")
    Double sumTotalEntreFechas(@Param("inicio") LocalDateTime inicio,
                                @Param("fin") LocalDateTime fin);

}

    // 1. Para la Paginación de Notas de Venta en Recepción
    Page<NotaVenta> findAll(Pageable pageable);

    // 2. Para el filtro de Sueldos por Comisión (Usa funciones nativas de año y mes de MySQL)
    @Query("SELECT n FROM NotaVenta n WHERE n.barbero = :barbero AND YEAR(n.fecha) = :anio AND MONTH(n.fecha) = :mes")
    List<NotaVenta> findByBarberoAndPeriodo(
            @Param("barbero") Barbero barbero, 
            @Param("anio") int anio, 
            @Param("mes") int mes);
}
