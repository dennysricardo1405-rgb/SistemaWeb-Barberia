package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // Alerta de stock: Busca productos activos cuyo stock sea menor o igual al límite (3 unidades)
    List<Producto> findByStockLessThanEqualAndActivoTrue(int limite);

    // Para el catálogo online público: Solo productos de barbería que estén activos
    // (Filtra buscando el nombre de la categoría padre o de la categoría misma)
    List<Producto> findByCategoriaNombreAndActivoTrue(String nombreCategoria);
    
    // Listar todos los productos activos
    List<Producto> findByActivoTrue();
    @Query("SELECT p FROM Producto p " +
       "JOIN FETCH p.categoria c " +
       "LEFT JOIN FETCH c.padre pad " +
       "WHERE p.activo = true AND (c.nombre = :nombreCat OR pad.nombre = :nombreCat)")
List<Producto> findProductosParaCatalogoPublico(@Param("nombreCat") String nombreCat);
}