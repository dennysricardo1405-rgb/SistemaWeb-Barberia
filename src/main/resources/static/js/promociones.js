document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("promoForm");
    const inputPorcentaje = document.getElementById("promoPorcentaje");
    const errorDescuento = document.getElementById("errorDescuento");
    const inputInicio = document.getElementById("promoInicio");
    const inputFin = document.getElementById("promoFin");

    // ── 1. CONFIGURACIÓN DE RESTRICCIÓN DE FECHAS ANTERIORES (datetime-local) ──
    const ahora = new Date();
    const anio = ahora.getFullYear();
    const mes = String(ahora.getMonth() + 1).padStart(2, '0');
    const dia = String(ahora.getDate()).padStart(2, '0');
    const horas = String(ahora.getHours()).padStart(2, '0');
    const minutos = String(ahora.getMinutes()).padStart(2, '0');
    
    // Formato requerido por datetime-local: YYYY-MM-DDTHH:mm
    const formatoFechaMinima = `${anio}-${mes}-${dia}T${horas}:${minutos}`;

    if (inputInicio) inputInicio.min = formatoFechaMinima;
    if (inputFin) inputFin.min = formatoFechaMinima;

    // Ajuste dinámico: La fecha fin no puede ser menor a la fecha de inicio elegida
    inputInicio.addEventListener("change", function () {
        if (inputFin) inputFin.min = this.value;
    });

    // ── 2. VALIDACIÓN DEL LÍMITE DE DESCUENTO (Máximo 60%) ──
    inputPorcentaje.addEventListener("input", function () {
        const valor = parseFloat(this.value) || 0;
        if (valor > 60) {
            errorDescuento.style.display = "block";
            this.classList.add("is-invalid");
        } else {
            errorDescuento.style.display = "none";
            this.classList.remove("is-invalid");
        }
    });

    // ── 3. INTERCEPTOR Y CONTROL DE ENVÍO ──
    form.addEventListener("submit", function (e) {
        const porcentaje = parseFloat(inputPorcentaje.value) || 0;

        if (porcentaje > 60) {
            e.preventDefault();
            errorDescuento.style.display = "block";
            inputPorcentaje.focus();
            return false;
        }

        if (inputFin.value && inputInicio.value && inputFin.value < inputInicio.value) {
            e.preventDefault();
            alert("⚠️ Error en Vigencia: La fecha y hora de fin no puede ser anterior al inicio.");
            return false;
        }
    });
});

// ── 4. FUNCIONES DE FLUJO ORIGINALES INTEGRADAS ──
function alternarCamposFlujo() {
    const tipo = document.getElementById("promoTipo").value;
    const wrapperServicio = document.getElementById("wrapperServicio");
    const wrapperProducto = document.getElementById("wrapperProducto");

    if (tipo === "SERVICIO") {
        wrapperServicio.classList.remove("d-none");
        wrapperProducto.classList.add("d-none");
        document.getElementById("promoProductoId").value = "";
        document.getElementById("promoCategoriaId").value = "";
    } else {
        wrapperServicio.classList.add("d-none");
        wrapperProducto.classList.remove("d-none");
        document.getElementById("promoServicioId").value = "";
        alternarAlcanceProducto();
    }
}

function alternarAlcanceProducto() {
    const alcance = document.getElementById("filtroAlcanceProducto").value;
    const wrapperUnico = document.getElementById("subWrapperProductoUnico");
    const wrapperCat = document.getElementById("subWrapperCategoria");

    if (alcance === "ESPECIFICO") {
        wrapperUnico.classList.remove("d-none");
        wrapperCat.classList.add("d-none");
        document.getElementById("promoCategoriaId").value = "";
    } else {
        wrapperUnico.classList.add("d-none");
        wrapperCat.classList.remove("d-none");
        document.getElementById("promoProductoId").value = "";
    }
}

function limpiarFormulario() {
    document.getElementById("promoForm").reset();
    document.getElementById("promoId").value = "";
    document.getElementById("formTitle").innerText = "Nueva Promoción";
    document.getElementById("errorDescuento").style.display = "none";
    document.getElementById("promoPorcentaje").classList.remove("is-invalid");
    alternarCamposFlujo();
}