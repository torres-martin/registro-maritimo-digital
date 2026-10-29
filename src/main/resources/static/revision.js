const $ = (id) => document.getElementById(id);

const CLASES = {
  "En revisión": "en-revision",
  "Aprobado": "aprobado",
  "Rechazado": "rechazado"
};

const id = new URLSearchParams(location.search).get("id");
const idValido = Boolean(id && /^\d+$/.test(id));

let solicitudActual = null;
let operacionEnCurso = false;
let cargandoSolicitud = false;
let documentoUrl = null;
let documentoPeticion = null;
let documentoVersion = 0;

function aviso(texto, ok = false) {
  const caja = $("aviso");

  caja.className = texto
    ? (ok ? "aviso ok" : "aviso mal")
    : "aviso";

  caja.textContent = texto;
}

function hayDato(v) {
  return v !== null &&
    v !== undefined &&
    String(v).trim() !== "";
}

function valor(v) {
  if (!hayDato(v)) return "No indicado";

  return typeof v === "boolean"
    ? (v ? "Sí" : "No")
    : String(v);
}

function numero(v) {
  return hayDato(v) && Number.isFinite(Number(v))
    ? new Intl.NumberFormat("es").format(Number(v))
    : valor(v);
}

function fecha(v) {
  const iso = String(v || "").trim();

  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return "";

  const dia = new Date(iso + "T00:00:00Z");

  if (
    Number.isNaN(dia.getTime()) ||
    dia.toISOString().slice(0, 10) !== iso
  ) {
    return "";
  }

  return new Intl.DateTimeFormat("es", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    timeZone: "UTC"
  }).format(dia);
}

function fechaDato(v) {
  return fecha(v) || "No indicada";
}

function extras(d) {
  try {
    const datos = d.adicionales
      ? JSON.parse(d.adicionales)
      : {};

    return datos &&
      typeof datos === "object" &&
      !Array.isArray(datos)
      ? datos
      : {};
  } catch {
    return {};
  }
}

// Las etiquetas y los valores del expediente se insertan como texto.
function seccion(titulo, filas) {
  const validas = filas.filter(([, v]) => hayDato(v));

  if (!validas.length) return null;

  const bloque = document.createElement("section");
  bloque.className = "revision-grupo";

  const h = document.createElement("h4");
  h.textContent = titulo;

  const dl = document.createElement("dl");
  dl.className = "revision-kv";

  validas.forEach(([etiqueta, v, formato]) => {
    const dato = document.createElement("div");
    const dt = document.createElement("dt");
    const dd = document.createElement("dd");

    dt.textContent = etiqueta;
    dd.textContent = formato ? formato(v) : valor(v);

    dato.append(dt, dd);
    dl.append(dato);
  });

  bloque.append(h, dl);

  return bloque;
}

function mostrarFicha(abierta, devolverFoco = false) {
  const ficha = $("ficha-completa");

  if (
    !abierta &&
    (devolverFoco || ficha.contains(document.activeElement))
  ) {
    $("ficha-toggle").focus();
  }

  ficha.hidden = !abierta;

  $("ficha-toggle").setAttribute(
    "aria-expanded",
    String(abierta)
  );

  $("ficha-toggle-texto").textContent = abierta
    ? "Ocultar ficha"
    : "Ver ficha completa";
}

function pintarFicha(d) {
  const x = extras(d);
  const desplazamiento = $("ficha-completa").scrollTop;

  const grupos = [
    seccion("Datos técnicos", [
      ["Distintivo de llamada", d.distintivoLlamada],
      ["Patente de navegación", d.patenteNavegacion],
      ["Tonelaje bruto", d.arqueoBruto, numero],
      ["Tonelaje neto", d.arqueoNeto, numero],
      ["Eslora (m)", d.eslora, numero],
      ["Manga (m)", d.manga, numero],
      ["Puntal (m)", d.puntal, numero],
      ["Inscrita en Panamá", x.inscritaEnPanama],
      ["Ficha", x.ficha],
      ["Observaciones", x.observaciones]
    ]),

    seccion("Autenticación", [
      ["Fecha de celebración", d.fechaCelebracion, fechaDato],
      ["Fecha de aceptación", x.fechaAceptacion, fechaDato],
      ["Persona que autentica", x.autenticaNombre],
      ["Cargo", x.autenticaCargo],
      ["Tipo de legalización", d.tipoLegalizacion],
      ["Autenticación consular o notarial", x.autenticaDetalle]
    ]),

    seccion("Transmitente", [
      ["Tipo", d.tipoTransmitente],
      ["Nombre", d.nombreTransmitente],
      ["Domicilio", d.domicilioTransmitente],
      ["Inscrita en", x.transmitenteInscritaEn],
      ["Ficha o folio real", x.transmitenteFicha],
      ["Representante", x.transmitenteRepresentante],
      ["Cargo", x.transmitenteCargo]
    ]),

    seccion("Propietario", [
      ["Tipo", d.tipoAdquirente],
      ["Nombre", d.propietario],
      ["Domicilio", d.domicilioPropietario],
      ["Inscrita en", x.adquirenteInscritaEn],
      ["Ficha o folio real", x.adquirenteFicha],
      ["Representante", x.adquirenteRepresentante],
      ["Cargo", x.adquirenteCargo]
    ]),

    seccion("Responsables", [
      ["Solicitante", d.solicitante],
      ["Abogado a cargo", d.abogado]
    ]),

    seccion("Datos del trámite", [
      ["Trámite", d.tramite],
      ["Documento base", d.documentoBase],
      ["Presentación", x.presentacion],
      ["Estado", d.estado],
      ["Fecha de creación", d.fechaCreacion, fechaDato],
      ["Liquidación", d.liquidacion],
      ["Fecha de decisión", d.fechaDecision, fechaDato],
      ["Certificado", d.certificadoFolio],
      ["Fecha de emisión", d.fechaEmision, fechaDato],
      ["Vigente hasta", d.fechaVencimiento, fechaDato],
      ["Estado del certificado", d.estadoCertificado],
      ["Fecha de revocación", d.fechaRevocacion, fechaDato],
      ["Motivo de revocación", d.motivoRevocacion]
    ])
  ].filter(Boolean);

  $("cuerpo").replaceChildren(...grupos);

  if (!grupos.length) {
    const p = document.createElement("p");
    p.textContent = "No hay información complementaria registrada.";
    $("cuerpo").append(p);
  }

  $("ficha-completa").scrollTop = desplazamiento;
}

function liberarDocumento() {
  documentoVersion += 1;
  documentoPeticion?.abort();
  documentoPeticion = null;

  $("documento-preview").removeAttribute("src");
  $("documento-preview").hidden = true;

  if (documentoUrl) {
    URL.revokeObjectURL(documentoUrl);
  }

  documentoUrl = null;
}

// La descarga autenticada permite revisar el PDF sin cambiar los headers del servidor.
async function cargarDocumento() {
  if (
    !solicitudActual?.tieneDocumento ||
    documentoUrl ||
    documentoPeticion
  ) {
    return;
  }

  const version = ++documentoVersion;
  const peticion = new AbortController();

  documentoPeticion = peticion;

  $("documento-visor").setAttribute("aria-busy", "true");
  $("documento-mensaje").hidden = false;
  $("documento-mensaje").textContent = "Cargando vista previa…";
  $("reintentar-doc").hidden = true;

  try {
    const r = await fetch("/api/naves/" + id + "/documento", {
      credentials: "same-origin",
      signal: peticion.signal
    });

    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }

    if (
      !r.ok ||
      !r.headers
        .get("Content-Type")
        ?.toLowerCase()
        .includes("application/pdf")
    ) {
      throw new Error("Vista previa no disponible");
    }

    const pdf = await r.blob();

    if (
      version !== documentoVersion ||
      peticion.signal.aborted
    ) {
      return;
    }

    documentoUrl = URL.createObjectURL(pdf);

    $("documento-preview").src =
      documentoUrl + "#navpanes=0&view=FitH";

    $("documento-preview").hidden = false;
    $("documento-mensaje").hidden = true;
  } catch (error) {
    if (
      error.name === "AbortError" ||
      version !== documentoVersion
    ) {
      return;
    }

    $("documento-mensaje").textContent =
      "No se pudo mostrar la vista previa. Puedes abrir el PDF en otra pestaña o reintentar.";

    $("reintentar-doc").hidden = false;
  } finally {
    if (version === documentoVersion) {
      documentoPeticion = null;

      $("documento-visor").setAttribute(
        "aria-busy",
        "false"
      );
    }
  }
}

function pintarDocumento(d) {
  const disponible = Boolean(d.tieneDocumento);

  $("ver-doc").hidden = !disponible;
  $("ver-doc").href = "/api/naves/" + id + "/documento";
  $("documento-ayuda").hidden = !disponible;

  $("documento-preview").title =
    "Documento adjunto de " + valor(d.nombreNave);

  if (!disponible) {
    liberarDocumento();

    $("documento-visor").setAttribute(
      "aria-busy",
      "false"
    );

    $("documento-mensaje").hidden = false;
    $("documento-mensaje").textContent =
      "Esta solicitud no tiene un documento adjunto.";

    $("reintentar-doc").hidden = true;
    return;
  }

  cargarDocumento();
}

function estaRevocado(d) {
  return d.revocado === true ||
    d.estadoCertificado === "Revocado";
}

function pintarCertificado(d) {
  const disponible = hayDato(d.certificadoFolio);

  $("certificado").hidden = !disponible;
  $("ver-cert").hidden = !disponible;
  $("ver-cert").href = "/api/naves/" + id + "/certificado";

  $("cert-folio").textContent = disponible
    ? valor(d.certificadoFolio)
    : "";

  const revocado = estaRevocado(d);
  const estado = $("cert-vigencia");
  const vencimiento = fecha(d.fechaVencimiento);

  let texto = "Vigencia no disponible";
  let clase = "sin-datos";

  if (revocado) {
    texto = "Certificado revocado";
    clase = "revocado";
  } else if (d.estadoCertificado === "Vencido") {
    texto = vencimiento
      ? "Vencido el " + vencimiento
      : "Certificado vencido";

    clase = "vencido";
  } else if (
    d.estadoCertificado === "Vigente" &&
    vencimiento
  ) {
    texto = "Vigente hasta " + vencimiento;
    clase = "vigente";
  }

  estado.textContent = texto;
  estado.className = "revision-vigencia " + clase;

  $("copiar-enlace").hidden =
    !disponible ||
    !hayDato(d.codigoVerificacion) ||
    revocado;

  $("revocacion").hidden = !disponible || revocado;

  if ($("revocacion").hidden) {
    $("revocacion").open = false;
  }
}

function pintarObservaciones(d) {
  const caja = $("obs");
  caja.replaceChildren();

  const revocado = estaRevocado(d);

  if (!hayDato(d.observacion) && !revocado) {
    caja.hidden = true;
    return;
  }

  caja.hidden = false;

  caja.classList.toggle(
    "revision-observacion-rechazo",
    d.estado === "Rechazado" || revocado
  );

  const titulo = document.createElement("h2");
  titulo.id = "obs-titulo";
  titulo.tabIndex = -1;

  titulo.textContent = revocado
    ? "Certificado revocado"
    : "Observación de la revisión";

  caja.append(titulo);

  if (hayDato(d.observacion)) {
    const p = document.createElement("p");
    p.textContent = valor(d.observacion);
    caja.append(p);
  }

  if (revocado) {
    const p = document.createElement("p");

    p.textContent = hayDato(d.motivoRevocacion)
      ? valor(d.motivoRevocacion)
      : "No se registró un motivo de revocación.";

    caja.append(p);

    const dia = fecha(d.fechaRevocacion);

    if (dia) {
      const tiempo = document.createElement("p");
      tiempo.textContent = "Fecha de revocación: " + dia;
      caja.append(tiempo);
    }
  }
}

function pintar(d) {
  solicitudActual = d;

  $("sub").textContent =
    valor(d.tramite) + " · " + valor(d.nombreNave);

  const insignia = document.createElement("span");
  insignia.className = "estado " + (CLASES[d.estado] || "");
  insignia.textContent = valor(d.estado);

  $("estado").replaceChildren(insignia);
  $("nave-nombre").textContent = valor(d.nombreNave);

  const imo = hayDato(d.imo)
    ? String(d.imo).replace(/^IMO\s*/i, "")
    : "No indicado";

  $("nave-meta").textContent =
    "IMO " + imo + " · " + valor(d.tipoNave);

  $("dato-base").textContent = valor(d.documentoBase);
  $("dato-fecha").textContent = fechaDato(d.fechaCelebracion);
  $("dato-transmitente").textContent = valor(d.nombreTransmitente);
  $("dato-propietario").textContent = valor(d.propietario);
  $("dato-solicitante").textContent = valor(d.solicitante);
  $("dato-abogado").textContent = valor(d.abogado);

  $("precio-bloque").hidden = !hayDato(d.precioVenta);
  $("dato-precio").textContent = numero(d.precioVenta);

  pintarFicha(d);
  pintarObservaciones(d);
  pintarCertificado(d);
  pintarDocumento(d);

  $("decision").hidden = d.estado !== "En revisión";

  $("liquidacion").value = hayDato(d.liquidacion)
    ? String(d.liquidacion)
    : "";

  $("observacion").value = hayDato(d.observacion)
    ? String(d.observacion)
    : "";
}

async function cargar() {
  if (!idValido || cargandoSolicitud) return;

  cargandoSolicitud = true;

  $("revision-carga").hidden = false;
  $("revision-error").hidden = true;

  let mensajeError = "No se pudo conectar con el servidor.";

  try {
    const r = await fetch("/api/naves/" + id);

    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }

    mensajeError = "No se pudo cargar la solicitud.";

    if (!r.ok) {
      mensajeError = r.status === 404
        ? "Solicitud no encontrada."
        : r.status === 403
          ? "No tienes permiso para consultar esta solicitud."
          : mensajeError;

      throw new Error();
    }

    const d = await r.json();

    if (
      !d ||
      typeof d !== "object" ||
      Array.isArray(d)
    ) {
      throw new Error("No se pudo cargar la solicitud.");
    }

    pintar(d);
    $("revision-panel").hidden = false;
  } catch {
    $("revision-error-texto").textContent = mensajeError;
    $("revision-error").hidden = false;
    $("reintentar-carga").hidden = false;
  } finally {
    cargandoSolicitud = false;
    $("revision-carga").hidden = true;
  }
}

function bloquearAcciones(bloqueadas) {
  operacionEnCurso = bloqueadas;

  ["aprobar", "rechazar", "revocar"].forEach((nombre) => {
    $(nombre).disabled = bloqueadas;
  });

  $("decision").setAttribute(
    "aria-busy",
    String(bloqueadas)
  );

  $("revocacion").setAttribute(
    "aria-busy",
    String(bloqueadas)
  );
}

function errorCampo(nombre, mensaje, errorId) {
  $(errorId).textContent = mensaje;

  $(nombre).setAttribute(
    "aria-invalid",
    String(Boolean(mensaje))
  );

  $(nombre)
    .closest(".campo")
    .classList.toggle("campo-error", Boolean(mensaje));

  if (mensaje) {
    $(nombre).focus();
  }
}

async function decidir(estado) {
  if (
    operacionEnCurso ||
    solicitudActual?.estado !== "En revisión"
  ) {
    return;
  }

  aviso("");
  errorCampo("observacion", "", "err-observacion");

  const observacion = $("observacion").value.trim();
  const liquidacion = $("liquidacion").value.trim();

  if (estado === "Rechazado" && !observacion) {
    errorCampo(
      "observacion",
      "Indique la observación del rechazo.",
      "err-observacion"
    );
    return;
  }

  if (observacion.length > 1000) {
    errorCampo(
      "observacion",
      "La observación admite hasta 1000 caracteres.",
      "err-observacion"
    );
    return;
  }

  if (liquidacion.length > 40) {
    aviso("El número de liquidación admite hasta 40 caracteres.");
    $("liquidacion").focus();
    return;
  }

  if (!confirm("La decisión no se puede cambiar. ¿Continuar?")) {
    return;
  }

  bloquearAcciones(true);

  try {
    const r = await fetch("/api/naves/" + id + "/estado", {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        estado,
        observacion,
        liquidacion
      })
    });

    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }

    const json = await r.json().catch(() => ({}));

    if (r.ok) {
      pintar(json);

      aviso(
        estado === "Aprobado"
          ? "Solicitud aprobada."
          : "Solicitud rechazada.",
        true
      );

      const destino =
        estado === "Aprobado" && !$("certificado").hidden
          ? $("titulo-certificado")
          : $("obs-titulo") || $("titulo-revision");

      destino.focus();
    } else {
      aviso(
        json.mensaje ||
        "No se pudo registrar la decisión."
      );
    }
  } catch {
    aviso("No se pudo conectar con el servidor.");
  } finally {
    bloquearAcciones(false);
  }
}

async function revocar() {
  if (
    operacionEnCurso ||
    !hayDato(solicitudActual?.certificadoFolio) ||
    estaRevocado(solicitudActual)
  ) {
    return;
  }

  aviso("");
  errorCampo("motivo-revocacion", "", "err-motivo");

  const motivo = $("motivo-revocacion").value.trim();

  if (!motivo || motivo.length > 500) {
    errorCampo(
      "motivo-revocacion",
      motivo
        ? "El motivo admite hasta 500 caracteres."
        : "Indique el motivo de la revocación.",
      "err-motivo"
    );
    return;
  }

  if (
    !confirm(
      "El certificado dejará de ser válido y no se puede deshacer. ¿Continuar?"
    )
  ) {
    return;
  }

  bloquearAcciones(true);

  try {
    const r = await fetch("/api/naves/" + id + "/revocar", {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ motivo })
    });

    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }

    const json = await r.json().catch(() => ({}));

    if (r.ok) {
      $("motivo-revocacion").value = "";

      pintar(json);
      aviso("Certificado revocado.", true);
      $("obs-titulo").focus();
    } else {
      aviso(
        json.mensaje ||
        "No se pudo revocar el certificado."
      );
    }
  } catch {
    aviso("No se pudo conectar con el servidor.");
  } finally {
    bloquearAcciones(false);
  }
}

async function copiarEnlace() {
  if (
    !hayDato(solicitudActual?.codigoVerificacion) ||
    estaRevocado(solicitudActual)
  ) {
    return;
  }

  const enlace = new URL(
    "/verificar.html",
    location.origin
  );

  enlace.searchParams.set(
    "codigo",
    solicitudActual.codigoVerificacion
  );

  try {
    await navigator.clipboard.writeText(enlace.href);
    aviso("Enlace de verificación copiado.", true);
  } catch {
    aviso(
      "No se pudo copiar automáticamente. Enlace de verificación: " +
      enlace.href
    );
  }
}

$("ficha-toggle").addEventListener("click", () => {
  mostrarFicha($("ficha-completa").hidden);
});

$("ficha-completa").addEventListener("keydown", (evento) => {
  if (evento.key === "Escape") {
    evento.preventDefault();
    mostrarFicha(false, true);
  }
});

$("reintentar-doc").addEventListener("click", cargarDocumento);
$("reintentar-carga").addEventListener("click", cargar);

$("aprobar").addEventListener("click", () => {
  decidir("Aprobado");
});

$("rechazar").addEventListener("click", () => {
  decidir("Rechazado");
});

$("revocar").addEventListener("click", revocar);
$("copiar-enlace").addEventListener("click", copiarEnlace);

window.addEventListener("pagehide", (evento) => {
  if (!evento.persisted) {
    liberarDocumento();
  }
});

if (idValido) {
  cargar();
} else {
  location.replace("bandeja.html");
}