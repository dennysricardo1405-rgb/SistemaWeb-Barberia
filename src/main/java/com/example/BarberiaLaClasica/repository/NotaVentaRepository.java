package com.example.BarberiaLaClasica.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.NotaVenta;

@Repository
public interface NotaVentaRepository extends JpaRepository<NotaVenta, Long> {

    // Cambiamos la query problemática por un filtro de rango limpio
    List<NotaVenta> findByBarberoAndFechaBetween(Barbero barbero, LocalDateTime inicio, LocalDateTime fin);
}