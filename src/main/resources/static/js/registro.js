 async function buscarDni() {
        const dniInput = document.getElementById('dniInput');
        const btn      = document.getElementById('btnBuscar');
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
                document.getElementById('apellidosInput').value =
                    data.datos.ape_paterno + ' ' + data.datos.ape_materno;
                dniInput.classList.add('is-valid');
                dniInput.classList.remove('is-invalid');
            } else {
                alert('No se encontró información para ese DNI.');
                dniInput.classList.add('is-invalid');
            }
        } catch (e) {
            alert('Error al conectar con el servicio de DNI.');
        } finally {
            btn.innerHTML = '<i class="fa-solid fa-magnifying-glass"></i>';
            btn.disabled  = false;
        }
    }