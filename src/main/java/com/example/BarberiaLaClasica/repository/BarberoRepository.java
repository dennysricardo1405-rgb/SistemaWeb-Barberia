package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Barbero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BarberoRepository extends JpaRepository<Barbero, Long> {
    List<Barbero> findByEstado(int estado);
}