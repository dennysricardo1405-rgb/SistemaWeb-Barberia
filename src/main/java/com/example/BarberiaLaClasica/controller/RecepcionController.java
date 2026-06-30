package com.example.BarberiaLaClasica.controller;

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

import java.util.*;

import jakarta.transaction.Transactional;

import com.example.BarberiaLaClasica.service.BarberoService;
import com.example.BarberiaLaClasica.service.ProductoService;
import com.example.BarberiaLaClasica.service.PromocionHelper;
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
    @Autowired
    private PromocionHelper promocionHelper;

    // ── Panel principal con PAGINACIÓN ───────────────────────────────────────
    @GetMapping("/recepcion")
    public String verPanelRecepcion(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Barbero> barberos = barberoService.listarTodos();

        Map<Long, Cita> reservasHoy = new HashMap<>();
        Map<Long, Boolean> enSesion = new HashMap<>();

        for (Barbero b : barberos) {
            recepcionService.getCitaReservaHoy(b.getId())
                    .ifPresent(c -> reservasHoy.put(b.getId(), c));
            recepcionService.getSessionActiva(b.getId())
                    .ifPresent(s -> enSesion.put(b.getId(), true));
        }

        // Paginación para Reservas Web de Hoy
        Pageable pageable = PageRequest.of(page, size, Sort.by("horaInicio").ascending());
        Page<Cita> reservasPage = recepcionService.listarReservasHoyPaginadas(pageable);

        model.addAttribute("barberos", barberos);
        model.addAttribute("reservasHoy", reservasHoy);
        model.addAttribute("enSesion", enSesion);
        model.addAttribute("servicios", servicioRepository.findByEstado(1));
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("clientesExistentes", clienteService.listarTodos());

        // Atributos de paginación
        model.addAttribute("reservasPage", reservasPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservasPage.getTotalPages());
        model.addAttribute("totalItems", reservasPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("promoHelper", promocionHelper);
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
            @RequestParam(required = false) Long clienteId,
            RedirectAttributes ra) {
        try {
            recepcionService.ocuparSillaWalkin(barberoId, clienteId, servicioId);
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

        // 1. Mapeamos los productos consumidos (aquí se mantiene el precio normal de
        // venta)
        List<Map<String, Object>> items = consumos.stream().map(c -> {
            double precioOriginal = c.getProducto().getPrecioVenta();
            double subtotalCalculado = precioOriginal * c.getCantidad();

            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("descripcion", c.getProducto().getNombre());
            item.put("cantidad", c.getCantidad());
            item.put("precioUnit", precioOriginal);
            item.put("precioOriginal", precioOriginal);
            item.put("subtotal", subtotalCalculado);
            return item;
        }).toList();

        double totalProductos = items.stream().mapToDouble(i -> (double) i.get("subtotal")).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("consumos", items);
        response.put("anticipoYape", 0.0);
        response.put("codigoYape", "");

        final double[] totalFinal = { totalProductos };

        // 2. Lógica Condicional para el Servicio de la Silla
        recepcionService.getSessionActiva(barberoId).ifPresent(s -> {
            double precioServicioOriginal = s.getServicio().getPrecio().doubleValue();
            double precioServicioFinal = precioServicioOriginal; // Por defecto tarifa normal (S/ 35.00)

            // REVISIÓN ESTRICTA: Debe tener cita, la cita debe existir en BD y no ser un
            // objeto vacío
            if (s.getCita() != null && s.getCita().getId() != null) {

                // Adicionalmente, verificamos que la cita no haya sido creada como "Walk-in"
                // manual
                // Si el monto de Yape de la reserva web existe y es mayor a 0, confirmamos que
                // es reserva web
                if (s.getCita().getMontoYape() != null
                        && s.getCita().getMontoYape().compareTo(java.math.BigDecimal.ZERO) > 0) {

                    // SÓLO AQUÍ se aplica el precio con descuento web (S/ 17.50)
                    precioServicioFinal = promocionHelper.calcularPrecioServicio(s.getServicio());

                    response.put("anticipoYape", s.getCita().getMontoYape().doubleValue());
                    response.put("codigoYape", s.getCita().getCodigoYape() != null ? s.getCita().getCodigoYape() : "");
                }
            }

            totalFinal[0] += precioServicioFinal;

            Map<String, Object> servicioMap = new HashMap<>();
            servicioMap.put("nombre", s.getServicio().getNombre());
            servicioMap.put("precio", precioServicioFinal);
            servicioMap.put("precioOriginal", precioServicioOriginal);
            response.put("servicio", servicioMap);

            if (s.getCliente() != null) {
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("nombres", s.getCliente().getNombres());
                clienteMap.put("apellidos", s.getCliente().getApellidos());
                clienteMap.put("dni", s.getCliente().getDni());
                response.put("cliente", clienteMap);
            }
        });

        response.put("total", totalFinal[0]);

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
    public String finalizar(
            @PathVariable Long barberoId,
            @RequestParam(required = false, defaultValue = "EFECTIVO") String metodoPago,
            @RequestParam(required = false, defaultValue = "0") double montoYape,
            @RequestParam(required = false) String codigoYape,
            RedirectAttributes ra) {
        try {
            // 1. Interceptamos la sesión activa de la silla usando tu servicio nativo
            recepcionService.getSessionActiva(barberoId).ifPresent(s -> {
                double precioServicioOriginal = s.getServicio().getPrecio().doubleValue();
                double precioServicioFinal = precioServicioOriginal; // Por defecto S/ 35.00

                // 2. VALIDADOR ESTRICTO: ¿Tiene una cita web con pago anticipado real?
                if (s.getCita() != null && s.getCita().getId() != null) {
                    var cita = s.getCita();
                    if (cita.getMontoYape() != null && cita.getMontoYape().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        // SÓLO si cumple, le damos la tarifa con descuento web (S/ 17.50)
                        precioServicioFinal = promocionHelper.calcularPrecioServicio(s.getServicio());
                    }
                }

                // 3. Forzamos el precio calculado en el objeto de la sesión antes de guardar
                s.getServicio().setPrecio(java.math.BigDecimal.valueOf(precioServicioFinal));
            });

            // 4. Tu flujo original intacto procesará el cobro con el valor correcto
            NotaVenta nota = recepcionService.finalizarAtencion(
                    barberoId, metodoPago, montoYape, codigoYape);

            ra.addFlashAttribute("exito", "Nota de venta #" + nota.getId() + " generada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/secretario/recepcion";
    }

    // ── Notas de venta ────────────────────────────────────────────────────────
    @GetMapping("/recepcion/notas-venta/{id}/detalle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalleNota(@PathVariable Long id) {
        NotaVenta nota = recepcionService.obtenerNota(id);

        List<Map<String, Object>> detalles = nota.getDetalles().stream().map(d -> Map.<String, Object>of(
                "descripcion", d.getDescripcion(),
                "cantidad", d.getCantidad(),
                "precioUnitario", d.getPrecioUnitario(),
                "subtotal", d.getSubtotal(),
                "tipo", d.getTipo())).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", nota.getId());
        resp.put("fecha", nota.getFecha().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        resp.put("cliente", nota.getCliente() != null
                ? nota.getCliente().getNombres() + " " + nota.getCliente().getApellidos()
                : null);
        resp.put("barbero", nota.getBarbero() != null ? nota.getBarbero().getNombre() : null);
        resp.put("total", nota.getTotal());
        resp.put("detalles", detalles);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/recepcion/asociar-cliente")
    @ResponseBody
    @Transactional
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

    // ── NOTAS DE VENTA CON PAGINACIÓN ─────────────────────────────────────────
    @GetMapping("/recepcion/notas-venta")
    public String notas(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());

        Page<NotaVenta> notasPage = recepcionService.listarNotasPaginadas(pageable);

        double totalGeneral = notasPage.getContent().stream()
                .mapToDouble(NotaVenta::getTotal)
                .sum();

        double promedio = notasPage.getContent().isEmpty() ? 0 : totalGeneral / notasPage.getContent().size();

        model.addAttribute("notasPage", notasPage);
        model.addAttribute("notas", notasPage.getContent());
        model.addAttribute("totalGeneral", totalGeneral);
        model.addAttribute("promedio", promedio);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notasPage.getTotalPages());
        model.addAttribute("totalItems", notasPage.getTotalElements());
        model.addAttribute("size", size);

        return "secretario/notas-venta";
    }

    @GetMapping("/recepcion/ultima-nota")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ultimaNota(@RequestParam Long barberoId) {
        NotaVenta nota = recepcionService.listarNotas().stream()
                .filter(n -> n.getBarbero() != null &&
                        n.getBarbero().getId().equals(barberoId))
                .findFirst()
                .orElse(null);

        if (nota == null)
            return ResponseEntity.notFound().build();

        List<Map<String, Object>> detalles = nota.getDetalles().stream().map(d -> Map.<String, Object>of(
                "descripcion", d.getDescripcion(),
                "cantidad", d.getCantidad(),
                "subtotal", d.getSubtotal(),
                "tipo", d.getTipo())).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", nota.getId());
        resp.put("fecha", nota.getFecha().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        resp.put("cliente", nota.getCliente() != null
                ? nota.getCliente().getNombres() + " " + nota.getCliente().getApellidos()
                : null);
        resp.put("barbero", nota.getBarbero() != null ? nota.getBarbero().getNombre() : null);
        resp.put("total", nota.getTotal());
        resp.put("metodoPago", nota.getMetodoPago());
        resp.put("montoYape", nota.getMontoYape());
        resp.put("montoEfectivo", nota.getMontoEfectivo());
        resp.put("codigoYape", nota.getCodigoYape());
        resp.put("detalles", detalles);

        return ResponseEntity.ok(resp);
    }
}