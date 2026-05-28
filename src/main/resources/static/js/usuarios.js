function prepararEdicion(boton) {
    const id = boton.getAttribute('data-id');
    const nombre = boton.getAttribute('data-nombre');
    const email = boton.getAttribute('data-email');
    const perfilId = boton.getAttribute('data-perfil');

    // Seleccionamos el modal por su ID
    const modalElement = document.getElementById('modalUsuario');
    const form = modalElement.querySelector('form');
    
    // Cambiar configuración del modal para Editar
    modalElement.querySelector('.modal-title').innerText = "Modificar Usuario";
    form.action = "/admin/usuarios/editar";
    
    // Manejo del ID oculto
    let inputId = document.getElementById('inputid');
    if(!inputId) {
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

    // SOLUCIÓN AL ERROR: Usar la instancia global de Bootstrap 5
    const modalBootstrap = bootstrap.Modal.getOrCreateInstance(modalElement);
    modalBootstrap.show();


    document.querySelector('form').addEventListener('submit', function(e) {
    const pass = document.querySelector('input[name="password"]').value;
    // Asumiendo que agregas un campo de "repetir contraseña"
    const confirmPass = document.querySelector('#confirmPassword')?.value; 
    
    if (pass && confirmPass && pass !== confirmPass) {
        e.preventDefault();
        alert("¡Error! Las contraseñas no coinciden.");
    }
});
}