function prepararEdicion(boton) {
    const id = boton.getAttribute('data-id');
    const nombre = boton.getAttribute('data-nombre');
    const email = boton.getAttribute('data-email');
    const perfilId = boton.getAttribute('data-perfil');
    const modalElement = document.getElementById('modalUsuario');
    const form = modalElement.querySelector('form');
    modalElement.querySelector('.modal-title').innerText = "Modificar Usuario";
    form.action = "/admin/usuarios/editar";
    let inputId = document.getElementById('inputid');
    if (!inputId) {
        inputId = document.createElement('input');
        inputId.type = 'hidden';
        inputId.name = 'id';
        inputId.id = 'inputid';
        form.appendChild(inputId);
    }
    inputId.value = id;
    form.querySelector('[name="nombre"]').value = nombre;
    form.querySelector('[name="email"]').value = email;
    form.querySelector('[name="perfilId"]').value = perfilId;
    const passField = form.querySelector('[name="password"]');
    passField.required = false;
    passField.placeholder = "Dejar en blanco para mantener actual";
    const modalBootstrap = bootstrap.Modal.getOrCreateInstance(modalElement);
    modalBootstrap.show();
    document.querySelector('form').addEventListener('submit', function (e) {
        const pass = document.querySelector('input[name="password"]').value;
        const confirmPass = document.querySelector('#confirmPassword')?.value;
        if (pass && confirmPass && pass !== confirmPass) {
            e.preventDefault();
            alert("¡Error! Las contraseñas no coinciden.");
        }
    });
}

// ── Contador visual de usuarios ──────────────────────────────
async function actualizarContadorUsuarios() {
    try {
        const response = await fetch('/admin/usuarios/count');
        const data = await response.json();
        const total = data.total;
        const maximo = 5;
        const porcentaje = (total / maximo) * 100;

        const badge = document.getElementById('badgeContador');
        if (badge) badge.textContent = `${total}/${maximo}`;

        const barra = document.getElementById('barraUsuarios');
        const texto = document.getElementById('textoContador');
        if (barra) {
            barra.style.width = porcentaje + '%';
            barra.style.backgroundColor = total >= maximo ? '#ef4444' : '#f97316';
        }
        if (texto) {
            texto.textContent = `${total} de ${maximo} usuario${total !== 1 ? 's' : ''}`;
        }
    } catch (error) {
        console.error('Error al obtener contador:', error);
    }
}

// Ejecutar al cargar la página
actualizarContadorUsuarios();

// ── Botón Nuevo Usuario ──────────────────────────────────────
document.getElementById('btnNuevoUsuario').addEventListener('click', async function () {
    try {
        const response = await fetch('/admin/usuarios/count');
        const data = await response.json();
        if (data.total >= 5) {
            Swal.fire({
                icon: 'warning',
                title: '¡Límite alcanzado!',
                text: 'No puedes agregar más usuarios. El máximo permitido es 5.',
                confirmButtonColor: '#f97316',
                confirmButtonText: 'Entendido',
                background: '#1e1e2e',
                color: '#ffffff'
            });
            return;
        }
        const modalElement = document.getElementById('modalUsuario');
        const form = modalElement.querySelector('form');
        modalElement.querySelector('.modal-title').innerHTML =
            '<i class="fa-solid fa-user-plus me-2"></i>Nuevo Usuario';
        form.action = "/admin/usuarios/guardar";
        form.reset();
        const passField = form.querySelector('[name="password"]');
        passField.required = true;
        passField.placeholder = "";
        const inputId = document.getElementById('inputid');
        if (inputId) inputId.remove();
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        modal.show();
    } catch (error) {
        console.error('Error al verificar usuarios:', error);
    }
});