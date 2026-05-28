package com.example.BarberiaLaClasica.controller;
 
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
@Controller
@RequestMapping("/admin/servicios")
public class ServicioController {
 
    @Autowired
    private ServicioService servicioService;
 
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicios", servicioService.listarTodos());
        return "servicios/servicios-lista";
    }
 
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Servicio servicio, RedirectAttributes ra) {
        try {
            servicioService.guardar(servicio);
            ra.addFlashAttribute("exito", "Servicio guardado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }
 
    @PostMapping("/editar")
    public String editar(@ModelAttribute Servicio servicio, RedirectAttributes ra) {
        try {
            servicioService.guardar(servicio);
            ra.addFlashAttribute("exito", "Servicio actualizado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }
 
    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {
        servicioService.cambiarEstado(id);
        return "redirect:/admin/servicios";
    }
}