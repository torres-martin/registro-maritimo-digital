const $ = (id) => document.getElementById(id);
const MAX_PDF = 10 * 1024 * 1024;
const PASO_PDF = 2; // índice del paso donde se sube el PDF

const pasos = Array.from(document.querySelectorAll(".pn"));
const pestanas = Array.from(document.querySelectorAll("#pestanas button"));
let actual = 0;
let archivo = null;
let vistaPdfUrl = null;
const formatoNumero = new Intl.NumberFormat("es-ES", { maximumFractionDigits: 3 });
const formatoPrecio = new Intl.NumberFormat("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// La fecha de celebración no puede ser futura
(function () {
  const h = new Date();
  const hoy = h.getFullYear() + "-" + String(h.getMonth() + 1).padStart(2, "0") + "-" + String(h.getDate()).padStart(2, "0");
  $("fechaCelebracion").max = hoy;
})();

function visible(el) {
  return !el.closest("[hidden]");
}

function error(el, mensaje) {
  const campo = el.closest(".campo");
  campo.querySelector(".err").textContent = mensaje;
  campo.classList.toggle("campo-error", !!mensaje);
}

// ---------- campos condicionales ----------

function condiciones() {
  $("c-precio").hidden = $("documentoBase").value !== "Contrato de compraventa";
}
$("documentoBase").addEventListener("change", condiciones);

// ---------- validación ----------

function imoValido(v) {
  const d = v.toUpperCase().replace(/\s/g, "").replace(/^IMO/, "");
  if (!/^\d{7}$/.test(d)) return false;
  let suma = 0;
  for (let i = 0; i < 6; i++) suma += Number(d[i]) * (7 - i);
  return suma % 10 === Number(d[6]);
}

function validarControl(el) {
  const v = el.value.trim();
  let msg = "";
  if (el.dataset.req !== undefined && !v) {
    msg = "Este campo es obligatorio.";
  } else if (v && el.id === "imo" && !/^\d{7}$/.test(v.toUpperCase().replace(/\s/g, "").replace(/^IMO/, ""))) {
    msg = "El IMO debe tener 7 dígitos (ejemplo: 1234567).";
  } else if (v && el.type === "number" && !(Number(v) > 0)) {
    msg = "Debe ser un número mayor que 0.";
  } else if (v && el.id === "fechaCelebracion" && el.max && v > el.max) {
    msg = "No puede ser una fecha futura.";
  }
  error(el, msg);
  return !msg;
}

function validarPaso(i) {
  let ok = true;
  pasos[i].querySelectorAll("[data-k]").forEach((el) => {
    if (!visible(el)) {
      error(el, "");
      return;
    }
    if (!validarControl(el)) ok = false;
  });
  if (i === PASO_PDF) {
    if (!archivo) {
      $("err-pdf").textContent = "Adjunte el documento en PDF.";
      ok = false;
    } else {
      $("err-pdf").textContent = "";
    }
  }
  return ok;
}

// ---------- navegación ----------

function resumen() {
  const valor = (id) => $(id).value.trim();
  const mostrar = (destino, origen) => {
    $(destino).textContent = valor(origen) || "No indicado";
  };

  mostrar("rs-base", "documentoBase");
  mostrar("rs-transmitente", "nombreTransmitente");
  mostrar("rs-propietario", "propietario");
  $("rs-nave").textContent = valor("nombreNave") || "Nombre de la nave";
  $("rs-imo").textContent = valor("imo").toUpperCase().replace(/\s/g, "").replace(/^IMO/, "") || "No indicado";
  mostrar("rs-tipo", "tipoNave");
  mostrar("rs-distintivo", "distintivoLlamada");
  mostrar("rs-patente", "patenteNavegacion");
  const arqueo = valor("arqueoBruto");
  $("rs-arqueo").textContent = arqueo && Number.isFinite(Number(arqueo))
    ? formatoNumero.format(Number(arqueo)) : "No indicado";

  mostrar("rs-solicitante", "solicitante");
  mostrar("rs-abogado", "abogado");
  const fecha = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor("fechaCelebracion"));
  $("rs-fecha").textContent = fecha ? fecha[3] + "/" + fecha[2] + "/" + fecha[1] : "No indicada";
  $("rs-precio-bloque").hidden = valor("documentoBase") !== "Contrato de compraventa";
  const precio = valor("precioVenta");
  $("rs-precio").textContent = precio && Number.isFinite(Number(precio))
    ? formatoPrecio.format(Number(precio)) : "No indicado";

  $("rs-pdf").textContent = archivo ? archivo.name : "Sin archivo adjunto";
  if (archivo) {
    const megabytes = archivo.size / (1024 * 1024);
    const tamano = megabytes >= 1
      ? new Intl.NumberFormat("es-ES", { maximumFractionDigits: 2 }).format(megabytes) + " MB"
      : Math.max(1, Math.ceil(archivo.size / 1024)) + " KB";
    $("rs-pdf-meta").textContent = "PDF · " + tamano;
  } else {
    $("rs-pdf-meta").textContent = "Adjunta un PDF de hasta 10 MB";
  }
  $("rs-ver-pdf").disabled = !vistaPdfUrl;
  $("rs-pdf-accion").textContent = archivo ? "Cambiar" : "Adjuntar";
  actualizarEstadoResumen();
}

// El estado vacío solo sustituye la lectura, sin ocultar los controles originales.
function actualizarEstadoResumen() {
  const contenedor = document.querySelector(".resumen-profesional");
  const contenido = $("resumen-contenido");
  const hayDatos = Boolean(archivo) || Array.from(document.querySelectorAll("#form-solicitud [data-k]"))
    .some((campo) => visible(campo) && campo.value.trim());
  const devolverFoco = !hayDatos && (contenido.contains(document.activeElement) || $("resumen-ficha").contains(document.activeElement));

  $("resumen-vacio").hidden = hayDatos;
  contenido.hidden = !hayDatos;
  contenedor.dataset.vacio = String(!hayDatos);
  contenedor.setAttribute("aria-labelledby", hayDatos ? "titulo-resumen" : "rp-vacio-titulo");

  if (!hayDatos) {
    mostrarFicha(false);
    if (devolverFoco) $("resumen-completar").focus({ preventScroll: true });
  }
}

function ir(i) {
  actual = Math.max(0, Math.min(pasos.length - 1, i));

  pasos.forEach((p, j) => {
    p.classList.toggle("on", j === actual);
    p.setAttribute("aria-hidden", j !== actual);
  });

  pestanas.forEach((b, j) => {
    b.setAttribute("aria-selected", j === actual);
    b.tabIndex = j === actual ? 0 : -1;
  });

  $("estado-paso").textContent =
    "Paso " + (actual + 1) + " de " + pasos.length;

  $("anterior").hidden = actual === 0;

  $("siguiente").textContent =
    actual === pasos.length - 1
      ? "Enviar solicitud"
      : "Siguiente";

  if (actual === pasos.length - 1) {
    resumen();
  }

  window.scrollTo(0, 0);
}

pestanas.forEach((b, j) => {
  b.addEventListener("click", () => ir(j));

  // Permite recorrer las pestañas con las flechas del teclado.
  b.addEventListener("keydown", (e) => {
    let destino;

    if (e.key === "ArrowRight") {
      destino = (j + 1) % pasos.length;
    } else if (e.key === "ArrowLeft") {
      destino = (j + pasos.length - 1) % pasos.length;
    } else if (e.key === "Home") {
      destino = 0;
    } else if (e.key === "End") {
      destino = pasos.length - 1;
    } else {
      return;
    }

    e.preventDefault();
    ir(destino);
    pestanas[destino].focus();
  });
});
$("anterior").addEventListener("click", () => ir(actual - 1));
$("siguiente").addEventListener("click", () => {
  if (actual === pasos.length - 1) {
    enviar();
  } else if (validarPaso(actual)) {
    ir(actual + 1);
  }
});
$("form-solicitud").addEventListener("submit", (e) => e.preventDefault());
$("form-solicitud").addEventListener("input", resumen);
$("form-solicitud").addEventListener("change", resumen);

// La ficha es un panel de lectura; ocultarla no elimina los valores del formulario.
function mostrarFicha(abierta, enfocarControl = false) {
  const ficha = $("resumen-ficha");
  const control = $("resumen-ficha-toggle");
  const devolverFoco = enfocarControl || (!abierta && ficha.contains(document.activeElement));
  const mostrar = abierta && !$("resumen-contenido").hidden;

  ficha.hidden = !mostrar;
  document.querySelector(".resumen-profesional").dataset.fichaAbierta = String(mostrar);
  control.setAttribute("aria-expanded", String(mostrar));
  $("resumen-ficha-toggle-texto").textContent = mostrar ? "Ocultar ficha" : "Ver ficha completa";

  if (devolverFoco) {
    const destino = $("resumen-contenido").hidden ? $("resumen-completar") : control;
    destino.focus({ preventScroll: true });
  }
}

$("resumen-ficha-toggle").addEventListener("click", () => mostrarFicha($("resumen-ficha").hidden));
$("resumen-ficha-cerrar").addEventListener("click", () => mostrarFicha(false, true));
$("resumen-ficha").addEventListener("keydown", (evento) => {
  if (evento.key === "Escape") {
    evento.preventDefault();
    mostrarFicha(false, true);
  }
});

// La edición vuelve al campo original sin reiniciar el formulario ni el adjunto.
document.querySelectorAll("[data-editar-paso]").forEach((boton) => {
  boton.addEventListener("click", () => {
    ir(Number(boton.dataset.editarPaso));
    const campo = $(boton.dataset.editarCampo);
    campo.focus({ preventScroll: true });
    campo.scrollIntoView({ block: "center", behavior: "instant" });
  });
});

$("rs-cambiar-pdf").addEventListener("click", () => $("pdf").click());
$("rs-ver-pdf").addEventListener("click", () => {
  if (vistaPdfUrl) window.open(vistaPdfUrl, "_blank", "noopener,noreferrer");
});

// ---------- documento PDF ----------

function pintarPdf() {
  $("fila-pdf").classList.toggle("ok", !!archivo);
  $("pdf-estado").textContent = archivo ? archivo.name : "Sin archivo seleccionado";
  $("pdf-icono").setAttribute("href", archivo ? "#ck" : "#cl");
  const boton = $("pdf-boton");
  boton.textContent = archivo ? "Quitar" : "Subir";
  boton.className = archivo ? "btn r x" : "btn o x";
  resumen();
}

// Cada archivo mantiene una sola URL temporal hasta sustituirlo o quitarlo.
function actualizarVistaPdf() {
  if (vistaPdfUrl) URL.revokeObjectURL(vistaPdfUrl);
  vistaPdfUrl = archivo ? URL.createObjectURL(archivo) : null;
  pintarPdf();
}

$("pdf-boton").addEventListener("click", () => {
  if (archivo) {
    archivo = null;
    $("pdf").value = "";
    actualizarVistaPdf();
  } else {
    $("pdf").click();
  }
});

$("pdf").addEventListener("change", () => {
  const f = $("pdf").files[0];
  $("err-pdf").textContent = "";
  if (!f) return;
  if (f.type !== "application/pdf") {
    $("err-pdf").textContent = "El archivo debe ser un PDF.";
    $("pdf").value = "";
    return;
  }
  if (f.size > MAX_PDF) {
    $("err-pdf").textContent = "El PDF no puede pesar más de 10 MB.";
    $("pdf").value = "";
    return;
  }
  archivo = f;
  actualizarVistaPdf();
});

// ---------- envío ----------

async function enviar() {
  const caja = $("resultado");
  caja.className = "aviso";
  caja.textContent = "";

  let primero = -1;
  for (let i = 0; i < pasos.length; i++) {
    if (!validarPaso(i) && primero < 0) primero = i;
  }
  if (primero >= 0) {
    ir(primero);
    return;
  }

  const datos = new FormData();
  document.querySelectorAll("#form-solicitud [data-k]").forEach((el) => {
    if (!visible(el)) return;
    const v = el.value.trim();
    if (v) datos.append(el.dataset.k, v);
  });
  datos.append("documento", archivo);

  const boton = $("siguiente");
  boton.disabled = true;
  try {
    const r = await fetch("/api/naves", { method: "POST", body: datos });
    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }
    const json = await r.json().catch(() => ({}));
    if (r.ok) {
      $("ok-tramite").textContent = json.tramite;
      $("ok-nave").textContent = json.nombreNave;
      $("ok-estado").textContent = json.estado;
      $("asistente").hidden = true;
      $("enviada").hidden = false;
      window.scrollTo(0, 0);
    } else {
      caja.className = "aviso mal";
      caja.textContent = json.mensaje || "No se pudo enviar la solicitud.";
    }
  } catch {
    caja.className = "aviso mal";
    caja.textContent = "No se pudo conectar con el servidor.";
  } finally {
    boton.disabled = false;
  }
}

// ---------- corrección de una solicitud rechazada ----------

async function precargar() {
  const id = new URLSearchParams(location.search).get("corregir");
  if (!id || !/^\d+$/.test(id)) return;

  const caja = $("correccion");
  const texto = (etiqueta, contenido) => {
    const p = document.createElement("div");
    if (etiqueta) {
      const t = document.createElement("strong");
      t.textContent = etiqueta;
      p.append(t, " ");
    }
    p.append(contenido);
    return p;
  };

  try {
    const r = await fetch("/api/naves/" + id);
    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }
    if (!r.ok) throw new Error();
    const d = await r.json();
    if (d.estado !== "Rechazado") return;

    // Las claves de los campos (data-k) coinciden con las del detalle
    document.querySelectorAll("#form-solicitud [data-k]").forEach((el) => {
      const v = d[el.dataset.k];
      if (v !== null && v !== undefined) el.value = v;
    });
    condiciones();
    resumen();

    caja.replaceChildren(
      texto("Corrigiendo la solicitud " + d.tramite, ""),
      texto("Observación del funcionario:", d.observacion || "—"),
      texto("", "Revise los datos y vuelva a adjuntar el PDF antes de enviar.")
    );
    caja.hidden = false;
  } catch {
    caja.replaceChildren(texto("", "No se pudo cargar la solicitud a corregir."));
    caja.hidden = false;
  }
}

condiciones();
resumen();
ir(0);
precargar();
