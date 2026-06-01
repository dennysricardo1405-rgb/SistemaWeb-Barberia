package com.example.BarberiaLaClasica.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
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

    // ── Cita de reserva confirmada para hoy de un barbero ─────────────────────
    public Optional<Cita> getCitaReservaHoy(Long barberoId) {
        return citaRepository.findByBarberoIdAndFechaAndEstado(
                barberoId, LocalDate.now(), 2); // estado 2 = confirmada
    }

    // ── Sesión activa de un barbero ───────────────────────────────────────────
    public Optional<SillaSession> getSessionActiva(Long barberoId) {
        return sessionRepository.findByBarberoIdAndEstado(barberoId, 1);
    }

    public NotaVenta obtenerNota(Long id) {
    return notaVentaRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Nota no encontrada"));
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
        session.setCita(cita); // vincula la reserva
        sessionRepository.save(session);

        // Marca la cita como en atención (estado 4)
        cita.setEstado(4);
        citaRepository.save(cita);
    }

    // ── Abrir sesión WALK-IN (sin reserva) ────────────────────────────────────
    @Transactional
    public void ocuparSillaWalkin(Long barberoId, Long clienteId, Long servicioId) {
        // Bloquear si hay reserva pendiente para hoy
        if (getCitaReservaHoy(barberoId).isPresent())
            throw new RuntimeException(
                    "Este barbero tiene una reserva confirmada para hoy. " +
                            "Usa 'Atender Reserva' o cancela la cita primero.");

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

        // Si ya existe, suma cantidad
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

    // ── Consumos actuales ─────────────────────────────────────────────────────
    public List<ConsumoSilla> obtenerConsumos(Long barberoId) {
        return getSessionActiva(barberoId)
                .map(s -> consumoRepository.findBySessionId(s.getId()))
                .orElse(List.of());
    }

    // ── Finalizar: genera nota y libera barbero ───────────────────────────────
    @Transactional
    public NotaVenta finalizarAtencion(Long barberoId) {
        SillaSession session = getSessionActiva(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        // Recarga la sesión fresca desde BD para asegurar que cliente esté cargado
        session = sessionRepository.findByIdConRelaciones(session.getId())
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        List<ConsumoSilla> consumos = consumoRepository.findBySessionId(session.getId());

        NotaVenta nota = new NotaVenta();
        nota.setSession(session);
        nota.setBarbero(session.getBarbero()); // ← agrega esto
        nota.setCliente(session.getCliente());
        List<DetalleNotaVenta> detalles = new ArrayList<>();

        // Línea del servicio
        DetalleNotaVenta linServicio = new DetalleNotaVenta();
        linServicio.setNotaVenta(nota);
        linServicio.setDescripcion(session.getServicio().getNombre());
        linServicio.setCantidad(1);
        linServicio.setPrecioUnitario(session.getServicio().getPrecio().doubleValue());
        linServicio.setSubtotal(session.getServicio().getPrecio().doubleValue());
        linServicio.setTipo("SERVICIO");
        detalles.add(linServicio);

        // Líneas de productos
        for (ConsumoSilla c : consumos) {
            DetalleNotaVenta lin = new DetalleNotaVenta();
            lin.setNotaVenta(nota);
            lin.setDescripcion(c.getProducto().getNombre());
            lin.setCantidad(c.getCantidad());
            lin.setPrecioUnitario(c.getProducto().getPrecioVenta()); // este ya es double, no necesita cambio
            lin.setSubtotal(c.getSubtotal()); // este también
            lin.setTipo("PRODUCTO");
            detalles.add(lin);

            // Descontar stock
            Producto p = c.getProducto();
            p.setStock(p.getStock() - c.getCantidad());
            productoRepository.save(p);
        }

        double total = detalles.stream().mapToDouble(DetalleNotaVenta::getSubtotal).sum();
        nota.setTotal(total);
        nota.setDetalles(detalles);
        notaVentaRepository.save(nota);

        // Si venía de reserva, marcarla como completada (estado 3)
        if (session.getCita() != null) {
            session.getCita().setEstado(3);
            citaRepository.save(session.getCita());
        }

        // Cerrar sesión y liberar barbero
        session.setEstado(0);
        sessionRepository.save(session);

        Barbero barbero = session.getBarbero();
        barbero.setEstado(1);
        barberoRepository.save(barbero);

        return nota;
    }

    public List<NotaVenta> listarNotas() {
        return notaVentaRepository.findAllByOrderByFechaDesc();
    }


    @Transactional
    public void asociarCliente(Long barberoId, Long clienteId) {
    SillaSession session = sessionRepository
        .findByBarberoIdAndEstado(barberoId, 1)
        .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

    clienteRepository.findById(clienteId)
        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

    sessionRepository.actualizarCliente(session.getId(), clienteId);
    
    System.out.println(">>> UPDATE ejecutado: sesión " + session.getId() + " → cliente " + clienteId);
}
}
