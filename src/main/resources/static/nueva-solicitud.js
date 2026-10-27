(function () {
  "use strict";

  const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const $ = (id) => document.getElementById(id);
  let formulario;
  let iniciado = false;
  let id = null;
  let version = 0;
  let existe = false;
  let origen = null;
  let guardado = null;
  let archivoGuardado = null;
  let tieneDocumento = false;
  let pendiente = false;
  let temporizador = null;
  let enCurso = null;
  let instantaneaEnCurso = null;
  let cargando = false;
  let errorCarga = false;
  let conflicto = false;
  let sesionCaducada = false;
  let pausaAutomatica = false;
  let enviando = false;
  let enviada = false;
  let saliendo = false;
  let salidaAutorizada = false;
  let mensajeError = "";
  let estado = "Borrador sin guardar";
  const controlesBloqueados = new Map();

  // Las instantáneas incluyen los campos ocultos y la referencia exacta del PDF.
  function instantanea() {
    const datos = {};
    document.querySelectorAll("#form-solicitud [data-k]").forEach((campo) => {
      datos[campo.dataset.k] = campo.value;
    });
    return {
      datos,
      texto: JSON.stringify(datos),
      paso: formulario.obtenerPaso(),
      archivo: formulario.obtenerArchivo(),
      origen
    };
  }

  function iguales(a, b) {
    return !!a && !!b && a.texto === b.texto && a.paso === b.paso
      && a.archivo === b.archivo && a.origen === b.origen;
  }

  function tieneDatos(valor) {
    return !!valor.archivo || Object.values(valor.datos).some((dato) => dato.trim());
  }

  function actualizarPendiente() {
    const valor = instantanea();
    const cambioDuranteGuardado = !!instantaneaEnCurso && !iguales(valor, instantaneaEnCurso);
    pendiente = (!iguales(valor, guardado) || cambioDuranteGuardado)
      && (existe || !!instantaneaEnCurso || tieneDatos(valor));
    return valor;
  }

  function cancelarTemporizador() {
    clearTimeout(temporizador);
    temporizador = null;
  }

  function pintar() {
    $("borrador-estado").textContent = estado;
    $("borrador-estado").dataset.estado = conflicto ? "conflicto"
      : cargando ? "cargando" : mensajeError ? "error" : enCurso ? "guardando"
        : pendiente ? "pendiente" : existe ? "guardado" : "pendiente";
    $("borrador-error").textContent = mensajeError;
    $("borrador-error").hidden = !mensajeError;
    $("borrador-login").hidden = !sesionCaducada;
    $("guardar-copia").hidden = !conflicto;
    $("guardar-copia").disabled = cargando || enviando || saliendo || !!enCurso;
    $("guardar-borrador").disabled = cargando || errorCarga || conflicto || enviando || enviada || saliendo || !!enCurso;
    $("borrador-reintentar").hidden = !mensajeError || conflicto;
    $("borrador-reintentar").textContent = errorCarga ? "Reintentar carga" : "Reintentar guardado";
    $("borrador-reintentar").disabled = cargando || enviando || saliendo || !!enCurso;
  }

  function bloquear() {
    document.querySelectorAll(
      "#form-solicitud input, #form-solicitud select, #form-solicitud textarea, "
      + "#form-solicitud button, #pestanas button, #anterior, #siguiente"
    ).forEach((control) => {
      if (!controlesBloqueados.has(control)) controlesBloqueados.set(control, control.disabled);
      control.disabled = true;
    });
    $("asistente").setAttribute("aria-busy", "true");
  }

  function desbloquear() {
    controlesBloqueados.forEach((deshabilitado, control) => { control.disabled = deshabilitado; });
    controlesBloqueados.clear();
    $("asistente").removeAttribute("aria-busy");
    formulario.actualizar();
  }

  function programar() {
    cancelarTemporizador();
    if (!pendiente || cargando || errorCarga || conflicto || sesionCaducada
      || pausaAutomatica || enviando || enviada || saliendo || enCurso) return;
    temporizador = setTimeout(() => { temporizador = null; guardar(); }, 900);
  }

  function cambio() {
    if (!iniciado || cargando || errorCarga || enviando || enviada || saliendo) return;
    actualizarPendiente();
    if (!conflicto && !sesionCaducada && !mensajeError && !enCurso) {
      estado = pendiente ? "Cambios sin guardar" : (existe ? "Borrador guardado" : "Borrador sin guardar");
    }
    pintar();
    programar();
  }

  function nuevoId() {
    if (typeof crypto.randomUUID === "function") return crypto.randomUUID();
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
    return hex.slice(0, 8) + "-" + hex.slice(8, 12) + "-" + hex.slice(12, 16)
      + "-" + hex.slice(16, 20) + "-" + hex.slice(20);
  }

  function actualizarUrl() {
    const url = new URL(location.href);
    url.searchParams.set("borrador", id);
    history.replaceState(null, "", url.pathname + url.search + url.hash);
  }

  function sesionVencida() {
    sesionCaducada = true;
    pausaAutomatica = true;
    cancelarTemporizador();
    estado = "Sesión vencida · cambios conservados en esta página";
    mensajeError = "Inicia sesión en otra pestaña y vuelve aquí para reintentar. Los cambios siguen en el formulario.";
    pintar();
  }

  function mostrarConflicto() {
    conflicto = true;
    pausaAutomatica = true;
    cancelarTemporizador();
    estado = "Este borrador cambió en otra ventana";
    mensajeError = "Tus cambios siguen aquí. Guarda una copia para conservarlos sin sobrescribir el otro borrador.";
    pintar();
  }

  async function leerRespuesta(respuesta) {
    const detalle = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
      const error = new Error(detalle.mensaje || "No se pudo guardar el borrador.");
      error.estado = respuesta.status;
      throw error;
    }
    return detalle;
  }

  function informarError(error) {
    pausaAutomatica = true;
    if (error.estado === 401) sesionVencida();
    else if (error.estado === 409) mostrarConflicto();
    else {
      estado = "Cambios sin guardar";
      mensajeError = error instanceof TypeError
        ? "No se pudo conectar con el servidor. Reintenta el guardado."
        : error.message || "No se pudo guardar el borrador. Reintenta el guardado.";
      pintar();
    }
  }

  async function guardar(manual = false) {
    cancelarTemporizador();
    if (!iniciado || cargando || errorCarga || conflicto || enviando || enviada) return false;
    if (enCurso) {
      const correcto = await enCurso;
      if (!correcto) return false;
      actualizarPendiente();
      return manual && pendiente ? guardar(true) : true;
    }
    const valor = actualizarPendiente();
    if ((!pendiente && existe) || (!manual && !tieneDatos(valor) && !existe)) return true;
    if (!manual && (pausaAutomatica || sesionCaducada)) return false;
    try {
      if (!id) id = nuevoId();
    } catch {
      informarError(new Error("Este navegador no permite crear un identificador seguro para el borrador."));
      return false;
    }
    if (manual) {
      sesionCaducada = false;
      pausaAutomatica = false;
    }
    mensajeError = "";
    estado = "Guardando borrador…";
    const datos = new FormData();
    datos.append("datos", valor.texto);
    datos.append("paso", String(valor.paso));
    datos.append("version", String(version));
    if (valor.origen) datos.append("origenSolicitudId", String(valor.origen));
    if (valor.archivo && valor.archivo !== archivoGuardado) datos.append("documento", valor.archivo);
    else if (!valor.archivo && tieneDocumento) datos.append("quitarDocumento", "true");

    // Una sola petición escribe cada versión; una edición posterior conserva su propia instantánea.
    instantaneaEnCurso = valor;
    enCurso = (async () => {
      try {
        const respuesta = await fetch("/api/borradores/" + id, { method: "PUT", body: datos });
        const detalle = await leerRespuesta(respuesta);
        if (!Number.isSafeInteger(detalle.version) || detalle.version < 0) {
          throw new Error("El servidor devolvió una versión de borrador inválida. Reintenta el guardado.");
        }
        version = detalle.version;
        existe = true;
        guardado = valor;
        archivoGuardado = valor.archivo;
        tieneDocumento = !!detalle.tieneDocumento;
        sesionCaducada = false;
        pausaAutomatica = false;
        mensajeError = "";
        actualizarUrl();
        actualizarPendiente();
        const fecha = detalle.actualizado ? new Date(detalle.actualizado) : null;
        const hora = fecha && !Number.isNaN(fecha.getTime())
          ? " · " + fecha.toLocaleTimeString("es", { hour: "2-digit", minute: "2-digit" }) : "";
        estado = pendiente ? "Cambios sin guardar" : "Borrador guardado" + hora;
        return true;
      } catch (error) {
        actualizarPendiente();
        informarError(error);
        return false;
      } finally {
        enCurso = null;
        instantaneaEnCurso = null;
        pintar();
        programar();
      }
    })();
    pintar();
    return enCurso;
  }

  async function cargar() {
    cargando = true;
    errorCarga = false;
    mensajeError = "";
    estado = "Cargando borrador…";
    bloquear();
    pintar();
    try {
      if (!id || !UUID.test(id)) throw new Error("El identificador del borrador no es válido.");
      const respuesta = await fetch("/api/borradores/" + id, { cache: "no-store" });
      const detalle = await leerRespuesta(respuesta);
      if (!detalle.datos || typeof detalle.datos !== "object" || Array.isArray(detalle.datos)
        || !Number.isSafeInteger(detalle.version) || detalle.version < 0
        || ![1, 2, 3].includes(detalle.paso)) throw new Error("No se pudo leer este borrador.");
      let pdf = null;
      if (detalle.tieneDocumento) {
        const adjunto = await fetch("/api/borradores/" + id + "/documento", { cache: "no-store" });
        if (!adjunto.ok) {
          const error = new Error("No se pudo recuperar el PDF del borrador. Reintenta la carga.");
          error.estado = adjunto.status;
          throw error;
        }
        const tipo = (adjunto.headers.get("Content-Type") || "").split(";")[0].trim().toLowerCase();
        if (tipo !== "application/pdf") throw new Error("El servidor no devolvió el PDF del borrador. Reintenta la carga.");
        const contenido = await adjunto.blob();
        if (!contenido.size || contenido.size > 10 * 1024 * 1024) {
          throw new Error("El PDF recuperado no es válido o supera los 10 MB. Reintenta la carga.");
        }
        pdf = new File([contenido], detalle.nombreDocumento || "documento.pdf", { type: "application/pdf" });
      }
      let origenValidado = null;
      if (detalle.origenSolicitudId) {
        origenValidado = await formulario.precargar(String(detalle.origenSolicitudId), true);
        if (!origenValidado) {
          const error = new Error("No se pudo comprobar la solicitud de origen. Reintenta la carga.");
          if (sesionCaducada) error.estado = 401;
          throw error;
        }
      }
      origen = origenValidado;
      formulario.restaurar(detalle, pdf);
      version = detalle.version;
      existe = true;
      tieneDocumento = !!detalle.tieneDocumento;
      archivoGuardado = pdf;
      guardado = instantanea();
      pendiente = false;
      sesionCaducada = false;
      pausaAutomatica = false;
      estado = "Borrador guardado · recuperado";
      mensajeError = "";
      desbloquear();
    } catch (error) {
      errorCarga = true;
      pausaAutomatica = true;
      if (error.estado === 401) sesionVencida();
      else {
        estado = "No se pudo cargar el borrador";
        mensajeError = error.estado === 404
          ? "Este borrador ya no está disponible. Puedes reintentar su carga o volver a Mis solicitudes."
          : error instanceof TypeError ? "No se pudo conectar con el servidor. Reintenta la carga."
            : error.message || "No se pudo cargar el borrador. Reintenta la carga.";
      }
    } finally {
      cargando = false;
      pintar();
    }
  }

  async function guardarCopia() {
    if (!conflicto || cargando || enviando || enCurso || saliendo) return;
    try {
      id = nuevoId();
    } catch {
      mensajeError = "Este navegador no permite crear un identificador seguro para la copia.";
      pintar();
      return;
    }
    version = 0;
    existe = false;
    conflicto = false;
    archivoGuardado = null;
    tieneDocumento = false;
    guardado = null;
    pendiente = true;
    await guardar(true);
  }

  async function antesDeEnviar() {
    cancelarTemporizador();
    if (cargando || errorCarga || conflicto || enviando || enviada || saliendo) return null;
    if (sesionCaducada) {
      sesionVencida();
      return null;
    }
    enviando = true;
    bloquear();
    pintar();
    if (enCurso && !(await enCurso)) return null;
    if (conflicto || sesionCaducada) return null;
    return existe
      ? { url: "/api/borradores/" + id + "/enviar", version,
        incluirDocumento: formulario.obtenerArchivo() !== archivoGuardado }
      : { url: "/api/naves", incluirDocumento: true };
  }

  function finalizarEnvio() {
    enviando = false;
    if (!errorCarga && !cargando) desbloquear();
    if (!enviada && !errorCarga && !cargando) {
      actualizarPendiente();
      programar();
    }
    pintar();
  }

  function enviado() {
    enviada = true;
    pendiente = false;
    guardado = instantanea();
    mensajeError = "";
    cancelarTemporizador();
    const url = new URL(location.href);
    url.searchParams.delete("borrador");
    url.searchParams.delete("corregir");
    history.replaceState(null, "", url.pathname + url.search + url.hash);
  }

  async function salir(destino, cerrarSesion, foco) {
    if (saliendo || cargando || enviando) return;
    saliendo = true;
    bloquear();
    cancelarTemporizador();
    pintar();
    let correcto = enCurso ? await enCurso : true;
    actualizarPendiente();
    if (pendiente) correcto = !conflicto && !errorCarga && await guardar(true);
    if ((!correcto || pendiente) && !window.confirm("No se pudieron guardar los cambios. ¿Quieres salir sin guardarlos?")) {
      saliendo = false;
      if (!errorCarga) desbloquear();
      pintar();
      foco?.focus({ preventScroll: true });
      return;
    }
    salidaAutorizada = true;
    if (cerrarSesion) {
      try {
        await fetch("/api/auth/logout", { method: "POST" });
      } catch {
        // El inicio de sesión permite recuperar la sesión si falló la conexión al salir.
      }
    }
    location.href = destino;
  }

  async function iniciar(adaptador) {
    formulario = adaptador;
    iniciado = true;
    guardado = instantanea();
    id = new URLSearchParams(location.search).get("borrador");
    const enfocarError = () => {
      if (mensajeError) $(conflicto ? "guardar-copia" : "borrador-reintentar").focus({ preventScroll: true });
    };
    $("guardar-borrador").addEventListener("click", async () => {
      if (!(await guardar(true))) enfocarError();
    });
    $("guardar-copia").addEventListener("click", guardarCopia);
    $("borrador-reintentar").addEventListener("click", async () => {
      if (errorCarga) await cargar();
      else await guardar(true);
      enfocarError();
    });
    $("form-solicitud").addEventListener("input", cambio);
    $("form-solicitud").addEventListener("change", cambio);
    window.addEventListener("beforeunload", (evento) => {
      if (!pendiente || salidaAutorizada || enviada) return;
      evento.preventDefault();
      evento.returnValue = "";
    });

    // La captura espera el guardado antes de ejecutar los enlaces y el cierre de sesión compartido.
    document.querySelector(".bar").addEventListener("click", (evento) => {
      const control = evento.target.closest("a, #salir");
      if (!control) return;
      if (control.tagName === "A" && (evento.button !== 0 || evento.ctrlKey || evento.metaKey
        || evento.shiftKey || evento.altKey || control.target === "_blank")) return;
      evento.preventDefault();
      evento.stopImmediatePropagation();
      if (cargando || enviando || saliendo) return;
      salir(control.id === "salir" ? "/login.html" : control.href, control.id === "salir", control);
    }, true);

    if (id !== null) {
      await cargar();
    } else if (new URLSearchParams(location.search).has("corregir")) {
      cargando = true;
      bloquear();
      estado = "Cargando solicitud para corregir…";
      pintar();
      try {
        origen = await formulario.precargar();
      } finally {
        cargando = false;
        desbloquear();
        cambio();
      }
    } else {
      pintar();
    }
  }

  window.borradorFormulario = {
    iniciar, cambio, antesDeEnviar, enviado, finalizarEnvio, sesionVencida,
    conflicto: mostrarConflicto
  };
})();

const $ = (id) => document.getElementById(id);
const MAX_PDF = 10 * 1024 * 1024;
const PASO_PDF = 2; // índice del paso donde se sube el PDF

const pasos = Array.from(document.querySelectorAll(".pn"));
const pestanas = Array.from(document.querySelectorAll("#pestanas button"));
let actual = 0;
let archivo = null;
let vistaPdfUrl = null;
let enviando = false;
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
  window.borradorFormulario?.cambio();
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
  window.borradorFormulario?.cambio();
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
  if (enviando) return;
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

  const boton = $("siguiente");
  enviando = true;
  if (!window.borradorFormulario) boton.disabled = true;
  try {
    const destino = window.borradorFormulario
      ? await window.borradorFormulario.antesDeEnviar()
      : { url: "/api/naves", incluirDocumento: true };
    if (!destino) return;

    const datos = new FormData();
    document.querySelectorAll("#form-solicitud [data-k]").forEach((el) => {
      if (!visible(el)) return;
      const v = el.value.trim();
      if (v) datos.append(el.dataset.k, v);
    });
    if (archivo && destino.incluirDocumento !== false) datos.append("documento", archivo);
    if (destino.version !== undefined) datos.append("version", String(destino.version));

    const r = await fetch(destino.url, { method: "POST", body: datos });
    if (r.status === 401) {
      if (window.borradorFormulario) window.borradorFormulario.sesionVencida();
      else location.href = "/login.html";
      return;
    }
    const json = await r.json().catch(() => ({}));
    if (r.ok) {
      window.borradorFormulario?.enviado();
      $("ok-tramite").textContent = json.tramite;
      $("ok-nave").textContent = json.nombreNave;
      $("ok-estado").textContent = json.estado;
      $("asistente").hidden = true;
      $("enviada").hidden = false;
      window.scrollTo(0, 0);
    } else {
      if (r.status === 409) window.borradorFormulario?.conflicto();
      caja.className = "aviso mal";
      caja.textContent = json.mensaje || "No se pudo enviar la solicitud.";
    }
  } catch {
    caja.className = "aviso mal";
    caja.textContent = "No se pudo conectar con el servidor.";
  } finally {
    enviando = false;
    if (window.borradorFormulario) window.borradorFormulario.finalizarEnvio();
    else boton.disabled = false;
  }
}

// ---------- corrección de una solicitud rechazada ----------

async function precargar(id = null, soloMensaje = false) {
  id = id || new URLSearchParams(location.search).get("corregir");
  if (!id || !/^\d+$/.test(String(id))) return null;

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
      if (window.borradorFormulario) window.borradorFormulario.sesionVencida();
      else location.href = "/login.html";
      return null;
    }
    if (!r.ok) throw new Error();
    const d = await r.json();
    if (d.estado !== "Rechazado") throw new Error();

    // Las claves de los campos (data-k) coinciden con las del detalle
    if (!soloMensaje) {
      document.querySelectorAll("#form-solicitud [data-k]").forEach((el) => {
        const v = d[el.dataset.k];
        if (v !== null && v !== undefined) el.value = v;
      });
      condiciones();
      resumen();
    }

    caja.replaceChildren(
      texto("Corrigiendo la solicitud " + d.tramite, ""),
      texto("Observación del funcionario:", d.observacion || "—"),
      texto("", soloMensaje
        ? "Revise los datos y el PDF de este borrador antes de enviar."
        : "Revise los datos y vuelva a adjuntar el PDF antes de enviar.")
    );
    caja.hidden = false;
    return String(id);
  } catch {
    caja.replaceChildren(texto("", "No se pudo cargar la solicitud a corregir."));
    caja.hidden = false;
    return null;
  }
}

condiciones();
resumen();
ir(0);
if (window.borradorFormulario) {
  window.borradorFormulario.iniciar({
    obtenerPaso: () => actual + 1,
    obtenerArchivo: () => archivo,
    precargar,
    restaurar(detalle, pdf) {
      document.querySelectorAll("#form-solicitud [data-k]").forEach((el) => {
        el.value = detalle.datos[el.dataset.k] ?? "";
      });
      archivo = pdf;
      condiciones();
      actualizarVistaPdf();
      ir(detalle.paso - 1);
    },
    actualizar: resumen
  });
} else {
  precargar();
}
