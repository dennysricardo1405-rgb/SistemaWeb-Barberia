package com.example.BarberiaLaClasica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.NotaVenta;

public interface NotaVentaRepository extends JpaRepository<NotaVenta, Long> {
    List<NotaVenta> findAllByOrderByFechaDesc();
}
