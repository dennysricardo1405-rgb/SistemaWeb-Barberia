// ============================================================
// clientes.js
// Ruta: static/js/clientes.js
// ============================================================

async function buscarDni() {
    const dniInput = document.getElementById('dniInput');
    const btn      = document.getElementById('btnBuscarDni');
    const dni      = dniInput.value;

    if (dni.length !== 8) {
        alert('El DNI debe tener 8 dígitos.');
        return;
    }

    // Estado de carga
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i>';
    btn.disabled  = true;

    try {
        const response = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data     = await response.json();

        if (data.success) {
            document.getElementById('nombresInput').value   = data.datos.nombres;
            document.getElementById('apellidosInput').value =
                data.datos.ape_paterno + ' ' + data.datos.ape_materno;
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

async function guardarCliente() {
    const btnGuardar = document.getElementById('btnGuardarCliente');
    btnGuardar.disabled = true;
    btnGuardar.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin me-2"></i>Guardando...';

    const datos = {
        dni:       document.getElementById('dniInput').value,
        nombres:   document.getElementById('nombresInput').value,
        apellidos: document.getElementById('apellidosInput').value,
        telefono:  document.querySelector('[name="telefono"]').value,
        correo:    document.querySelector('[name="correo"]').value
    };

    try {
        const res  = await fetch('/api/clientes/guardar-rapido', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(datos)
        });
        const data = await res.json();

        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalNuevo')).hide();
            location.reload(); // refresca la tabla
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