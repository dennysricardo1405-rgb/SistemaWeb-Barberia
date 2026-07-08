package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.CompraProveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraProveedorRepository extends JpaRepository<CompraProveedor, Long> {
    
    // Este método traerá las compras paginadas y ordenadas por ID descendente (las más recientes primero)
    Page<CompraProveedor> findAllByOrderByIdDesc(Pageable pageable);
}