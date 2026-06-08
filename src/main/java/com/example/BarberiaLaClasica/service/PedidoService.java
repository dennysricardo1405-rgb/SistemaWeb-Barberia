package com.example.BarberiaLaClasica.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.DetallePedido;
import com.example.BarberiaLaClasica.model.PedidoOnline;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.PedidoOnlineRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
public class PedidoService {

    @Autowired
    private PedidoOnlineRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.upload.dir:uploads/comprobantes}")
    private String uploadDir;

    // ── Guardar pedido con comprobante ────────────────────
    @Transactional
    public PedidoOnline confirmarPedido(
            String correoCliente,
            List<Map<String, Object>> itemsCarrito,
            MultipartFile comprobante) throws IOException {

        Cliente cliente = clienteRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Guardar comprobante
        String nombreArchivo = UUID.randomUUID() + "_" + comprobante.getOriginalFilename();
        Path ruta = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(ruta);
        Files.copy(comprobante.getInputStream(), ruta.resolve(nombreArchivo),
                StandardCopyOption.REPLACE_EXISTING);

        PedidoOnline pedido = new PedidoOnline();
        pedido.setCliente(cliente);
        pedido.setComprobantePago(nombreArchivo);
        pedido.setEstado(1);

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0;

        for (Map<String, Object> item : itemsCarrito) {
            Long productoId = Long.parseLong(item.get("id").toString());
            int cantidad = Integer.parseInt(item.get("cantidad").toString());

            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Validar stock
            if (producto.getStock() < cantidad)
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            detalle.setSubtotal(cantidad * producto.getPrecioVenta());
            detalles.add(detalle);

            total += detalle.getSubtotal();

            // Descontar stock
            producto.setStock(producto.getStock() - cantidad);
            productoRepository.save(producto);
        }

        pedido.setTotal(total);
        pedido.setDetalles(detalles);
        pedidoRepository.save(pedido);

        return pedido;
    }

    // ── Historial del cliente ─────────────────────────────
    public List<PedidoOnline> historialCliente(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return pedidoRepository.findByClienteOrderByFechaPedidoDesc(cliente);
    }

    // ── Secretario: listar todos ──────────────────────────
    public List<PedidoOnline> listarTodos() {
        return pedidoRepository.findAllByOrderByFechaPedidoDesc();
    }

    // ── Secretario: aceptar ───────────────────────────────
    @Transactional
    public void aceptarPedido(Long id) {
        PedidoOnline pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        pedido.setEstado(2);
        pedidoRepository.save(pedido);
        enviarCorreo(pedido, true);
    }

    // ── Secretario: rechazar ──────────────────────────────
    @Transactional
    public void rechazarPedido(Long id) {
        PedidoOnline pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // Devolver stock
        for (DetallePedido d : pedido.getDetalles()) {
            Producto p = d.getProducto();
            p.setStock(p.getStock() + d.getCantidad());
            productoRepository.save(p);
        }

        pedido.setEstado(0);
        pedidoRepository.save(pedido);
        enviarCorreo(pedido, false);
    }

    // ── Correo ────────────────────────────────────────────
    private void enviarCorreo(PedidoOnline pedido, boolean aceptado) {
        try {
            if (pedido.getCliente() == null || pedido.getCliente().getCorreo() == null)
                return;

            String asunto = aceptado
                    ? "Tu pedido fue aceptado — Ya puedes venir a recogerlo"
                    : "Tu pedido fue rechazado — Barbería La Clásica";

            StringBuilder items = new StringBuilder();
            for (DetallePedido d : pedido.getDetalles()) {
                items.append(String.format(
                        "<tr><td style='padding:8px;color:#aaa'>%s</td>" +
                                "<td style='padding:8px;text-align:center'>%d</td>" +
                                "<td style='padding:8px;text-align:right;color:#c9a84c'>S/ %.2f</td></tr>",
                        d.getProducto().getNombre(), d.getCantidad(), d.getSubtotal()));
            }

            String cuerpo = """
                    <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;
                                background:#111;color:#f0ece0;border-radius:12px;overflow:hidden;">
                        <div style="background:%s;padding:24px;text-align:center;">
                            <h2 style="margin:0;color:%s;">Barbería La Clásica</h2>
                            <p style="margin:4px 0 0;color:%s;font-size:0.9rem;">%s</p>
                        </div>
                        <div style="padding:28px;">
                            <p>Hola <strong>%s</strong>, tu pedido #%d ha sido
                               <strong style="color:%s;">%s</strong></p>
                            <table style="width:100%%;border-collapse:collapse;margin-top:16px;">
                                <thead>
                                    <tr style="background:#1a1a1a;">
                                        <th style="padding:8px;text-align:left;color:#aaa;">Producto</th>
                                        <th style="padding:8px;text-align:center;color:#aaa;">Cant.</th>
                                        <th style="padding:8px;text-align:right;color:#aaa;">Subtotal</th>
                                    </tr>
                                </thead>
                                <tbody>%s</tbody>
                            </table>
                            <div style="margin-top:16px;text-align:right;font-size:1.1rem;font-weight:bold;">
                                Total: <span style="color:#c9a84c;">S/ %.2f</span>
                            </div>
                            %s
                        </div>
                    </div>
                    """.formatted(
                    aceptado ? "#c9a84c" : "#e74c3c",
                    aceptado ? "#0a0a0a" : "#fff",
                    aceptado ? "#0a0a0a" : "#fff",
                    aceptado ? "Pedido Aceptado ✅" : "Pedido Rechazado ❌",
                    pedido.getCliente().getNombres(), pedido.getId(),
                    aceptado ? "#c9a84c" : "#e74c3c",
                    aceptado ? "ACEPTADO 🎉" : "RECHAZADO",
                    items.toString(),
                    pedido.getTotal(),
                    aceptado
                            ? "<p style='margin-top:20px;color:#aaa;font-size:0.85rem;'>" +
                                    "Ya puedes venir a recoger tu pedido a la barbería. " +
                                    "¡Te esperamos!</p>"
                            : "<p style='margin-top:20px;color:#aaa;font-size:0.85rem;'>" +
                                    "Lo sentimos, tu pedido no pudo ser procesado. " +
                                    "Contáctanos para más información.</p>");

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(pedido.getCliente().getCorreo());
            helper.setSubject(asunto);
            helper.setText(cuerpo, true);
            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando correo pedido: " + e.getMessage());
        }
    }

    public List<PedidoOnline> listarPorEstado(int estado) {
        return pedidoRepository.findByEstadoOrderByFechaPedidoDesc(estado);
    }

    @Transactional
    public void marcarEntregado(Long id) {
        PedidoOnline pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        if (pedido.getEstado() != 2)
            throw new RuntimeException("Este pedido no está en estado aceptado");
        pedido.setEstado(3); // 3 = ENTREGADO
        pedidoRepository.save(pedido);
    }
}
