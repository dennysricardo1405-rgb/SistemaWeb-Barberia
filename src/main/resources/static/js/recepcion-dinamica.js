// ============================================================
// recepcion-dinamica.js
// ============================================================

let modalOcupar    = null;
let modalGestionar = null;

const HEADERS_JSON = { 'Content-Type': 'application/json' };

document.addEventListener('DOMContentLoaded', () => {
    const elOcupar    = document.getElementById('modalOcuparSilla');
    const elGestionar = document.getElementById('modalGestionarSilla');
    if (elOcupar)    modalOcupar    = new bootstrap.Modal(elOcupar);
    if (elGestionar) modalGestionar = new bootstrap.Modal(elGestionar);
});

// ── ABRIR MODAL OCUPAR ────────────────────────────────────────
function abrirModalOcupar(barberoId, barberoNombre) {
    document.getElementById('modalOcuparBarberoId').value = barberoId;
    document.getElementById('modalOcuparBarberoNombre').textContent = barberoNombre;
    document.getElementById('selectServicio').value = '';
    modalOcupar.show();
}

// ── ABRIR MODAL GESTIONAR ─────────────────────────────────────
function abrirModalGestionar(id, nombre) {
    document.getElementById('modalGestionarBarberoNombre').innerText = nombre;
    document.getElementById('consumoBarberoId').value = id;
    limpiarClienteGestion();
    switchGestionTab('existente');
    cargarConsumosDeSilla(id);
    modalGestionar.show();
}

// ── CARGAR CONSUMOS ───────────────────────────────────────────
function cargarConsumosDeSilla(barberoId) {
    fetch(`/secretario/recepcion/api-consumos/${barberoId}`)
    .then(r => r.json())
    .then(data => {

        // ── Servicio en el label izquierdo ──
        const labelServicio = document.getElementById('labelServicioActual');
        if (labelServicio) {
            labelServicio.textContent = data.servicio
                ? data.servicio.nombre + '  ·  S/ ' + Number(data.servicio.precio).toFixed(2)
                : '—';
        }

        // ── Cliente ya asociado ──
        if (data.cliente) {
            document.getElementById('clienteGestionNombre').textContent =
                data.cliente.nombres + ' ' + data.cliente.apellidos + ' — ' + data.cliente.dni;
            document.getElementById('clienteGestionInfo').style.display = 'block';
        }

        // ── Tabla: primero servicio base, luego productos ──
        const tbody = document.getElementById('tablaDetalleConsumos');
        tbody.innerHTML = '';

        // ✅ Fila 1: Servicio base (sin botón eliminar)
        if (data.servicio) {
            const precio = Number(data.servicio.precio);
            tbody.innerHTML += `
                <tr style="border-left:3px solid #c9a84c;">
                    <td>
                        <i class="fa-solid fa-scissors me-1 text-warning small"></i>
                        <span class="fw-bold text-warning">${data.servicio.nombre}</span>
                        <span class="badge bg-warning text-dark ms-1" style="font-size:0.6rem;">SERVICIO</span>
                    </td>
                    <td class="text-center">1</td>
                    <td class="text-end">S/ ${precio.toFixed(2)}</td>
                    <td class="text-end text-success fw-bold">S/ ${precio.toFixed(2)}</td>
                    <td class="text-center">
                        <i class="fa-solid fa-lock text-muted small" title="No removible"></i>
                    </td>
                </tr>`;
        }

        // ✅ Filas siguientes: productos agregados
        if (!data.consumos || data.consumos.length === 0) {
            tbody.innerHTML += `
                <tr>
                    <td colspan="5" class="text-center text-muted py-2 small">
                        <i class="fa-solid fa-box-open me-1"></i>Sin productos agregados aún
                    </td>
                </tr>`;
        } else {
            data.consumos.forEach(c => {
                tbody.innerHTML += `
                <tr>
                    <td>
                        <i class="fa-solid fa-box me-1 text-muted small"></i>
                        ${c.descripcion}
                    </td>
                    <td class="text-center">${c.cantidad}</td>
                    <td class="text-end">S/ ${Number(c.precioUnit).toFixed(2)}</td>
                    <td class="text-end text-success fw-bold">S/ ${Number(c.subtotal).toFixed(2)}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-danger py-0 px-2"
                            onclick="quitarConsumoDeSilla(${c.id}, ${barberoId})">
                            <i class="fa-solid fa-xmark"></i>
                        </button>
                    </td>
                </tr>`;
            });
        }

        // ── Total = servicio + productos ──
        const totalConsumos  = data.total || 0;
        const precioServicio = data.servicio ? Number(data.servicio.precio) : 0;
        document.getElementById('textoTotalSilla').textContent =
            'S/ ' + (totalConsumos + precioServicio).toFixed(2);
    })
    .catch(err => console.error('Error cargando consumos:', err));
}

// ── AGREGAR PRODUCTO ──────────────────────────────────────────
function agregarProductoDirecto(productoId) {
    const barberoId = document.getElementById('consumoBarberoId').value;
    const cantInput = document.getElementById(`cant-${productoId}`);
    const cantidad  = parseInt(cantInput?.value) || 1;

    fetch(`/secretario/recepcion/api-consumos/agregar?barberoId=${barberoId}&productoId=${productoId}&cantidad=${cantidad}`, {
        method: 'POST'
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) { alert('Error: ' + data.error); return; }
        if (cantInput) cantInput.value = 1;
        cargarConsumosDeSilla(barberoId);
    })
    .catch(err => alert('Error al agregar producto: ' + err.message));
}

// ── QUITAR CONSUMO ────────────────────────────────────────────
function quitarConsumoDeSilla(consumoId, barberoId) {
    if (!confirm('¿Remover este producto de la cuenta?')) return;
    fetch(`/secretario/recepcion/api-consumos/eliminar/${consumoId}`, {
        method: 'DELETE',
        headers: HEADERS_JSON
    })
    .then(r => {
        if (!r.ok) throw new Error('No se pudo eliminar.');
        cargarConsumosDeSilla(barberoId);
    })
    .catch(err => alert(err.message));
}

// ── FILTRAR PRODUCTOS ─────────────────────────────────────────
function filtrarProductos(texto) {
    document.querySelectorAll('.fila-producto').forEach(fila => {
        fila.style.display =
            fila.dataset.nombre.toLowerCase().includes(texto.toLowerCase()) ? '' : 'none';
    });
}

// ── TABS GESTIONAR ────────────────────────────────────────────
function switchGestionTab(tab) {
    document.getElementById('gTabExistente').style.display = tab === 'existente' ? 'block' : 'none';
    document.getElementById('gTabNuevo').style.display     = tab === 'nuevo'     ? 'block' : 'none';
    document.getElementById('gTabLibre').style.display     = tab === 'libre'     ? 'block' : 'none';

    const btns = { existente: 'gBtnExistente', nuevo: 'gBtnNuevo', libre: 'gBtnLibre' };
    Object.entries(btns).forEach(([key, id]) => {
        const btn = document.getElementById(id);
        if (!btn) return;
        btn.className = key === tab
            ? 'btn btn-sm fw-bold px-3 rounded-pill btn-warning text-dark'
            : 'btn btn-sm fw-bold px-3 rounded-pill btn-outline-secondary text-white';
    });
}

// ── BUSCAR CLIENTE EN GESTIONAR ───────────────────────────────
let timeoutGestion = null;
function buscarClienteGestion(valor) {
    const resultado = document.getElementById('resultadoBusquedaGestion');
    resultado.style.display = 'none';
    resultado.innerHTML = '';
    if (valor.length < 3) return;

    clearTimeout(timeoutGestion);
    timeoutGestion = setTimeout(() => {
        fetch(`/api/clientes/buscar-dni?q=${encodeURIComponent(valor)}`)
        .then(r => r.json())
        .then(clientes => {
            resultado.style.display = 'block';
            if (!clientes || clientes.length === 0) {
                resultado.innerHTML = `<span class="text-muted small p-2 d-block">No encontrado.</span>`;
                return;
            }
            resultado.innerHTML = '';
            clientes.forEach(cli => {
                const div = document.createElement('div');
                div.className = 'p-2 rounded-2 mb-1 d-flex justify-content-between align-items-center';
                div.style.cssText = 'background:#252525; cursor:pointer;';
                div.innerHTML = `
                    <div>
                        <span class="fw-bold text-white small">${cli.nombres} ${cli.apellidos}</span>
                        <span class="text-muted font-monospace ms-2 small">${cli.dni}</span>
                    </div>
                    <button type="button" class="btn btn-sm btn-success py-0 px-2 rounded-pill small fw-bold">
                        Seleccionar
                    </button>`;
                div.querySelector('button').onclick = () =>
                    asociarClienteGestion(cli.id, `${cli.nombres} ${cli.apellidos}`, cli.dni);
                resultado.appendChild(div);
            });
        })
        .catch(() => {
            resultado.style.display = 'block';
            resultado.innerHTML = `<span class="text-danger small p-2 d-block">Error al buscar.</span>`;
        });
    }, 350);
}

// ── ASOCIAR CLIENTE A SESIÓN ──────────────────────────────────
async function asociarClienteGestion(clienteId, nombre, dni) {
    const barberoId = document.getElementById('consumoBarberoId').value;
    
    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content 
                   || document.querySelector('input[name="_csrf"]')?.value 
                   || '';

    try {
        const res = await fetch(
            `/secretario/recepcion/asociar-cliente?barberoId=${barberoId}&clienteId=${clienteId}`,
            { 
                method: 'POST',
                headers: { 'X-CSRF-TOKEN': csrfToken }  // ← agregar esto
            }
        );
        if (!res.ok) {
            const err = await res.json();
            alert('Error: ' + (err.error || 'No se pudo asociar.'));
            return;
        }
        document.getElementById('clienteGestionNombre').textContent = nombre + ' — ' + dni;
        document.getElementById('clienteGestionInfo').style.display = 'block';
        document.getElementById('resultadoBusquedaGestion').style.display = 'none';
        document.getElementById('buscarDniGestion').value = '';
    } catch {
        alert('Error de conexión al asociar cliente.');
    }
}

// ── LIMPIAR CLIENTE GESTIONAR ─────────────────────────────────
function limpiarClienteGestion() {
    const info = document.getElementById('clienteGestionInfo');
    const nombre = document.getElementById('clienteGestionNombre');
    const input = document.getElementById('buscarDniGestion');
    const resultado = document.getElementById('resultadoBusquedaGestion');
    if (info) info.style.display = 'none';
    if (nombre) nombre.textContent = '';
    if (input) input.value = '';
    if (resultado) resultado.style.display = 'none';
}

// ── BUSCAR DNI API PARA NUEVO CLIENTE EN GESTIONAR ───────────
async function buscarDniGestionNuevo() {
    const dni = document.getElementById('gNuevoDni').value.trim();
    if (dni.length !== 8) return;
    try {
        const res  = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data = await res.json();
        document.getElementById('gNuevoNombres').value   = data.nombres   || '';
        document.getElementById('gNuevoApellidos').value = data.apellidos || '';
    } catch { }
}

// ── REGISTRAR NUEVO CLIENTE Y ASOCIAR ────────────────────────
async function registrarYAsociarCliente() {
    const dni       = document.getElementById('gNuevoDni').value.trim();
    const nombres   = document.getElementById('gNuevoNombres').value.trim();
    const apellidos = document.getElementById('gNuevoApellidos').value.trim();
    const telefono  = document.getElementById('gNuevoTelefono').value.trim();
    const correo    = document.getElementById('gNuevoCorreo').value.trim();
    const msg       = document.getElementById('gNuevoClienteMsg');

    if (!dni || dni.length !== 8) { mostrarMsg(msg, 'error', 'DNI debe tener 8 dígitos.'); return; }
    if (!nombres || !apellidos)   { mostrarMsg(msg, 'error', 'Nombres y apellidos obligatorios.'); return; }
    if (!correo)                  { mostrarMsg(msg, 'error', 'Correo obligatorio para que el cliente pueda iniciar sesión.'); return; }

    try {
        const res  = await fetch('/api/clientes/guardar-rapido', {
            method: 'POST',
            headers: HEADERS_JSON,
            body: JSON.stringify({ dni, nombres, apellidos, telefono, correo })
        });
        const data = await res.json();
        if (!res.ok) { mostrarMsg(msg, 'error', data.error || 'Error al guardar.'); return; }

        mostrarMsg(msg, 'exito', `Cliente ${data.nombres} guardado.`);
        await asociarClienteGestion(data.id, `${data.nombres} ${data.apellidos}`, data.dni);

        ['gNuevoDni','gNuevoNombres','gNuevoApellidos','gNuevoTelefono','gNuevoCorreo']
            .forEach(id => document.getElementById(id).value = '');
        switchGestionTab('existente');
    } catch {
        mostrarMsg(msg, 'error', 'Error de conexión.');
    }
}

// ── HELPER MENSAJES ───────────────────────────────────────────
function mostrarMsg(el, tipo, texto) {
    el.style.display = 'block';
    el.className = 'mt-2 small fw-bold ' + (tipo === 'error' ? 'text-danger' : 'text-success');
    el.innerHTML = `<i class="fa-solid fa-${tipo === 'error' ? 'circle-xmark' : 'check'} me-1"></i>${texto}`;
}

async function finalizarAtencion() {
    const barberoId = document.getElementById('consumoBarberoId').value;
    if (!confirm('¿Confirmar pago y liberar la silla?')) return;

    const res = await fetch(`/secretario/recepcion/finalizar-pago/${barberoId}`);

    // La respuesta es un redirect, así que recargamos con mensaje
    if (res.redirected || res.ok) {
        // Mostrar modal de resumen antes de redirigir
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
        const resNota = await fetch(`/secretario/recepcion/ultima-nota?barberoId=${barberoId}`, {
            headers: { 'X-CSRF-TOKEN': csrfToken }
        });
        if (resNota.ok) {
            const nota = await resNota.json();
            mostrarResumenNota(nota);
        } else {
            window.location.href = '/secretario/recepcion';
        }
    }
}

function mostrarResumenNota(nota) {
    document.getElementById('resumenNotaId').textContent    = '#' + nota.id;
    document.getElementById('resumenNotaFecha').textContent = nota.fecha;
    document.getElementById('resumenNotaCliente').textContent = nota.cliente || 'Sin registro';
    document.getElementById('resumenNotaBarbero').textContent = nota.barbero || '—';
    document.getElementById('resumenNotaTotal').textContent  = 'S/ ' + Number(nota.total).toFixed(2);

    const tbody = document.getElementById('resumenNotaDetalles');
    tbody.innerHTML = '';
    nota.detalles.forEach(d => {
        tbody.innerHTML += `
            <tr>
                <td>${d.descripcion}</td>
                <td class="text-center">${d.cantidad}</td>
                <td class="text-end">S/ ${Number(d.subtotal).toFixed(2)}</td>
            </tr>`;
    });

    new bootstrap.Modal(document.getElementById('modalResumenNota')).show();
}

