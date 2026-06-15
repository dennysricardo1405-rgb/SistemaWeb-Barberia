package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.service.CitaService;
import com.example.BarberiaLaClasica.service.ClienteService;
import com.example.BarberiaLaClasica.service.RecepcionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/secretario")
public class SecretarioController {

    @Autowired
    private CitaService citaService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private RecepcionService recepcionService;

    // 1. DASHBOARD DEL SECRETARIO (Vista Principal con la Agenda de Citas)
    @GetMapping("/dashboard")
    public String dashboardSecretario(Model model) {
        List<Cita> citasHoy = citaService.listarCitasDeHoy();
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("totalCitasHoy", citasHoy.size());
        model.addAttribute("citasPendientesCount", citaService.listarPendientes().size()); // ← agrega esta línea
        return "secretario/dashboard";
    }

    // Asegúrate de tener inyectado tu servicio de clientes aquí arriba
    // @Autowired private ClienteService clienteService;

    // ── MANTENIMIENTO DE CLIENTES REUTILIZANDO LA VISTA DE TU CONTROLLER ──

    // 1. Listar Clientes
    @GetMapping("/cliente")
    public String listarClientesParaSecretario(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("cliente", new Cliente()); // Thymeleaf requiere el objeto vacío para el modal de crear

        // USAMOS TU RUTA EXACTA DE PLANTILLA CORREGIDA 🎯
        return "cliente/clientes-lista";
    }

    // 2. Guardar Cliente desde la Recepción
    @PostMapping("/cliente/guardar")
    public String guardarClienteDesdeSecretario(
            @ModelAttribute Cliente cliente,
            @RequestParam("passwordPlana") String passwordPlana,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            clienteService.crearDesdeAdmin(cliente, passwordPlana);
            ra.addFlashAttribute("exito", "Cliente registrado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        // Redirige al mismo entorno del secretario
        return "redirect:/secretario/cliente";
    }

    // 3. Cambiar Estado (Activar/Inactivar)
    @GetMapping("/cliente/estado/{id}")
    public String cambiarEstadoClienteDesdeSecretario(@PathVariable Long id) {
        clienteService.cambiarEstado(id);
        return "redirect:/secretario/cliente";
    }

    @GetMapping("/citas")
    public String gestionCitas(Model model) {
        model.addAttribute("citasPendientes", citaService.listarPendientes());
        model.addAttribute("citasHoy", citaService.listarCitasDeHoy());
        return "secretario/citas-gestion";
    }

    // ── Aceptar cita + enviar WhatsApp ────────────────────────────────────────
    @PostMapping("/citas/{id}/aceptar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> aceptarCita(@PathVariable Long id) {
        try {
            citaService.aceptarCita(id);
            return ResponseEntity.ok(Map.of("mensaje", "Cita confirmada. Se notificó al cliente por WhatsApp."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Cancelar cita ─────────────────────────────────────────────────────────
    @PostMapping("/citas/{id}/cancelar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cancelarCita(@PathVariable Long id) {
        try {
            citaService.cancelarCita(id);
            return ResponseEntity.ok(Map.of("mensaje", "Cita cancelada."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/recepcion/api-estado-barbero/{barberoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> estadoBarbero(
            @PathVariable Long barberoId) {

        Map<String, Object> resp = new HashMap<>();

        recepcionService.getCitaReservaHoy(barberoId).ifPresentOrElse(cita -> {
            LocalTime ahora = LocalTime.now();
            LocalTime horaRes = cita.getHoraInicio();
            long minutos = java.time.Duration.between(ahora, horaRes).toMinutes();

            resp.put("tieneReserva", true);
            resp.put("horaReserva", horaRes.toString());
            resp.put("minutosRestantes", minutos);
            resp.put("bloqueado", minutos >= 0 && minutos <= 30);
            resp.put("cliente", cita.getCliente() != null
                    ? cita.getCliente().getNombres() + " " + cita.getCliente().getApellidos()
                    : "—");
        }, () -> {
            resp.put("tieneReserva", false);
            resp.put("bloqueado", false);
        });

        return ResponseEntity.ok(resp);
    }
}