const $ = (id) => document.getElementById(id);
const MAX_PDF = 10 * 1024 * 1024;
const PASO_PDF = 2; // índice del paso donde se sube el PDF

const pasos = Array.from(document.querySelectorAll(".pn"));
const pestanas = Array.from(document.querySelectorAll("#pestanas button"));
let actual = 0;
let archivo = null;

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
  const v = (id) => $(id).value.trim() || "—";
  $("rs-base").textContent = v("documentoBase");
  $("rs-nave").textContent = v("nombreNave");
  $("rs-imo").textContent = v("imo");
  $("rs-propietario").textContent = v("propietario");
  $("rs-pdf").textContent = archivo ? archivo.name : "—";
}

function ir(i) {
  actual = Math.max(0, Math.min(pasos.length - 1, i));
  pasos.forEach((p, j) => p.classList.toggle("on", j === actual));
  pestanas.forEach((b, j) => b.setAttribute("aria-selected", j === actual));
  $("anterior").hidden = actual === 0;
  $("siguiente").textContent = actual === pasos.length - 1 ? "Enviar solicitud" : "Siguiente";
  if (actual === pasos.length - 1) resumen();
  window.scrollTo(0, 0);
}

pestanas.forEach((b, j) => b.addEventListener("click", () => ir(j)));
$("anterior").addEventListener("click", () => ir(actual - 1));
$("siguiente").addEventListener("click", () => {
  if (actual === pasos.length - 1) {
    enviar();
  } else if (validarPaso(actual)) {
    ir(actual + 1);
  }
});
$("form-solicitud").addEventListener("submit", (e) => e.preventDefault());

// ---------- documento PDF ----------

function pintarPdf() {
  $("fila-pdf").classList.toggle("ok", !!archivo);
  $("pdf-estado").textContent = archivo ? archivo.name : "Sin archivo seleccionado";
  $("pdf-icono").setAttribute("href", archivo ? "#ck" : "#cl");
  const boton = $("pdf-boton");
  boton.textContent = archivo ? "Quitar" : "Subir";
  boton.className = archivo ? "btn r x" : "btn o x";
}

$("pdf-boton").addEventListener("click", () => {
  if (archivo) {
    archivo = null;
    $("pdf").value = "";
    pintarPdf();
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
  pintarPdf();
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
ir(0);
precargar();