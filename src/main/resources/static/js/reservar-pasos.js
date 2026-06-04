// ── Estado global de la reserva ──────────────────────────────────────────────
const reserva = {
    servicioId: null, servicioNombre: '', servicioPrecio: '',
    barberoId: null, barberoNombre: '',
    fecha: '', hora: ''
};

// ── Navegación entre pasos ────────────────────────────────────────────────────
function irPaso(n) {
    document.querySelectorAll('.step-panel').forEach((p, i) => {
        p.classList.toggle('visible', i + 1 === n);
    });
    actualizarProgress(n);
    if (n === 4) rellenarResumen();
}

function actualizarProgress(paso) {
    for (let i = 1; i <= 4; i++) {
        const dot = document.getElementById('dot' + i);
        if (i < paso) {
            dot.classList.add('done');
            dot.classList.remove('active');
            dot.innerHTML = '<i class="fa-solid fa-check" style="font-size:0.75rem"></i>';
        } else if (i === paso) {
            dot.classList.add('active');
            dot.classList.remove('done');
            dot.textContent = i;
        } else {
            dot.classList.remove('active', 'done');
            dot.textContent = i;
        }
    }
    for (let i = 1; i <= 3; i++) {
        document.getElementById('line' + i).classList.toggle('done', i < paso);
    }
}

// ── Paso 1: Seleccionar servicio ──────────────────────────────────────────────
function seleccionarServicio(el) {
    document.querySelectorAll('.option-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    reserva.servicioId = el.dataset.id;
    reserva.servicioNombre = el.dataset.nombre;
    reserva.servicioPrecio = el.dataset.precio;
    document.getElementById('btnSiguiente1').disabled = false;
}

// ── Paso 2: Seleccionar barbero ───────────────────────────────────────────────
function seleccionarBarbero(el) {
    document.querySelectorAll('.barbero-card').forEach(c => {
        c.classList.remove('selected');
        const icon = c.querySelector('.check-icon');
        if (icon) icon.style.display = 'none';
    });
    el.classList.add('selected');
    const icon = el.querySelector('.check-icon');
    if (icon) icon.style.display = 'block';
    reserva.barberoId = el.dataset.id;
    reserva.barberoNombre = el.dataset.nombre;
    document.getElementById('btnSiguiente2').disabled = false;
}

// ── Paso 3: Fecha mínima y carga de horas ────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    const hoy = new Date();
    const hoyStr = hoy.toISOString().split('T')[0];

    // Fecha mínima: hoy
    document.getElementById('fechaInput').min = hoyStr;

    // Fecha máxima: último día del mes actual + 2 meses
    const mesesPermitidos = 2; // ← cambia este número según necesites
    const fechaMax = new Date(hoy.getFullYear(), hoy.getMonth() + 1 + mesesPermitidos, 0);
    const fechaMaxStr = fechaMax.toISOString().split('T')[0];
    document.getElementById('fechaInput').max = fechaMaxStr;
});

async function cargarHoras() {
    const fecha = document.getElementById('fechaInput').value;
    if (!fecha || !reserva.barberoId) return;

    reserva.fecha = fecha;
    reserva.hora = '';
    document.getElementById('btnSiguiente3').disabled = true;
    document.getElementById('loadingHoras').style.display = 'block';
    document.getElementById('horasContainer').style.display = 'none';
    document.getElementById('sinHoras').style.display = 'none';

    const res = await fetch(`/api/citas/horas-disponibles?barberoId=${reserva.barberoId}&fecha=${fecha}`);
    const data = await res.json();
    const slots = data.slots;

    document.getElementById('loadingHoras').style.display = 'none';

    const disponibles = slots.filter(s => s.disponible);
    if (disponibles.length === 0) {
        document.getElementById('sinHoras').style.display = 'block';
        return;
    }

    const grid = document.getElementById('horasGrid');
    grid.innerHTML = '';

    // Sin slots en absoluto (día fuera de rango, etc.)
    if (slots.length === 0) {
        document.getElementById('sinHoras').style.display = 'block';
        return;
    }

    slots.forEach(s => {
        const btn = document.createElement('button');
        btn.textContent = s.hora;

        // ── BLOQUEAR HORAS PASADAS si es hoy ─────────────────────────
        const esHoy = document.getElementById('fechaInput').value ===
            new Date().toISOString().split('T')[0];
        const [hh, mm] = s.hora.split(':').map(Number);
        const ahora = new Date();
        const yaFue = esHoy && (hh < ahora.getHours() ||
            (hh === ahora.getHours() && mm <= ahora.getMinutes()));

        if (yaFue) {
            btn.className = 'hora-btn ocupada confirmada';
            btn.disabled = true;
            btn.title = 'Hora no disponible';
        } else if (s.disponible) {
            btn.className = 'hora-btn';
            btn.onclick = () => {
                document.querySelectorAll('.hora-btn').forEach(b => b.classList.remove('selected'));
                btn.classList.add('selected');
                reserva.hora = s.hora;
                document.getElementById('btnSiguiente3').disabled = false;
            };
        } else if (s.estadoOcupacion === 'pendiente') {
            btn.className = 'hora-btn ocupada pendiente';
            btn.disabled = true;
            btn.title = 'Reserva en revisión';
        } else {
            btn.className = 'hora-btn ocupada confirmada';
            btn.disabled = true;
            btn.title = 'Horario no disponible';
        }

        grid.appendChild(btn);
    });

    // Aviso de "sin horas" solo si TODAS están ocupadas
    const hayLibres = slots.some(s => s.disponible);
    if (!hayLibres) {
        document.getElementById('sinHoras').style.display = 'block';
    }

    document.getElementById('horasContainer').style.display = 'block';
}

// ── Paso 4: Rellenar resumen ──────────────────────────────────────────────────
function rellenarResumen() {
    document.getElementById('resumenServicio').textContent = reserva.servicioNombre;
    document.getElementById('resumenBarbero').textContent = reserva.barberoNombre;
    document.getElementById('resumenFecha').textContent = formatearFecha(reserva.fecha);
    document.getElementById('resumenHora').textContent = reserva.hora;
    document.getElementById('resumenPrecio').textContent = 'S/. ' + reserva.servicioPrecio;
}

function formatearFecha(f) {
    if (!f) return '—';
    const [y, m, d] = f.split('-');
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return `${d} ${meses[parseInt(m) - 1]} ${y}`;
}

// ── Proceder al pago ──────────────────────────────────────────────────────────
async function procederAlPago() {
    const res = await fetch('/api/citas/pre-reserva', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()   // ← ya usa el meta tag
        },
        body: JSON.stringify({
            servicioId: reserva.servicioId,
            barberoId: reserva.barberoId,
            fecha: reserva.fecha,
            hora: reserva.hora
        })
    });
    const data = await res.json();
    if (data.requiereAutenticacion) {
        new bootstrap.Modal(document.getElementById('modalAuth')).show();
    } else {
        window.location.href = data.redireccion;
    }
}

// ── Modal Auth: cambiar tabs ──────────────────────────────────────────────────
function showTab(tab) {
    document.getElementById('tabLogin').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('tabRegistro').style.display = tab === 'registro' ? 'block' : 'none';
    document.querySelectorAll('.tab-btn').forEach((b, i) => {
        b.classList.toggle('active',
            (i === 0 && tab === 'login') || (i === 1 && tab === 'registro')
        );
    });
}

// ── Login desde modal ─────────────────────────────────────────────────────────
function hacerLogin() {
    const email = document.getElementById('loginEmail').value;
    const pass = document.getElementById('loginPassword').value;
    document.getElementById('loginError').style.display = 'none';

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/cliente/login';
    form.innerHTML = `
        <input name="username" value="${email}">
        <input name="password" value="${pass}">
        <input name="_csrf"    value="${getCsrfToken()}">
    `;
    document.body.appendChild(form);
    form.submit();
}

// ── Registro desde modal ──────────────────────────────────────────────────────
async function buscarDniModal() {
    const dni = document.getElementById('regDni').value;

    if (dni.length !== 8) {
        alert('El DNI debe tener 8 dígitos');
        return;
    }

    try {
        const response = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data = await response.json();

        console.log(data);

        if (data.success) {

            document.getElementById('regNombres').value =
                data.datos.nombres || '';

            document.getElementById('regApellidos').value =
                (data.datos.ape_paterno || '') + ' ' +
                (data.datos.ape_materno || '');

        } else {
            alert('No se encontró información para ese DNI');
        }

    } catch (error) {
        console.error(error);
        alert('Error al consultar el DNI');
    }
}
async function hacerRegistro() {
    const errEl = document.getElementById('registroError');
    errEl.style.display = 'none';

    const body = new URLSearchParams({
        dni: document.getElementById('regDni').value,
        nombres: document.getElementById('regNombres').value,
        apellidos: document.getElementById('regApellidos').value,
        telefono: document.getElementById('regTelefono').value,
        correo: document.getElementById('regCorreo').value,
        passwordPlana: document.getElementById('regPassword').value,
        _csrf: getCsrfToken()
    });

    const res = await fetch('/cliente/registro', { method: 'POST', body });
    if (res.redirected && res.url.includes('login')) {
        document.getElementById('loginEmail').value = document.getElementById('regCorreo').value;
        document.getElementById('loginPassword').value = document.getElementById('regPassword').value;
        showTab('login');
        hacerLogin();
    } else {
        errEl.textContent = 'Error al registrarse. Verifica los datos.';
        errEl.style.display = 'block';
    }
}

// ── Helper CSRF ───────────────────────────────────────────────────────────────
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content || '';
}