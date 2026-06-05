package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.service.BarberoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/barberos")
public class BarberoController {

    @Autowired
    private BarberoService barberoService;

    private static final String[] DIAS = {"MARTES", "MIERCOLES"};

    // ── Lista con Paginación ─────────────────────────────────────
    @GetMapping
    public String lista(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());

        Page<Barbero> barberosPage = barberoService.buscar(search, pageable);

        model.addAttribute("barberos", barberosPage.getContent());
        model.addAttribute("barberosPage", barberosPage);
        model.addAttribute("dias", DIAS);
        model.addAttribute("search", search);

        return "barberos/lista";
    }

    // ── Guardar nuevo ─────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String nombre,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String diaLibre,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            RedirectAttributes redirect) {
        try {
            Barbero b = new Barbero();
            b.setNombre(nombre);
            b.setEspecialidad(especialidad);
            b.setTelefono(telefono);
            b.setDiaLibre(diaLibre != null ? diaLibre : "MARTES");
            barberoService.guardar(b, foto);
            redirect.addFlashAttribute("exito", "Barbero registrado correctamente ✓");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
        }
        return "redirect:/admin/barberos";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String diaLibre,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            RedirectAttributes redirect) {
        try {
            Barbero datos = new Barbero();
            datos.setNombre(nombre);
            datos.setEspecialidad(especialidad);
            datos.setTelefono(telefono);
            datos.setDiaLibre(diaLibre != null ? diaLibre : "MARTES");
            datos.setEstado(1);
            barberoService.actualizar(id, datos, foto);
            redirect.addFlashAttribute("exito", "Barbero actualizado correctamente ✓");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/admin/barberos";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            barberoService.cambiarEstado(id);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al cambiar estado");
        }
        return "redirect:/admin/barberos";
    }
}