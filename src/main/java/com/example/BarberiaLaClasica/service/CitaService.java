package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.*;
import com.example.BarberiaLaClasica.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CitaService {

    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ServicioRepository servicioRepository;

    // --- Twilio config (se carga desde application.properties) ---
    @Value("${twilio.account.sid}")
    private String twilioSid;
    @Value("${twilio.auth.token}")
    private String twilioToken;
    @Value("${twilio.whatsapp.from}")
    private String twilioFrom; // "whatsapp:+14155238886"

    // Carpeta donde se guardan los comprobantes (configura en
    // application.properties)
    @Value("${app.upload.dir:uploads/comprobantes}")
    private String uploadDir;

    // ── Historial de citas de un cliente ──────────────────────────────────────
    public List<Cita> obtenerHistorialCliente(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + correo));
        return citaRepository.findByClienteOrderByFechaDescHoraInicioDesc(cliente);
    }

    // ── Horas disponibles para un barbero en una fecha ───────────────────────
    

    // ── Confirmar reserva: guarda la cita con comprobante de pago ─────────────
    public Cita confirmarReserva(
            String correoCliente,
            Long servicioId,
            Long barberoId,
            String fechaStr,
            String horaStr,
            MultipartFile comprobante) throws IOException {

        Cliente cliente = clienteRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        Barbero barbero = barberoRepository.findById(barberoId)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));

        LocalDate fecha = LocalDate.parse(fechaStr);
        LocalTime horaInicio = LocalTime.parse(horaStr);
        LocalTime horaFin = horaInicio.plusMinutes(servicio.getDuracionMinutos());

        // Verificar que no haya conflicto de horario
        if (citaRepository.existeConflictoHorario(barberoId, fecha, horaInicio, horaFin)) {
            throw new RuntimeException("El horario seleccionado ya no está disponible.");
        }

        // Guardar imagen del comprobante en disco
        String nombreArchivo = UUID.randomUUID() + "_" + comprobante.getOriginalFilename();
        Path ruta = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(ruta);
        Files.copy(comprobante.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);

        // Crear y persistir la cita
        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setBarbero(barbero);
        cita.setServicio(servicio);
        cita.setFecha(fecha);
        cita.setHoraInicio(horaInicio);
        cita.setHoraFin(horaFin);
        cita.setTotalPrecio(servicio.getPrecio());
        cita.setComprobantePago(nombreArchivo);
        cita.setEstado(1); // PENDIENTE

        return citaRepository.save(cita);
    }

    // ── Secretario: cambiar estado a ACEPTADO (2) y notificar por WhatsApp ────
    public void aceptarCita(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstado(2); // ACEPTADA / EN_SILLA según tu lógica
        citaRepository.save(cita);

        // Notificar al cliente por WhatsApp si tiene teléfono
        if (cita.getCliente() != null && cita.getCliente().getTelefono() != null) {
            enviarWhatsApp(cita);
        }
    }

    // ── Secretario: cancelar cita ─────────────────────────────────────────────
    public void cancelarCita(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        cita.setEstado(0); // CANCELADA
        citaRepository.save(cita);
    }

    // ── Enviar mensaje WhatsApp con Twilio ────────────────────────────────────
    private void enviarWhatsApp(Cita cita) {
        try {
            Twilio.init(twilioSid, twilioToken);

            DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

            String mensaje = String.format(
                    "✅ *Barbería La Clásica* - Tu cita ha sido CONFIRMADA 🎉\n\n" +
                            "📅 Fecha: *%s*\n" +
                            "⏰ Hora: *%s*\n" +
                            "💇 Servicio: *%s*\n" +
                            "✂️ Barbero: *%s*\n\n" +
                            "¡Te esperamos! Por favor llega 5 minutos antes. 🙏",
                    cita.getFecha().format(fmtFecha),
                    cita.getHoraInicio().format(fmtHora),
                    cita.getServicio().getNombre(),
                    cita.getBarbero().getNombre());

            // El número del cliente debe tener formato internacional: +51987654321
            String numeroCliente = "whatsapp:+51" + cita.getCliente().getTelefono();

            Message.creator(
                    new PhoneNumber(numeroCliente),
                    new PhoneNumber(twilioFrom),
                    mensaje).create();

        } catch (Exception e) {
            // No falla silenciosamente — loguea el error pero no revienta el flujo
            System.err.println("ERROR enviando WhatsApp: " + e.getMessage());
        }
    }

    // ── Para el secretario: listar citas pendientes ───────────────────────────
    public List<Cita> listarPendientes() {
        return citaRepository.findByEstadoOrderByFechaAscHoraInicioAsc(1);
    }

    public List<Cita> listarCitasDeHoy() {
        return citaRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now());
    }
}