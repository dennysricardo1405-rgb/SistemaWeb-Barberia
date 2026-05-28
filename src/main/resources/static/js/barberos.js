// ══════════════════════════════════════════════
//  barberos.js — sin fetch, igual que usuarios
// ══════════════════════════════════════════════

// Rellena el modal de editar con los datos del botón
function prepararEdicion(btn) {
    const id          = btn.dataset.id;
    const nombre      = btn.dataset.nombre;
    const especialidad= btn.dataset.especialidad || '';
    const telefono    = btn.dataset.telefono     || '';
    const diaLibre    = btn.dataset.dialib       || 'MARTES';
    const imagen      = btn.dataset.imagen       || '';

    // Setear la action del form con el id correcto
    document.getElementById('form-editar').action = `/admin/barberos/${id}/actualizar`;

    // Rellenar campos
    document.getElementById('edit-nombre').value       = nombre;
    document.getElementById('edit-especialidad').value = especialidad;
    document.getElementById('edit-telefono').value     = telefono;

    // Seleccionar día libre
    const sel = document.getElementById('edit-diaLibre');
    for (const opt of sel.options) {
        opt.selected = opt.value === diaLibre;
    }

    // Foto
    const preview     = document.getElementById('preview-editar');
    const placeholder = document.getElementById('placeholder-editar');
    if (imagen && imagen !== 'null') {
        preview.src = imagen;
        preview.classList.remove('d-none');
        placeholder.classList.add('d-none');
    } else {
        preview.src = '';
        preview.classList.add('d-none');
        placeholder.classList.remove('d-none');
    }

    // Limpiar input de archivo por si quedó de una edición anterior
    document.getElementById('foto-editar').value = '';

    // Abrir modal
    new bootstrap.Modal(document.getElementById('modalEditar')).show();
}

// Preview de foto en tiempo real
function previewFoto(input, previewId, placeholderId) {
    if (!input.files || !input.files[0]) return;
    const reader = new FileReader();
    reader.onload = e => {
        const img = document.getElementById(previewId);
        const ph  = document.getElementById(placeholderId);
        img.src = e.target.result;
        img.classList.remove('d-none');
        if (ph) ph.classList.add('d-none');
    };
    reader.readAsDataURL(input.files[0]);
}