// ═══════════════════════════════════════════════════════════════════
// ARCHIVO 6: PromocionService.java
// ═══════════════════════════════════════════════════════════════════
package com.example.BarberiaLaClasica.service;
 
import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
@Service
public class PromocionService {
 
    private final PromocionRepository repo;
    private static final String UPLOAD_DIR = "uploads/promociones/";
 
    public PromocionService(PromocionRepository repo) {
        this.repo = repo;
    }
 
    public List<Promocion> listarTodas()   { return repo.findAllByOrderByOrdenAsc(); }
    public List<Promocion> listarActivas() { return repo.findByActivoTrueOrderByOrdenAsc(); }
 
    public Promocion guardar(MultipartFile archivo, String titulo, String descripcion,
                             String badge, Integer orden,
                             LocalDate fechaInicio, LocalDate fechaFin) throws IOException {
        String imagenUrl = null;
 
        if (archivo != null && !archivo.isEmpty()) {
            String ext = "";
            String orig = archivo.getOriginalFilename();
            if (orig != null && orig.contains(".")) ext = orig.substring(orig.lastIndexOf("."));
            String nombre = UUID.randomUUID() + ext;
            Path destino = Paths.get(UPLOAD_DIR + nombre);
            Files.createDirectories(destino.getParent());
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            imagenUrl = "/uploads/promociones/" + nombre;
        }
 
        Promocion promo = new Promocion();
        promo.setTitulo(titulo);
        promo.setDescripcion(descripcion);
        promo.setImagenUrl(imagenUrl);
        promo.setBadge(badge);
        promo.setOrden(orden != null ? orden : 0);
        promo.setActivo(true);
        promo.setFechaInicio(fechaInicio);
        promo.setFechaFin(fechaFin);
        return repo.save(promo);
    }
 
    public void toggleActivo(Long id) {
        Promocion p = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Promoción no encontrada: " + id));
        p.setActivo(!p.getActivo());
        repo.save(p);
    }
 
    public void eliminar(Long id) throws IOException {
        Promocion p = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Promoción no encontrada: " + id));
        if (p.getImagenUrl() != null) {
            Path archivo = Paths.get("uploads/promociones/" +
                p.getImagenUrl().replace("/uploads/promociones/", ""));
            Files.deleteIfExists(archivo);
        }
        repo.delete(p);
    }
}
 