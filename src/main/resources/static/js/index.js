// ── Carrito ───────────────────────────────────────────────
const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content || '';
const ES_CLIENTE = /*[[${#authorization.expression('hasRole(''CLIENTE'')')}]]*/ false;

(function () {
    var slides = document.querySelectorAll('.slide-bg-item');
    var dots = document.querySelectorAll('.slider-dot');
    var titulo = document.getElementById('heroTitulo');
    var desc = document.getElementById('heroDesc');
    var DEFAULT_TITULO = 'El Arte del Buen Corte';
    var DEFAULT_DESC = 'Reserva tu cita con los mejores barberos de la ciudad. Experiencia, precisión y estilo en cada visita.';
    if (!slides.length) return;
    var current = 0, timer = null, total = slides.length;
    function activar(idx) {
        slides[current].classList.remove('active');
        if (dots[current]) dots[current].classList.remove('active');
        current = ((idx % total) + total) % total;
        slides[current].classList.add('active');
        if (dots[current]) dots[current].classList.add('active');
        var t = slides[current].dataset.titulo;
        var d = slides[current].dataset.desc;
        if (titulo) titulo.textContent = (t && t.trim() !== '') ? t : DEFAULT_TITULO;
        if (desc) desc.textContent = (d && d.trim() !== '') ? d : DEFAULT_DESC;
    }
    function resetTimer() {
        clearInterval(timer);
        if (total > 1) timer = setInterval(function () { activar(current + 1); }, 5000);
    }
    window.sliderGoTo = function (idx) { activar(idx); resetTimer(); };
    window.sliderNext = function () { activar(current + 1); resetTimer(); };
    window.sliderPrev = function () { activar(current - 1); resetTimer(); };
    resetTimer();
    var heroEl = document.querySelector('.hero-slider');
    if (heroEl) {
        heroEl.addEventListener('mouseenter', function () { clearInterval(timer); });
        heroEl.addEventListener('mouseleave', resetTimer);
    }
})();

// Navbar scroll effect
window.addEventListener('scroll', function () {
    var nb = document.querySelector('.nb');
    if (window.scrollY > 50) {
        nb.style.background = 'rgba(8,8,8,0.97)';
        nb.style.boxShadow = '0 4px 30px rgba(0,0,0,0.4)';
    } else {
        nb.style.background = 'rgba(10,10,10,0.82)';
        nb.style.boxShadow = 'none';
    }
});


function mostrarModalAuth() {
    new bootstrap.Modal(document.getElementById('modalAuthCarrito')).show();
}

function irAlCarrito() {
    // Aquí sí validamos si está logueado
    fetch('/api/carrito/count')
        .then(r => r.json())
        .then(data => {
            if (data.totalItems === 0) {
                window.location.href = '/catalogo';
                return;
            }
            window.location.href = '/cliente/carrito/pago';
        })
        .catch(() => window.location.href = '/cliente/carrito/pago');
}

async function agregarAlCarrito(productoId, stock) {
    // Buscar datos del producto desde la página
    const btn    = event.currentTarget;
    const card   = btn.closest('.card-producto-premium');
    const nombre = card.querySelector('.prod-titulo')?.textContent?.trim() || '';
    const precio = parseFloat(
        card.querySelector('.prod-precio')?.textContent?.replace('S/ ','') || 0);
    const imagen = card.querySelector('img')?.src || '';

    const carrito = getCarrito();
    const existe  = carrito.findIndex(i => i.id == productoId);

    if (existe >= 0) {
        const nueva = carrito[existe].cantidad + 1;
        if (nueva > stock) {
            mostrarToast('⚠ Solo hay ' + stock + ' unidades disponibles', '#e74c3c');
            return;
        }
        carrito[existe].cantidad = nueva;
    } else {
        carrito.push({ id: productoId, nombre, precio, imagen, cantidad: 1, stock });
    }

    saveCarrito(carrito);
    renderCarrito();
    abrirCarrito();
    mostrarToast('✓ ' + nombre + ' agregado', '#c9a84c');
}

function actualizarContadorCarrito(total) {
    const badge = document.getElementById('carritoCount');
    if (!badge) return;
    if (total > 0) {
        badge.textContent = total;
        badge.style.display = 'flex';
    } else {
        badge.style.display = 'none';
    }
}


// Cargar contador al iniciar
document.addEventListener('DOMContentLoaded', async () => {
    if (!ES_CLIENTE) return;
    try {
        const res = await fetch('/api/carrito/count');
        const data = await res.json();
        actualizarContadorCarrito(data.totalItems);
    } catch { }
});


function getCarrito() {
    try { return JSON.parse(localStorage.getItem('carrito_barberia') || '[]'); }
    catch { return []; }
}
function saveCarrito(carrito) {
    localStorage.setItem('carrito_barberia', JSON.stringify(carrito));
    sincronizarConServidor(carrito);
}

// Sincroniza con la sesión del servidor
async function sincronizarConServidor(carrito) {
    try {
        await fetch('/api/carrito/sincronizar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': CSRF_TOKEN
            },
            body: JSON.stringify(carrito)
        });
    } catch {}
}

function abrirCarrito()  {
    document.getElementById('carritoDrawer').classList.add('open');
    document.getElementById('carritoOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function cerrarCarrito() {
    document.getElementById('carritoDrawer').classList.remove('open');
    document.getElementById('carritoOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

function renderCarrito() {
    const carrito = getCarrito();
    const body    = document.getElementById('carritoDrawerBody');
    const vacio   = document.getElementById('carritoVacio');
    const total   = carrito.reduce((s, i) => s + i.precio * i.cantidad, 0);
    const count   = carrito.reduce((s, i) => s + i.cantidad, 0);

    document.getElementById('drawerTotal').textContent =
        'S/ ' + total.toFixed(2);
    document.getElementById('drawerCount').textContent = count;
    actualizarContadorNavbar(count);

    if (carrito.length === 0) {
        body.innerHTML = '';
        body.appendChild(document.getElementById('carritoVacio') ||
            crearVacio());
        vacio && (vacio.style.display = 'flex');
        return;
    }

    body.innerHTML = carrito.map((item, idx) => `
        <div class="carrito-item">
            <img class="carrito-item-img"
                 src="${item.imagen || 'https://thebarbercompany.pe/wp-content/uploads/2019/04/serv4.webp'}"
                 alt="${item.nombre}"
                 onerror="this.src='https://thebarbercompany.pe/wp-content/uploads/2019/04/serv4.webp'">
            <div class="carrito-item-info">
                <div class="carrito-item-nombre" title="${item.nombre}">${item.nombre}</div>
                <div class="carrito-item-precio">S/ ${item.precio.toFixed(2)} c/u</div>
                <div class="carrito-item-controles">
                    <button class="ctrl-btn" onclick="cambiarCantidad(${idx}, -1)">
                        <i class="fa-solid fa-minus"></i>
                    </button>
                    <span class="ctrl-cantidad">${item.cantidad}</span>
                    <button class="ctrl-btn" onclick="cambiarCantidad(${idx}, 1)">
                        <i class="fa-solid fa-plus"></i>
                    </button>
                    <button class="ctrl-btn-del" onclick="eliminarItem(${idx})"
                            title="Eliminar">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="carrito-item-subtotal">
                S/ ${(item.precio * item.cantidad).toFixed(2)}
            </div>
        </div>
    `).join('');
}

function cambiarCantidad(idx, delta) {
    const carrito = getCarrito();
    const item    = carrito[idx];
    if (!item) return;

    const nueva = item.cantidad + delta;
    if (nueva <= 0) {
        eliminarItem(idx); return;
    }
    if (nueva > item.stock) {
        mostrarToast('⚠ Solo hay ' + item.stock + ' unidades disponibles', '#e74c3c');
        return;
    }
    carrito[idx].cantidad = nueva;
    saveCarrito(carrito);
    renderCarrito();
}

function eliminarItem(idx) {
    const carrito = getCarrito();
    carrito.splice(idx, 1);
    saveCarrito(carrito);
    renderCarrito();
}

function actualizarContadorNavbar(count) {
    const badge = document.getElementById('carritoCount');
    if (!badge) return;
    badge.textContent  = count;
    badge.style.display = count > 0 ? 'flex' : 'none';
}


function irAPagar() {
    // Verificar si está logueado intentando ir a la ruta protegida
    // Spring Security redirigirá al login si no está autenticado
    window.location.href = '/cliente/carrito/pago';
}

// Al cargar la página
document.addEventListener('DOMContentLoaded', () => {
    renderCarrito();

    // Botón carrito del navbar
    document.querySelectorAll('[onclick="irAlCarrito()"]').forEach(btn => {
        btn.setAttribute('onclick', 'abrirCarrito()');
    });
});

// ── Toast ─────────────────────────────────────────────────
function mostrarToast(mensaje, color = '#c9a84c') {
    const toast = document.getElementById('toastCarrito');
    const span  = document.getElementById('toastMensaje');
    if (!toast || !span) return;
    span.textContent    = mensaje;
    toast.style.borderColor = color;
    toast.style.transform   = 'translateX(-50%) translateY(0)';
    setTimeout(() => {
        toast.style.transform = 'translateX(-50%) translateY(100px)';
    }, 3000);
}

// ── Filtro subcategoría ───────────────────────────────────
function filtrarSubcategoria(nombre, event) {
    document.querySelectorAll('.item-producto-tarjeta').forEach(item => {
        item.style.display =
            (nombre === 'TODOS' || item.dataset.subcategoria === nombre) ? '' : 'none';
    });
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    if (event?.target) event.target.classList.add('active');
}