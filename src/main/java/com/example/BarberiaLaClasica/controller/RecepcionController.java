package com.example.BarberiaLaClasica.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import com.example.BarberiaLaClasica.service.BarberoService;
import com.example.BarberiaLaClasica.service.ProductoService;
import com.example.BarberiaLaClasica.service.RecepcionService;
import com.example.BarberiaLaClasica.service.ClienteService;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.ConsumoSilla;
import com.example.BarberiaLaClasica.model.NotaVenta;

@Controller
@RequestMapping("/secretario")
public class RecepcionController {

    @Autowired
    private BarberoService barberoService;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private RecepcionService recepcionService;

    // ── Panel principal ───────────────────────────────────────────────────────
    @GetMapping("/recepcion")
    public String verPanelRecepcion(Model model) {
        List<Barbero> barberos = barberoService.listarTodos();

        // Para cada barbero, busca si tiene reserva hoy o sesión activa
        Map<Long, Cita> reservasHoy = new HashMap<>();
        Map<Long, Boolean> enSesion = new HashMap<>();

        for (Barbero b : barberos) {
            recepcionService.getCitaReservaHoy(b.getId())
                    .ifPresent(c -> reservasHoy.put(b.getId(), c));
            recepcionService.getSessionActiva(b.getId())
                    .ifPresent(s -> enSesion.put(b.getId(), true));
        }

        model.addAttribute("barberos", barberos);
        model.addAttribute("reservasHoy", reservasHoy);
        model.addAttribute("enSesion", enSesion);
        model.addAttribute("servicios", servicioRepository.findByEstado(1));
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("clientesExistentes", clienteService.listarTodos());
        return "secretario/recepcion";
    }

    // ── Atender reserva existente ─────────────────────────────────────────────
    @PostMapping("/recepcion/atender-reserva/{barberoId}")
    public String atenderReserva(@PathVariable Long barberoId, RedirectAttributes ra) {
        try {
            recepcionService.atenderReserva(barberoId);
            ra.addFlashAttribute("exito", "Sesión iniciada desde reserva.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/secretario/recepcion";
    }

    // ── Walk-in: ocupar silla manualmente ────────────────────────────────────
    @PostMapping("/recepcion/ocupar-silla")
public String ocuparSilla(
        @RequestParam Long barberoId,
        @RequestParam Long servicioId,
        @RequestParam(required = false) Long clienteId,  // ← agregar esto
        RedirectAttributes ra) {
    try {
        recepcionService.ocuparSillaWalkin(barberoId, clienteId, servicioId);  // ← ya no null
        ra.addFlashAttribute("exito", "¡Estación abierta!");
    } catch (Exception e) {
        ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/secretario/recepcion";
}

    // ── API consumos ──────────────────────────────────────────────────────────
    @GetMapping("/recepcion/api-consumos/{barberoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consumos(@PathVariable Long barberoId) {
        List<ConsumoSilla> consumos = recepcionService.obtenerConsumos(barberoId);
        double total = consumos.stream().mapToDouble(ConsumoSilla::getSubtotal).sum();

        List<Map<String, Object>> items = consumos.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "descripcion", c.getProducto().getNombre(),
                "cantidad", c.getCantidad(),
                "precioUnit", c.getProducto().getPrecioVenta(),
                "subtotal", c.getSubtotal())).toList();

        // Agrega servicio y cliente de la sesión activa
        Map<String, Object> response = new HashMap<>();
        response.put("consumos", items);
        response.put("total", total);

        recepcionService.getSessionActiva(barberoId).ifPresent(s -> {
            response.put("servicio", Map.of(
                    "nombre", s.getServicio().getNombre(),
                    "precio", s.getServicio().getPrecio()));
            if (s.getCliente() != null) {
                response.put("cliente", Map.of(
                        "nombres", s.getCliente().getNombres(),
                        "apellidos", s.getCliente().getApellidos(),
                        "dni", s.getCliente().getDni()));
            }
        });

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recepcion/api-consumos/agregar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> agregar(
            @RequestParam Long barberoId,
            @RequestParam Long productoId,
            @RequestParam int cantidad) {
        try {
            recepcionService.agregarProducto(barberoId, productoId, cantidad);
            return ResponseEntity.ok(Map.of("mensaje", "Producto agregado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/recepcion/api-consumos/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        recepcionService.quitarConsumo(id);
        return ResponseEntity.ok(Map.of("mensaje", "Eliminado"));
    }

    // ── Finalizar ─────────────────────────────────────────────────────────────
    @GetMapping("/recepcion/finalizar-pago/{barberoId}")
    public String finalizar(@PathVariable Long barberoId, RedirectAttributes ra) {
        try {
            NotaVenta nota = recepcionService.finalizarAtencion(barberoId);
            ra.addFlashAttribute("exito", "Nota de venta #" + nota.getId() + " generada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/secretario/recepcion";
    }

    // ── Notas de venta ────────────────────────────────────────────────────────
    @GetMapping("/recepcion/notas-venta")
    public String notas(Model model) {
        model.addAttribute("notas", recepcionService.listarNotas());
        return "secretario/notas-venta";
    }

    @PostMapping("/recepcion/asociar-cliente")
    @ResponseBody
    public ResponseEntity<Map<String, String>> asociarCliente(
            @RequestParam Long barberoId,
            @RequestParam Long clienteId) {
        try {
            recepcionService.asociarCliente(barberoId, clienteId);
            return ResponseEntity.ok(Map.of("mensaje", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}