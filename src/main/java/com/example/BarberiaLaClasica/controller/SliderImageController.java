package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.service.ConfiguracionSitioService;
import com.example.BarberiaLaClasica.service.PromocionService;
import com.example.BarberiaLaClasica.service.SliderImageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/admin/slider")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SliderImageController {

    private final SliderImageService        service;
    private final ConfiguracionSitioService configService;
    private final PromocionService          promoService;

    public SliderImageController(SliderImageService service,
                                 ConfiguracionSitioService configService,
                                 PromocionService promoService) {
        this.service       = service;
        this.configService = configService;
        this.promoService  = promoService;
    }

    // ════════════════════════════════════════════════
    // GET — carga la vista unificada con todo el contenido
    // ════════════════════════════════════════════════
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("sliderImagenes", service.listarTodas());
        model.addAttribute("config",         configService.obtenerMapa());
        model.addAttribute("promociones",    promoService.listarTodas());
        return "slider/lista";
    }

    // ════════════════════════════════════════════════
    // SLIDER — guardar, eliminar, toggle
    // ════════════════════════════════════════════════
    @PostMapping("/guardar")
    public String guardar(@RequestParam("file")       MultipartFile file,
                          @RequestParam("titulo")      String titulo,
                          @RequestParam("descripcion") String descripcion,
                          @RequestParam("orden")       Integer orden,
                          RedirectAttributes ra) {
        try {
            if (file.isEmpty()) {
                ra.addFlashAttribute("error", "Debes seleccionar una imagen.");
                return "redirect:/admin/slider";
            }
            service.guardar(file, titulo, descripcion, orden);
            ra.addFlashAttribute("success", "Imagen agregada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al subir: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("success", "Imagen eliminada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }

    @PostMapping("/toggle/{id}")
    public String toggleSlider(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.toggleActivo(id);
            ra.addFlashAttribute("success", "Visibilidad del slide actualizada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }

    // ════════════════════════════════════════════════
    // CONFIGURACIÓN — guardar identidad, contacto, redes, horarios
    // ════════════════════════════════════════════════
    @PostMapping("/config/guardar")
    public String guardarConfig(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            RedirectAttributes ra) {
        try {
            params.remove("_csrf");
            params.remove("grupo");
            configService.guardarGrupo(params, logoFile);
            ra.addFlashAttribute("success", "Configuración guardada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }

    // ════════════════════════════════════════════════
    // PROMOCIONES — guardar, toggle, eliminar
    // ════════════════════════════════════════════════
    @PostMapping("/promociones/guardar")
    public String guardarPromo(
            @RequestParam("file")                                         MultipartFile file,
            @RequestParam("titulo")                                       String titulo,
            @RequestParam(value = "descripcion", defaultValue = "")      String descripcion,
            @RequestParam(value = "badge",       defaultValue = "NUEVO") String badge,
            @RequestParam(value = "orden",       defaultValue = "1")     Integer orden,
            @RequestParam(value = "fechaInicio", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)           LocalDate fechaInicio,
            @RequestParam(value = "fechaFin",    required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)           LocalDate fechaFin,
            RedirectAttributes ra) {
        try {
            promoService.guardar(file, titulo, descripcion, badge, orden, fechaInicio, fechaFin);
            ra.addFlashAttribute("success", "Promoción guardada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }

    @PostMapping("/promociones/toggle/{id}")
    public String togglePromo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promoService.toggleActivo(id);
            ra.addFlashAttribute("success", "Estado de promoción actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }

    @PostMapping("/promociones/eliminar/{id}")
    public String eliminarPromo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promoService.eliminar(id);
            ra.addFlashAttribute("success", "Promoción eliminada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/admin/slider";
    }
}