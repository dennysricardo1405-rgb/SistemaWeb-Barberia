package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.service.ClienteService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public String listar(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaRegistro").descending());
        
        // MODIFICADO: Ahora pasamos la variable 'search' a la capa de servicio
        Page<Cliente> clientesPage = clienteService.listarTodosPaginado(pageable, search);

        model.addAttribute("clientesPage", clientesPage);
        model.addAttribute("clientes", clientesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clientesPage.getTotalPages());
        model.addAttribute("totalItems", clientesPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("search", search);

        // MANTENIDO: Tu ruta exacta de Thymeleaf
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
        // MANTENIDO: Redirección original
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

    @PostMapping("/actualizar/{id}")
    public String actualizarCliente(@PathVariable Long id, 
                                    @ModelAttribute Cliente cliente, 
                                    RedirectAttributes ra) {
        try {
            clienteService.actualizarDesdeAdmin(id, cliente); 
            ra.addFlashAttribute("exito", "Datos del cliente actualizados con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        // MANTENIDO: Redirección original
        return "redirect:/admin/cliente";
    }

    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {
        clienteService.cambiarEstado(id);
        // MANTENIDO: Redirección original
        return "redirect:/admin/cliente";
    }
}