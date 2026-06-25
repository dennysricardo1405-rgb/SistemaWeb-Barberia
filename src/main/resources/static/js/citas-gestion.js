let modalPagoInstance;

// ── Ver comprobante en modal secundario ───────────────────────────────────
function verComprobante(src) {
    const imgGrande = document.getElementById('comprobanteGrande');
    if (imgGrande) {
        imgGrande.src = src;
        new bootstrap.Modal(document.getElementById('modalComprobante')).show();
    }
}

// ── Obtener Token CSRF de Spring Security ──────────────────────────────────
function getCsrf() {
    return document.querySelector('meta[name="_csrf"]')?.content || '';
}
function abrirModalConfirmacion(button) {
    const id = button.getAttribute('data-id');
    const totalServicio = parseFloat(button.getAttribute('data-total')) || 0;
    
    const fila = document.getElementById('fila-' + id);
    const imgElement = fila ? fila.querySelector('.comprobante-thumb') : null;

    document.getElementById('modalCitaId').value = id;
    
    if (document.getElementById('txtTotalServicio')) {
        document.getElementById('txtTotalServicio').innerText = totalServicio.toFixed(2);
    }
    
    // Cambiado dinámicamente a Código Yape (OCR)
    const txtCodigo = document.getElementById('txtCodigoDetectado');
    if (txtCodigo) txtCodigo.innerText = "Escaneando...";

    // Inicializamos los inputs vacíos/cero para la digitación del Secretario
    document.getElementById('formMontoYape').value = 0;
    document.getElementById('formMontoEfectivo').value = 0;
    
    calcularCuadre();

    modalPagoInstance = new bootstrap.Modal(document.getElementById('modalConfirmarPago'));
    modalPagoInstance.show();

    if (imgElement && imgElement.src) {
        ejecutarOcrInteligente(imgElement.src);
    } else {
        if (txtCodigo) txtCodigo.innerText = "NO_DETECTADO";
    }
}

function ejecutarOcrInteligente(imgUrl) {
    if (typeof Tesseract === 'undefined') return;

    Tesseract.recognize(imgUrl, 'eng')
    .then(({ data: { text } }) => {
        // Extrae el código de 8 dígitos continuo de Yape
        const matchCodigo = text.match(/\b\d{8}\b/);
        let codigoDetectado = matchCodigo ? matchCodigo[0] : "NO_DETECTADO";

        if (codigoDetectado === "NO_DETECTADO") {
            const matchFlex = text.match(/(?:operación|nro\.?|nro)\s*:?\s*(\d+)/i);
            if (matchFlex && matchFlex[1]) codigoDetectado = matchFlex[1];
        }

        const txtCodigo = document.getElementById('txtCodigoDetectado');
        if (txtCodigo) txtCodigo.innerText = codigoDetectado;
    })
    .catch(() => {
        const txtCodigo = document.getElementById('txtCodigoDetectado');
        if (txtCodigo) txtCodigo.innerText = "NO_DETECTADO";
    });
}

// ── Calculadora de Cuadre y Alertas de Seguridad ───────────────────────────
function calcularCuadre() {
    const txtTotal = document.getElementById('txtTotalServicio');
    if (!txtTotal) return; // Failsafe por si el elemento no ha cargado en el DOM

    const total = parseFloat(txtTotal.innerText) || 0;
    let yape = parseFloat(document.getElementById('formMontoYape').value) || 0;
    let efectivo = parseFloat(document.getElementById('formMontoEfectivo').value) || 0;

    // Validación estricta: Bloquear números negativos manuales en caliente
    if (yape < 0) { yape = 0; document.getElementById('formMontoYape').value = 0; }
    if (efectivo < 0) { efectivo = 0; document.getElementById('formMontoEfectivo').value = 0; }

    const saldo = total - (yape + efectivo);
    const box = document.getElementById('boxStatusSaldo');

    if (box) {
        if (saldo === 0) {
            box.style.background = "rgba(46,204,113,0.15)";
            box.style.color = "#2ecc71";
            box.innerHTML = '<h6 class="m-0 font-monospace">✨ ¡SERVICIO CANCELADO COMPLETAMENTE! S/ 0.00</h6>';
        } else if (saldo < 0) {
            box.style.background = "rgba(231,76,60,0.15)";
            box.style.color = "#e74c3c";
            box.innerHTML = '<h6>⚠️ ERROR: El monto supera el costo total</h6>';
        } else {
            box.style.background = "rgba(201,168,76,0.15)";
            box.style.color = "#c9a84c";
            box.innerHTML = `<h6 class="m-0 font-monospace">Saldo Restante en Caja/Silla: S/ ${saldo.toFixed(2)}</h6>`;
        }
    }
}

// ── Envío AJAX con Popups de Confirmación Contextuales ─────────────────────
async function enviarConfirmacionHibrida() {
    const id = document.getElementById('modalCitaId').value;
    const total = parseFloat(document.getElementById('txtTotalServicio').innerText) || 0;
    const yape = parseFloat(document.getElementById('formMontoYape').value) || 0;
    const efectivo = parseFloat(document.getElementById('formMontoEfectivo').value) || 0;
    const codigo = document.getElementById('txtCodigoDetectado').innerText;

    if (yape < 0 || efectivo < 0) {
        alert("Los montos no pueden ser negativos.");
        return;
    }
    if ((yape + efectivo) > total) {
        alert("Error: El monto ingresado no puede superar el costo del servicio.");
        return;
    }

    const saldo = total - (yape + efectivo);
    const msg = saldo === 0 
        ? `¿Está seguro de procesar? El servicio quedará marcado como TOTALMENTE CANCELADO.` 
        : `¿Está seguro de procesar? Quedará un saldo pendiente por cobrar en el local de S/ ${saldo.toFixed(2)}.`;
    
    if (!confirm(msg)) return;

    try {
        const res = await fetch(`/secretario/citas/${id}/aceptar?montoYape=${yape}&montoEfectivo=${efectivo}&codigoYape=${codigo}`, {
            method: 'POST',
            headers: { 'X-CSRF-TOKEN': getCsrf() }
        });
        
        if (res.ok) {
            if (modalPagoInstance) modalPagoInstance.hide();
            const fila = document.getElementById('fila-' + id);
            if (fila) fila.remove();
            mostrarToast('ok', '✅ Registro y cuadre de caja guardado.');
        } else {
            mostrarToast('err', 'Error al procesar el abono en el servidor.');
        }
    } catch (e) {
        mostrarToast('err', 'Error de conexión con el servidor.');
    }
}

// ── Acción para Cancelar/Rechazar Reservas ────────────────────────────────
async function accionCita(id, accion) {
    if (accion === 'aceptar') return; // Se gestiona en el modal
    if (!confirm('¿Seguro que deseas cancelar esta cita?')) return;

    try {
        const res = await fetch(`/secretario/citas/${id}/${accion}`, {
            method: 'POST',
            headers: { 'X-CSRF-TOKEN': getCsrf() }
        });
        if (res.ok) { 
            const fila = document.getElementById('fila-' + id);
            if (fila) fila.remove(); 
            mostrarToast('ok', '❌ Cita cancelada.'); 
        }
    } catch (e) { 
        mostrarToast('err', 'Error de conexión.'); 
    }
}

// ── Mostrar Notificaciones Flotantes ──────────────────────────────────────
function mostrarToast(tipo, texto) {
    const box = document.getElementById('toastBox');
    if (box) {
        box.innerHTML = `<div class="toast-${tipo}"><span>${texto}</span></div>`;
        box.style.display = 'block';
        setTimeout(() => { box.style.display = 'none'; }, 4500);
    }
}