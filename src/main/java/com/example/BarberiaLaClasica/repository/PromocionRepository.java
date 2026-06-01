// ═══════════════════════════════════════════════════════════════════
// ARCHIVO 4: PromocionRepository.java
// ═══════════════════════════════════════════════════════════════════
package com.example.BarberiaLaClasica.repository;
 
import com.example.BarberiaLaClasica.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
 
@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    List<Promocion> findByActivoTrueOrderByOrdenAsc();
    List<Promocion> findAllByOrderByOrdenAsc();
}