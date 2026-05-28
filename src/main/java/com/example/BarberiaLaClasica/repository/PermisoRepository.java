package com.example.BarberiaLaClasica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.BarberiaLaClasica.model.Permiso;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    
} 