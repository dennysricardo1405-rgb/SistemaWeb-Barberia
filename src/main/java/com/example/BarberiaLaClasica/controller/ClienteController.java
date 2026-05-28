package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.service.ClienteService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        return "cliente/clientes-lista";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Cliente cliente,
            @RequestParam("passwordPlana") String passwordPlana,
            RedirectAttributes ra) {
        try {
            clienteService.crearDesdeAdmin(cliente, passwordPlana);
            ra.addFlashAttribute("exito", "Cliente registrado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/cliente";
    }

    @PostMapping("/guardar-rapido")
    public ResponseEntity<?> guardarRapido(@RequestBody Map<String, Object> datos) {
        try {
            Cliente c = new Cliente();
            c.setDni((String) datos.get("dni"));
            c.setNombres((String) datos.get("nombres"));
            c.setApellidos((String) datos.get("apellidos"));
            c.setTelefono((String) datos.get("telefono"));
            String password = "B" + datos.get("dni");
            clienteService.crearDesdeAdmin(c, password);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", c.getId());
            resp.put("dni", c.getDni());
            resp.put("nombres", c.getNombres());
            resp.put("apellidos", c.getApellidos());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {
        clienteService.cambiarEstado(id);
        return "redirect:/admin/cliente";
    }
}