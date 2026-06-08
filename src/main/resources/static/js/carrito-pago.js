// Obtener el token CSRF desde el meta tag del HTML
const CSRF = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';

// Cargar carrito desde localStorage
function getCarrito() {
    try { 
        return JSON.parse(localStorage.getItem('carrito_barberia') || '[]'); 
    } catch { 
        return []; 
    }
}

async function cargarPaginaPago() {
    const carrito = getCarrito();

    if (carrito.length === 0) {
        window.location.href = '/catalogo';
        return;
    }

    // Enriquecer con datos actuales de BD para verificar stock y precio real
    try {
        const res = await fetch('/api/carrito/enriquecer', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': CSRF
            },
            body: JSON.stringify(carrito)
        });
        const data = await res.json();

        if (data.error) {
            const errorContainer = document.getElementById('errorContainer');
            const errorMsg = document.getElementById('errorMsg');
            if (errorContainer && errorMsg) {
                errorContainer.style.display = 'flex';
                errorMsg.textContent = data.error;
            }
            return;
        }

        // Renderizar items de forma dinámica en el contenedor alternativo si existe
        const listaEl = document.getElementById('listaItems');
        const totalEl = document.getElementById('totalPagar');
        
        if (listaEl && data.items) {
            listaEl.innerHTML = '';
            let total = 0;

            data.items.forEach(item => {
                total += item.subtotal;
                listaEl.innerHTML += `
                    <div class="item-pedido">
                        <img src="${item.imagen || 'https://thebarbercompany.pe/wp-content/uploads/2019/04/serv4.webp'}"
                             alt="${item.nombre}" onerror="this.src='https://thebarbercompany.pe/wp-content/uploads/2019/04/serv4.webp'">
                        <div>
                            <div class="item-pedido-nombre">${item.nombre}</div>
                            <div class="item-pedido-meta">
                                Cantidad: <strong>${item.cantidad}</strong> × S/ ${item.precio.toFixed(2)}
                            </div>
                        </div>
                        <div class="item-pedido-precio">S/ ${item.subtotal.toFixed(2)}</div>
                    </div>`;
            });

            if (totalEl) totalEl.textContent = 'S/ ' + total.toFixed(2);

            // Guardar carritoJson para el envío del formulario
            const carritoInput = document.getElementById('carritoJsonInput');
            if (carritoInput) {
                carritoInput.value = JSON.stringify(
                    data.items.map(i => ({ id: i.id, cantidad: i.cantidad }))
                );
            }
        }
    } catch (error) {
        console.error("Error al enriquecer el carrito:", error);
    }
}

function previewFile(e) {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
        alert('La imagen no debe superar 5MB'); 
        return;
    }
    const img = document.getElementById('previewImg');
    if (img) {
        img.src = URL.createObjectURL(file);
        img.style.display = 'block';
    }
    const btnConfirmar = document.getElementById('btnConfirmar');
    if (btnConfirmar) btnConfirmar.disabled = false;
}

function handleDrop(e) {
    e.preventDefault();
    e.currentTarget.classList.remove('dragover');
    const file = e.dataTransfer.files[0];
    if (!file || !file.type.startsWith('image/')) return;
    
    const dt = new DataTransfer();
    dt.items.add(file);
    
    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.files = dt.files;
        previewFile({ target: { files: [file] } });
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    cargarPaginaPago();

    // Configurar los listeners dinámicamente para evitar scripts inline en el HTML
    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.addEventListener('change', previewFile);
    }

    const uploadZone = document.querySelector('.upload-zone');
    if (uploadZone) {
        uploadZone.addEventListener('click', () => fileInput && fileInput.click());
        uploadZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadZone.classList.add('dragover');
        });
        uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('dragover'));
        uploadZone.addEventListener('drop', handleDrop);
    }
});