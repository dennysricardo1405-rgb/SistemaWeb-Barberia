package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.service.ProductoService;
import com.example.BarberiaLaClasica.service.PromocionService;
import com.example.BarberiaLaClasica.service.ServicioService;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/promociones")
public class PromocionController {

    @Autowired
    private PromocionService promocionService;
    @Autowired
    private PromocionRepository promocionRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ServicioService servicioService;
    @Autowired
    private ProductoService productoService;

    // ── 1. Listar todas las promociones en el Panel ─────────────────────────
    @GetMapping
    public String listarPromociones(Model model) {
        model.addAttribute("promocion", new Promocion());
        model.addAttribute("promociones", promocionService.listarTodas());
        model.addAttribute("servicios", servicioService.listarTodos());
        model.addAttribute("productos", productoService.listarTodos());

        // CORRECCIÓN: Filtramos para obtener las Subcategorías válidas
        List<Categoria> subcategoriasProductos = categoriaRepository.findAll().stream()
                // 1. Asegura que sea una subcategoría (tiene un padre asignado)
                .filter(cat -> cat.getPadre() != null)
                // 2. Opcional: Filtra para asegurarte de que el nombre del papá sea el de
                // Productos
                .filter(cat -> "Productos de Barbería".equalsIgnoreCase(cat.getPadre().getNombre())
                        || "Productos".equalsIgnoreCase(cat.getPadre().getNombre()))
                .collect(Collectors.toList());

        // Enviamos la lista filtrada con el nombre exacto que espera tu archivo HTML
        model.addAttribute("categoriasConSubcategorias", subcategoriasProductos);

        return "promociones/lista"; // O la ruta exacta de tu vista
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("promocion", new Promocion());

        // Filtrado Inteligente basado en tu entidad Autorreferencial
        List<Categoria> categoriasFiltradas = categoriaRepository.findAll().stream()
                // 1. Asegura que sea una Categoría Padre/Principal (no tiene papá)
                .filter(cat -> cat.getPadre() == null)
                // 2. Asegura que tenga subcategorías asignadas (lista no vacía)
                .filter(cat -> cat.getSubcategorias() != null && !cat.getSubcategorias().isEmpty())
                .collect(Collectors.toList());

        model.addAttribute("categoriasConSubcategorias", categoriasFiltradas);
        return "admin/promociones-formulario";
    }

    // ── 2. Guardar o Editar Promoción ───────────────────────────────────────
    @PostMapping("/guardar")
    public String guardarPromocion(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String tipoPromocion, // "SERVICIO" o "PRODUCTO"
            @RequestParam(required = false) BigDecimal porcentajeDescuento, // 🛟 Cambiado a false para evitar Error 400
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio, // 🛟
                                                                                                                           // Cambiado
                                                                                                                           // a
                                                                                                                           // false
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, // 🛟
                                                                                                                        // Cambiado
                                                                                                                        // a
                                                                                                                        // false
            @RequestParam(defaultValue = "0") int minimoVisitasRequeridas,
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long categoriaId,
            RedirectAttributes ra) {

        try {
            // ── 1. VALIDACIÓN DE CAMPOS VACÍOS (Evita errores de Spring) ──
            if (nombre == null || nombre.trim().isEmpty() ||
                    descripcion == null || descripcion.trim().isEmpty() ||
                    tipoPromocion == null || tipoPromocion.trim().isEmpty() ||
                    porcentajeDescuento == null || fechaInicio == null || fechaFin == null) {

                throw new RuntimeException("Todos los campos obligatorios deben ser completados.");
            }

            // ── 2. VALIDACIÓN DE REGLA DE NEGOCIO: MÁXIMO 4 MESES VIGENCIA ──
            if (fechaInicio.isAfter(fechaFin)) {
                throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin.");
            }

            if (fechaInicio.plusMonths(4).isBefore(fechaFin)) {
                throw new RuntimeException("La vigencia máxima permitida para una promoción es de 4 meses.");
            }

            // Lógica existente de persistencia
            Promocion promo;
            if (id != null) {
                promo = promocionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Promoción no encontrada"));
            } else {
                promo = new Promocion();
            }

            promo.setNombre(nombre);
            promo.setDescripcion(descripcion);
            promo.setTipoPromocion(tipoPromocion);
            promo.setPorcentajeDescuento(porcentajeDescuento);
            promo.setFechaInicio(fechaInicio);
            promo.setFechaFin(fechaFin);
            promo.setMinimoVisitasRequeridas(minimoVisitasRequeridas);

            // Asignar relaciones según el tipo de promoción
            if ("SERVICIO".equalsIgnoreCase(tipoPromocion)) {
                if (servicioId != null) {
                    promo.setServicio(servicioRepository.findById(servicioId).orElse(null));
                }
                promo.setProducto(null);
                promo.setCategoria(null);
            } else { // "PRODUCTO"
                if (productoId != null) {
                    promo.setProducto(productoRepository.findById(productoId).orElse(null));
                    promo.setCategoria(null);
                } else if (categoriaId != null) {
                    promo.setCategoria(categoriaRepository.findById(categoriaId).orElse(null));
                    promo.setProducto(null);
                }
                promo.setServicio(null);
            }

            promocionRepository.save(promo);
            ra.addFlashAttribute("exito", "Promoción guardada correctamente.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/promociones";
    }

    // ── 3. Pausar o Activar (Toggle) sin borrar ─────────────────────────────
    @GetMapping("/toggle/{id}")
    public String togglePromocion(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promocionService.toggleActivo(id);
            ra.addFlashAttribute("exito", "Estado de la promoción actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promociones";
    }

    // ── 4. Eliminar definitivamente ─────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminarPromocion(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promocionService.eliminar(id);
            ra.addFlashAttribute("exito", "Promoción eliminada con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promociones";
    }
}