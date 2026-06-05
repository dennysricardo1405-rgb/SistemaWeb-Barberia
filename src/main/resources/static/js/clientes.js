// ============================================================
// clientes.js
// Ruta: static/js/clientes.js
// ============================================================

document.addEventListener('DOMContentLoaded', function () {
    
    // 1. Escuchador para buscar DNI al hacer click en la lupa
    const btnBuscarDni = document.getElementById('btnBuscarDni');
    if (btnBuscarDni) {
        btnBuscarDni.addEventListener('click', buscarDni);
    }

    // 2. Escuchador para registrar nuevo cliente (Guardado rápido)
    const btnGuardarCliente = document.getElementById('btnGuardarCliente');
    if (btnGuardarCliente) {
        btnGuardarCliente.addEventListener('click', guardarCliente);
    }

    // 3. Controlar el cambio de tamaño de registros en la paginación conservando la búsqueda activa
    const selectSize = document.getElementById('select-size-pages');
    if (selectSize) {
        selectSize.addEventListener('change', function() {
            const currentSearch = document.getElementById('current-search').value || '';
            window.location.href = `/admin/cliente?page=0&size=${this.value}&search=${encodeURIComponent(currentSearch)}`;
        });
    }

    // 4. Regex para evitar que se escriban letras en campos numéricos (DNI / Celular)
    const dniInput = document.getElementById('dniInput');
    if (dniInput) {
        dniInput.addEventListener('input', function() { this.value = this.value.replace(/[^0-9]/g, ''); });
    }
    
    const tlfInput = document.getElementById('telefonoInput');
    if (tlfInput) {
        tlfInput.addEventListener('input', function() { this.value = this.value.replace(/[^0-9]/g, ''); });
    }

    const editTlf = document.getElementById('edit-telefono');
    if (editTlf) {
        editTlf.addEventListener('input', function() { this.value = this.value.replace(/[^0-9]/g, ''); });
    }

    // 5. Escuchador dinámico para botones "Editar" en la tabla
    document.querySelectorAll('.edit-client-btn').forEach(button => {
        button.addEventListener('click', function() {
            prepararEdicionCliente(this);
        });
    });

    // 6. Escuchador dinámico para los interruptores (switches) de cambio de estado directo
    document.querySelectorAll('.status-switch').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const targetUrl = this.getAttribute('data-url') || this.getAttribute('th:data-url');
            if (targetUrl) {
                window.location.href = targetUrl;
            }
        });
    });
});

/**
 * Consulta asíncrona a la API interna de DNI
 */
async function buscarDni() {
    const dniInput = document.getElementById('dniInput');
    const btn      = document.getElementById('btnBuscarDni');
    const dni      = dniInput.value;

    if (dni.length !== 8) {
        alert('El DNI debe tener 8 dígitos.');
        return;
    }

    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i>';
    btn.disabled  = true;

    try {
        const response = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data     = await response.json();

        if (data.success) {
            document.getElementById('nombresInput').value   = data.datos.nombres;
            document.getElementById('apellidosInput').value = data.datos.ape_paterno + ' ' + data.datos.ape_materno;
            dniInput.classList.add('is-valid');
            dniInput.classList.remove('is-invalid');
        } else {
            alert('No se encontró información para ese DNI.');
            dniInput.classList.add('is-invalid');
            dniInput.classList.remove('is-valid');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Hubo un problema al conectar con el servicio de DNI.');
    } finally {
        btn.innerHTML = '<i class="fa-solid fa-magnifying-glass"></i>';
        btn.disabled  = false;
    }
}

/**
 * Envía datos JSON de registro mediante Fetch API 
 */
async function guardarCliente() {
    const btnGuardar = document.getElementById('btnGuardarCliente');
    btnGuardar.disabled = true;
    btnGuardar.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin me-2"></i>Guardando...';

    const datos = {
        dni:       document.getElementById('dniInput').value,
        nombres:   document.getElementById('nombresInput').value,
        apellidos: document.getElementById('apellidosInput').value,
        telefono:  document.getElementById('telefonoInput').value,
        correo:    document.getElementById('correoInput').value
    };

    try {
        const res  = await fetch('/api/clientes/guardar-rapido', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(datos)
        });
        const data = await res.json();

        if (res.ok) {
            const modalEl = document.getElementById('modalNuevo');
            const modalInstance = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            modalInstance.hide();
            location.reload(); 
        } else {
            alert('Error: ' + (data.error || 'No se pudo guardar el cliente.'));
        }
    } catch (e) {
        alert('Error de conexión.');
    } finally {
        btnGuardar.disabled = false;
        btnGuardar.innerHTML = 'Registrar Cliente';
    }
}

/**
 * Mapea los atributos de la fila seleccionada y levanta el Modal de edición
 */
function prepararEdicionCliente(btn) {
    const id = btn.getAttribute('data-id');
    const dni = btn.getAttribute('data-dni');
    const nombres = btn.getAttribute('data-nombres');
    const apellidos = btn.getAttribute('data-apellidos');
    const telefono = btn.getAttribute('data-telefono');
    const correo = btn.getAttribute('data-correo');

    // Rellenar campos correspondientes del modal
    document.getElementById('edit-dni').value = dni;
    document.getElementById('edit-nombres').value = nombres;
    document.getElementById('edit-apellidos').value = apellidos;
    document.getElementById('edit-telefono').value = (telefono === '—' || telefono === null) ? '' : telefono;
    document.getElementById('edit-correo').value = (correo === '—' || correo === null) ? '' : correo;

    // Cambiar dinámicamente la ruta de destino del formulario pasando su ID
    document.getElementById('form-editar').action = `/admin/cliente/actualizar/${id}`;

    // Desplegar modal mediante Bootstrap API
    const modalEditar = new bootstrap.Modal(document.getElementById('modalEditar'));
    modalEditar.show();
}