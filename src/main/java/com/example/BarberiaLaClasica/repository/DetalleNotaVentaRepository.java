package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.DetalleNotaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetalleNotaVentaRepository extends JpaRepository<DetalleNotaVenta, Long> {

    // Usado en la pestaña "Productos" — filtra solo por tipo (PRODUCTO o SERVICIO)
    @Query("SELECT d FROM DetalleNotaVenta d " +
           "WHERE d.tipo = :tipo " +
           "AND d.notaVenta.fecha BETWEEN :inicio AND :fin")
    List<DetalleNotaVenta> findByTipoAndFechaBetween(
            @Param("tipo") String tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Usado en la pestaña "Todos" — trae todo sin importar el tipo
    @Query("SELECT d FROM DetalleNotaVenta d WHERE d.notaVenta.fecha BETWEEN :inicio AND :fin")
    List<DetalleNotaVenta> findByFechaBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
}