package com.example.BarberiaLaClasica.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.ConsumoSilla;
import com.example.BarberiaLaClasica.model.DetalleNotaVenta;
import com.example.BarberiaLaClasica.model.NotaVenta;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.model.SillaSession;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.ConsumoSillaRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.repository.SillaSessionRepository;

<<<<<<< HEAD

=======
>>>>>>> 0cd79e854664d219825bd64c7122626485b80e08
@Service
public class RecepcionService {

    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private SillaSessionRepository sessionRepository;
    @Autowired
    private ConsumoSillaRepository consumoRepository;
    @Autowired
    private NotaVentaRepository notaVentaRepository;
    @Autowired
    private PromocionHelper promocionHelper;

    // ── Cita de reserva confirmada para hoy de un barbero ─────────────────────
    public Optional<Cita> getCitaReservaHoy(Long barberoId) {
        return citaRepository.findProximaCitaPorBarberoFechaEstado(
                barberoId, LocalDate.now(), 2);
    }

    // ── Sesión activa de un barbero ───────────────────────────────────────────
    public Optional<SillaSession> getSessionActiva(Long barberoId) {
        return sessionRepository.findByBarberoIdAndEstado(barberoId, 1);
    }

    public NotaVenta obtenerNota(Long id) {
        return notaVentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota no encontrada"));
    }

    // ── Listar todas las notas (mantener compatibilidad) ─────────────────────
    public List<NotaVenta> listarNotas() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("fecha").descending());
        // ✅ Cambiado a .findAll(pageable)
        return notaVentaRepository.findAll(pageable).getContent();
    }

    // ── NUEVO: Listar notas con paginación ────────────────────────────────
    public Page<NotaVenta> listarNotasPaginadas(Pageable pageable) {
        // ✅ Cambiado a .findAll(pageable)
        return notaVentaRepository.findAll(pageable);
    }

    // ── Reservas Web para Hoy con Paginación (CORREGIDO) ─────────────────────
    public Page<Cita> listarReservasHoyPaginadas(Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        // Estado 2 = Confirmada (cambia el número si tu estado confirmado es diferente)
        return citaRepository.findByFechaAndEstadoOrderByHoraInicioAsc(hoy, 2, pageable);
    }

    // ── Abrir sesión desde RESERVA ────────────────────────────────────────────
    @Transactional
    public void atenderReserva(Long barberoId) {
        Cita cita = getCitaReservaHoy(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay reserva confirmada para hoy"));

        Barbero barbero = cita.getBarbero();
        barbero.setEstado(2);
        barberoRepository.save(barbero);

        SillaSession session = new SillaSession();
        session.setBarbero(barbero);
        session.setCliente(cita.getCliente());
        session.setServicio(cita.getServicio());
        session.setCita(cita);
        sessionRepository.save(session);

        cita.setEstado(4);
        citaRepository.save(cita);
    }

    // ── Abrir sesión WALK-IN ─────────────────────────────────────────────────
    @Transactional
    public void ocuparSillaWalkin(Long barberoId, Long clienteId, Long servicioId) {
        // Solo bloquear si la reserva es en los próximos 30 minutos
        getCitaReservaHoy(barberoId).ifPresent(cita -> {
            LocalTime ahora = LocalTime.now();
            LocalTime horaRes = cita.getHoraInicio();
            long minutos = java.time.Duration.between(ahora, horaRes).toMinutes();
            if (minutos >= 0 && minutos <= 30) {
                throw new RuntimeException(
                        "Hay una reserva en " + minutos + " min. " +
                                "Usa 'Atender Reserva' o espera a que pase.");
            }
        });

        Barbero barbero = barberoRepository.findById(barberoId)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        barbero.setEstado(2);
        barberoRepository.save(barbero);

        SillaSession session = new SillaSession();
        session.setBarbero(barbero);
        session.setServicio(servicio);
        if (clienteId != null)
            clienteRepository.findById(clienteId).ifPresent(session::setCliente);

        sessionRepository.save(session);
    }

    // ── Agregar producto ──────────────────────────────────────────────────────
    @Transactional
    public void agregarProducto(Long barberoId, Long productoId, int cantidad) {
        SillaSession session = getSessionActiva(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getStock() < cantidad)
            throw new RuntimeException("Stock insuficiente: solo hay " + producto.getStock());

        consumoRepository.findBySessionId(session.getId()).stream()
                .filter(c -> c.getProducto().getId().equals(productoId))
                .findFirst()
                .ifPresentOrElse(c -> {
                    c.setCantidad(c.getCantidad() + cantidad);
                    c.setSubtotal(c.getCantidad() * producto.getPrecioVenta());
                    consumoRepository.save(c);
                }, () -> {
                    ConsumoSilla nuevo = new ConsumoSilla();
                    nuevo.setSession(session);
                    nuevo.setProducto(producto);
                    nuevo.setCantidad(cantidad);
                    nuevo.setSubtotal(cantidad * producto.getPrecioVenta());
                    consumoRepository.save(nuevo);
                });
    }

    // ── Quitar consumo ────────────────────────────────────────────────────────
    @Transactional
    public void quitarConsumo(Long consumoId) {
        consumoRepository.deleteById(consumoId);
    }

    public List<ConsumoSilla> obtenerConsumos(Long barberoId) {
        return getSessionActiva(barberoId)
                .map(s -> consumoRepository.findBySessionId(s.getId()))
                .orElse(List.of());
    }

    @Transactional
    public NotaVenta finalizarAtencion(Long barberoId, String metodoPago, double montoYape, String codigoYape) {
        SillaSession session = getSessionActiva(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        session = sessionRepository.findByIdConRelaciones(session.getId())
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        List<ConsumoSilla> consumos = consumoRepository.findBySessionId(session.getId());

        NotaVenta nota = new NotaVenta();
        nota.setSession(session);
        nota.setBarbero(session.getBarbero());
        nota.setCliente(session.getCliente());

        nota.setMetodoPago(metodoPago);
        nota.setMontoYape(montoYape);

        List<DetalleNotaVenta> detalles = new ArrayList<>();

        // ── 1. CONDICIONAL DE PROMOCIÓN PARA EL SERVICIO ──────────────────
        double precioServicioOriginal = session.getServicio().getPrecio().doubleValue();
        double precioServicioFinal = precioServicioOriginal; // Tarifa normal por defecto (S/ 35.00)

        // Verificamos si la sesión cuenta con una reserva web legítima y activa
        if (session.getCita() != null && session.getCita().getId() != null) {
            Cita cita = session.getCita();
            // Si tiene un anticipo web mayor a cero, confirmamos que es una cita reservada
            // por la web
            if (cita.getMontoYape() != null && cita.getMontoYape().compareTo(java.math.BigDecimal.ZERO) > 0) {
                precioServicioFinal = promocionHelper.calcularPrecioServicio(session.getServicio()); // Aplica S/ 17.50
            }
        }

        DetalleNotaVenta linServicio = new DetalleNotaVenta();
        linServicio.setNotaVenta(nota);
        linServicio.setDescripcion(session.getServicio().getNombre());
        linServicio.setCantidad(1);
        linServicio.setPrecioUnitario(precioServicioFinal);
        linServicio.setSubtotal(precioServicioFinal);
        linServicio.setTipo("SERVICIO");
        detalles.add(linServicio);

        // ── 2. CONDICIONAL DE PROMOCIÓN PARA PRODUCTOS ────────────────────
        for (ConsumoSilla c : consumos) {
            double precioProductoOriginal = c.getProducto().getPrecioVenta();
            double precioProductoFinal = precioProductoOriginal; // Tarifa normal por defecto

            // Si es cliente de reserva web, sus productos también pueden evaluar promoción
            if (session.getCita() != null && session.getCita().getId() != null) {
                Cita cita = session.getCita();
                if (cita.getMontoYape() != null && cita.getMontoYape().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    precioProductoFinal = promocionHelper.calcularPrecioProducto(c.getProducto());
                }
            }

            double subtotalProductoFinal = precioProductoFinal * c.getCantidad();

            DetalleNotaVenta lin = new DetalleNotaVenta();
            lin.setNotaVenta(nota);
            lin.setDescripcion(c.getProducto().getNombre());
            lin.setCantidad(c.getCantidad());
            lin.setPrecioUnitario(precioProductoFinal);
            lin.setSubtotal(subtotalProductoFinal);
            lin.setTipo("PRODUCTO");
            detalles.add(lin);

            // Descuento de Stock físico
            Producto p = c.getProducto();
            p.setStock(p.getStock() - c.getCantidad());
            productoRepository.save(p);
        }

        // ── 3. Cálculo Final de Totales ───────────────────────────────────
        double total = detalles.stream().mapToDouble(DetalleNotaVenta::getSubtotal).sum();
        nota.setTotal(total);

        if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
            nota.setMontoEfectivo(total);
            nota.setMontoYape(0.0);
        } else if ("YAPE".equalsIgnoreCase(metodoPago)) {
            nota.setMontoEfectivo(0.0);
            nota.setMontoYape(total);
            nota.setCodigoYape(codigoYape);
        } else { // Caso "MIXTO"
            nota.setMontoYape(montoYape);
            nota.setMontoEfectivo(Math.max(0, total - montoYape));
            nota.setCodigoYape(codigoYape);
        }

        nota.setDetalles(detalles);
        notaVentaRepository.save(nota);

        // ── 4. Actualización de Cita y Sesión ──────────────────────────────
        if (session.getCita() != null) {
            session.getCita().setEstado(3); // Finalizada / Atendida
            citaRepository.save(session.getCita());
        }

        session.setEstado(0); // Cerrar silla
        sessionRepository.save(session);

        Barbero barbero = session.getBarbero();
        barbero.setEstado(1); // Barbero Disponible
        barberoRepository.save(barbero);

        return nota;
    }

    @Transactional
    public void asociarCliente(Long barberoId, Long clienteId) {
        SillaSession session = sessionRepository
                .findByBarberoIdAndEstado(barberoId, 1)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        sessionRepository.actualizarCliente(session.getId(), clienteId);
    }
}