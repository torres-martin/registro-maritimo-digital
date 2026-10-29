const $ = (id) => document.getElementById(id);
const CLASES = { "En revisión": "en-revision", "Aprobado": "aprobado", "Rechazado": "rechazado" };
let naves = [];
let vencimientos = [];
let cargandoVencimientos = false;

// Todo se escribe con textContent (nunca innerHTML) para evitar XSS
function celda(texto) {
  const td = document.createElement("td");
  td.textContent = texto || "—";
  return td;
}

function insignia(estado) {
  const s = document.createElement("span");
  s.className = "estado " + (CLASES[estado] || "");
  s.textContent = estado;
  return s;
}

function filaMensaje(texto) {
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 7;
  td.className = "vacio";
  td.textContent = texto;
  tr.appendChild(td);
  return tr;
}

function aviso(texto, ok) {
  const caja = $("aviso");
  caja.className = texto ? (ok ? "aviso ok" : "aviso mal") : "aviso";
  caja.textContent = texto;
}

// ---------- lista ----------

function pintarResumen() {
  $("resumen-total").textContent = naves.length;

  $("resumen-revision").textContent =
    naves.filter((n) => n.estado === "En revisión").length;

  $("resumen-aprobado").textContent =
    naves.filter((n) => n.estado === "Aprobado").length;

  $("resumen-rechazado").textContent =
    naves.filter((n) => n.estado === "Rechazado").length;
}

function pintarTabla() {
  const texto = $("buscar").value.trim().toLowerCase();
  const estado = $("filtro-estado").value;
  const cuerpo = $("tabla-body");

  const visibles = naves.filter((n) => {
    const coincide = [n.tramite, n.nombreNave, n.imo]
      .join(" ")
      .toLowerCase()
      .includes(texto);

    return coincide && (!estado || n.estado === estado);
  });

  $("lista-cantidad").textContent =
    visibles.length +
    (visibles.length === 1 ? " solicitud" : " solicitudes");

  cuerpo.replaceChildren();

  if (visibles.length === 0) {
    cuerpo.append(
      filaMensaje(
        naves.length === 0
          ? "Aún no has enviado solicitudes."
          : "Ninguna solicitud coincide con la búsqueda."
      )
    );

    return;
  }

  visibles.forEach((n) => {
    const tr = document.createElement("tr");

    const tdCodigo = document.createElement("td");
    const codigo = document.createElement("span");

    codigo.className = "codigo";
    codigo.textContent = n.tramite;
    tdCodigo.appendChild(codigo);

    const tdEstado = document.createElement("td");
    tdEstado.appendChild(insignia(n.estado));

    const tdVer = document.createElement("td");
    tdVer.className = "solicitud-acciones";

    const ver = document.createElement("button");
    ver.type = "button";
    ver.className = "btn o x";
    ver.textContent = "Ver";
    ver.addEventListener("click", () => abrir(n.id));
    tdVer.appendChild(ver);

    // Las solicitudes aprobadas permiten abrir el certificado.
    if (n.estado === "Aprobado") {
      const cert = document.createElement("a");

      cert.className = "btn o x";
      cert.style.marginLeft = "6px";
      cert.href =
        "/api/naves/" + encodeURIComponent(n.id) + "/certificado";
      cert.target = "_blank";
      cert.rel = "noopener";
      cert.textContent = "Certificado";

      tdVer.appendChild(cert);
    }

    // Las solicitudes rechazadas permiten precargar el formulario de reenvío.
    if (n.estado === "Rechazado") {
      const corregir = document.createElement("a");

      corregir.className = "btn o x";
      corregir.style.marginLeft = "6px";
      corregir.href =
        "nueva-solicitud.html?corregir=" + encodeURIComponent(n.id);
      corregir.textContent = "Corregir y reenviar";

      tdVer.appendChild(corregir);
    }

    tr.append(
      tdCodigo,
      celda(n.nombreNave),
      celda(n.imo),
      celda(n.documentoBase),
      tdEstado,
      celda(n.fechaCreacion),
      tdVer
    );

    cuerpo.append(tr);
  });
}

async function cargar() {
  try {
    const r = await fetch("/api/naves/mias");

    if (!r.ok) {
      throw new Error();
    }

    naves = await r.json();

    pintarResumen();
    pintarTabla();
  } catch {
    $("lista-cantidad").textContent = "Lista no disponible";

    $("tabla-body").replaceChildren(
      filaMensaje(
        "No se pudo cargar la lista. Recarga la página para intentarlo de nuevo."
      )
    );
  }
}

// ---------- avisos de vencimiento ----------

function mostrarVencimientos(abiertos) {
  const lista = $("vencimientos-lista");
  if (!abiertos && lista.contains(document.activeElement)) {
    $("vencimientos-toggle").focus();
  }
  lista.hidden = !abiertos;
  $("vencimientos-toggle").setAttribute("aria-expanded", String(abiertos));
  $("vencimientos-toggle-texto").textContent = abiertos ? "Ocultar avisos" : "Ver avisos";
}

function pintarVencimientos() {
  const lista = $("vencimientos-lista");
  lista.replaceChildren();
  $("vencimientos").dataset.estado = vencimientos.length ? "listo" : "vacio";
  $("vencimientos-cantidad").textContent = vencimientos.length;
  $("vencimientos-cantidad").hidden = false;
  $("vencimientos-toggle").hidden = !vencimientos.length;
  $("vencimientos-resumen").textContent = vencimientos.length
    ? (vencimientos.length === 1 ? "1 certificado vence" : vencimientos.length + " certificados vencen") + " en los próximos 30 días."
    : "No tienes certificados que venzan en los próximos 30 días.";

  vencimientos.forEach((n) => {
    const fila = nodo("li", "vencimiento");
    const identidad = nodo("div", "vencimiento-identidad");
    identidad.append(
      nodo("strong", "", valorConsulta(n.nombreNave)),
      nodo("span", "", valorConsulta(n.certificadoFolio) + " · " + valorConsulta(n.tramite))
    );
    const vencimiento = nodo("div", "vencimiento-fecha");
    const dia = document.createElement("time");
    dia.dateTime = n.fechaVencimiento;
    dia.textContent = fecha(n.fechaVencimiento);
    vencimiento.append(nodo("span", "", "Vigente hasta"), dia);

    const dias = n.diasRestantes;
    const plazo = nodo("span", "vencimiento-plazo",
      dias === 0 ? "Vence hoy" : dias === 1 ? "En 1 día" : "En " + dias + " días");
    plazo.dataset.urgencia = dias === 0 ? "hoy" : dias <= 7 ? "proximo" : "normal";

    const ver = nodo("button", "btn o x vencimiento-ver", "Ver expediente");
    ver.type = "button";
    ver.dataset.solicitud = String(n.id);
    ver.setAttribute("aria-label", "Ver expediente de " + valorConsulta(n.nombreNave));
    ver.addEventListener("click", () => abrir(n.id));
    fila.append(identidad, vencimiento, plazo, ver);
    lista.append(fila);
  });
  if (!vencimientos.length) mostrarVencimientos(false);
}

async function cargarVencimientos() {
  if (cargandoVencimientos) return;
  const devolverFoco = document.activeElement === $("vencimientos-reintentar");
  if (devolverFoco) $("vencimientos-titulo").focus({ preventScroll: true });
  cargandoVencimientos = true;
  $("vencimientos").dataset.estado = "cargando";
  $("vencimientos").setAttribute("aria-busy", "true");
  $("vencimientos-cantidad").hidden = true;
  $("vencimientos-toggle").hidden = true;
  $("vencimientos-reintentar").hidden = true;
  $("vencimientos-resumen").textContent = "Consultando tus certificados…";
  mostrarVencimientos(false);
  try {
    const r = await fetch("/api/naves/mias/vencimientos");
    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }
    if (!r.ok) throw new Error();
    const datos = await r.json();
    if (!Array.isArray(datos) || datos.some((n) =>
      !n || !/^\d+$/.test(String(n.id)) || !hayDato(n.certificadoFolio)
      || !fecha(n.fechaVencimiento) || !Number.isInteger(n.diasRestantes)
      || n.diasRestantes < 0 || n.diasRestantes > 30)) {
      throw new Error();
    }
    vencimientos = datos;
    pintarVencimientos();
    if (devolverFoco) {
      (vencimientos.length ? $("vencimientos-toggle") : $("vencimientos-titulo")).focus({ preventScroll: true });
    }
  } catch {
    $("vencimientos").dataset.estado = "error";
    $("vencimientos-lista").replaceChildren();
    $("vencimientos-resumen").textContent = "No se pudieron consultar los próximos vencimientos. Puedes reintentar.";
    $("vencimientos-reintentar").hidden = false;
    if (devolverFoco) $("vencimientos-reintentar").focus({ preventScroll: true });
  } finally {
    cargandoVencimientos = false;
    $("vencimientos").setAttribute("aria-busy", "false");
  }
}

// ---------- detalle ----------

function extras(d) {
  try {
    const datos = d.adicionales ? JSON.parse(d.adicionales) : {};
    return datos && typeof datos === "object" && !Array.isArray(datos) ? datos : {};
  } catch {
    return {};
  }
}

function hayDato(v) {
  return v !== null && v !== undefined && String(v).trim() !== "";
}

function valorConsulta(v) {
  return hayDato(v) ? (typeof v === "boolean" ? (v ? "Sí" : "No") : String(v)) : "No indicado";
}

function numeroConsulta(v) {
  return hayDato(v) && Number.isFinite(Number(v))
    ? new Intl.NumberFormat("es").format(Number(v)) : valorConsulta(v);
}

function grupoConsulta(titulo, filas) {
  const validas = filas.filter(([, v]) => hayDato(v));
  if (validas.length === 0) return null;
  const s = document.createElement("section");
  s.className = "consulta-grupo";
  const h = document.createElement("h5");
  h.textContent = titulo;
  const dl = document.createElement("dl");
  dl.className = "consulta-kv";
  validas.forEach(([k, v, formato]) => {
    const dato = document.createElement("div");
    dato.className = "consulta-dato";
    const dt = document.createElement("dt");
    dt.textContent = k;
    const dd = document.createElement("dd");
    dd.textContent = formato ? formato(v) : valorConsulta(v);
    dato.append(dt, dd);
    dl.append(dato);
  });
  s.append(h, dl);
  return s;
}

let codigoActual = null;

let botonOrigen = null;

function fecha(iso) {
  const valor = String(iso || "").trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(valor)) return "";
  const dia = new Date(valor + "T00:00:00Z");
  if (Number.isNaN(dia.getTime()) || dia.toISOString().slice(0, 10) !== valor) return "";
  return new Intl.DateTimeFormat("es", {
    day: "2-digit", month: "short", year: "numeric", timeZone: "UTC"
  }).format(dia);
}

const TRAZO_OK = "m6 12 4 4 8-9";
const TRAZO_X = "M7 7l10 10M17 7 7 17";
const TRAZO_RELOJ = "M12 7v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0";
const TRAZO_DOCUMENTO = "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M8 13h8M8 17h5";
const TRAZO_AVISO = "M12 3 2 21h20zM12 9v4M12 17h.01";
const TRAZO_CERTIFICADO = "M14 2H5v20h6M14 2v6h6v4M5 2h9l6 6M8 12h5M8 16h3M22 16a4 4 0 1 1-8 0 4 4 0 0 1 8 0M16 20v3l2-1 2 1v-3";

function icono(trazo) {
  const ns = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(ns, "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("fill", "none");
  svg.setAttribute("stroke", "currentColor");
  svg.setAttribute("stroke-width", "2");
  svg.setAttribute("stroke-linecap", "round");
  svg.setAttribute("stroke-linejoin", "round");
  svg.setAttribute("aria-hidden", "true");
  const path = document.createElementNS(ns, "path");
  path.setAttribute("d", trazo);
  svg.append(path);
  return svg;
}

function nodo(etiqueta, clase, texto) {
  const el = document.createElement(etiqueta);
  if (clase) el.className = clase;
  if (texto !== undefined) el.textContent = texto;
  return el;
}

// La vigencia se toma del servidor; no modifica la resolución de la solicitud.
function vigenciaCertificado(d) {
  const vencimiento = fecha(d.fechaVencimiento);
  if (d.revocado || d.estadoCertificado === "Revocado") {
    return { tipo: "revocado", texto: "Certificado revocado" };
  }
  if (d.estadoCertificado === "Vencido") {
    return { tipo: "vencido", texto: "Vencido" + (vencimiento ? " · Venció el " + vencimiento : "") };
  }
  if (d.estadoCertificado === "Vigente") {
    return { tipo: "vigente", texto: "Vigente" + (vencimiento ? " hasta " + vencimiento : "") };
  }
  return { tipo: "sin-datos", texto: "Vigencia no informada" };
}

function pintarEncabezado(d) {
  const emitido = Boolean(d.certificadoFolio);
  const rechazado = d.estado === "Rechazado";
  const vigencia = vigenciaCertificado(d);
  $("det-portada").dataset.certificado = String(emitido);
  $("det-titulo").textContent = emitido ? "Certificado de registro"
    : d.estado === "En revisión" ? "Solicitud en revisión"
    : rechazado ? "Solicitud rechazada"
    : d.estado === "Aprobado" ? "Solicitud aprobada" : "Detalle de la solicitud";
  $("det-icono").replaceChildren(icono(emitido ? TRAZO_DOCUMENTO : rechazado ? TRAZO_AVISO : TRAZO_RELOJ));
  $("det-folio").textContent = d.certificadoFolio || "";
  $("det-folio").hidden = !emitido;
  $("det-nave").textContent = valorConsulta(d.nombreNave);
  $("det-tramite").textContent = valorConsulta(d.tramite);
  const estado = insignia(d.estado || "Estado no informado");
  estado.textContent = d.estado === "Aprobado" ? "Solicitud aprobada"
    : rechazado ? "Solicitud rechazada" : d.estado || "Estado no informado";
  $("det-estado").replaceChildren(estado);
  $("det-vigencia").hidden = !emitido;
  $("det-vigencia").textContent = emitido ? vigencia.texto : "";
  $("det-vigencia").dataset.estado = vigencia.tipo;

  const obs = $("det-obs");
  obs.replaceChildren();
  function observacion(titulo, texto) {
    const caja = nodo("div", "obs");
    caja.append(nodo("strong", "", titulo), nodo("p", "", texto));
    obs.append(caja);
  }
  if (d.observacion) observacion("Observación de la resolución", d.observacion);
  if (emitido && vigencia.tipo === "revocado") {
    const revocacion = fecha(d.fechaRevocacion);
    observacion("Certificado revocado", [
      revocacion ? "Fecha: " + revocacion + "." : "",
      d.motivoRevocacion || "Este certificado ya no es válido."
    ].filter(Boolean).join(" "));
  }
}

// Las etapas muestran recepción, revisión, resolución y emisión; solo se fechan los datos registrados.
function pintarHistorial(d) {
  const aprobado = d.estado === "Aprobado";
  const rechazado = d.estado === "Rechazado";
  const enRevision = d.estado === "En revisión";
  const emitido = Boolean(d.certificadoFolio);
  const resuelto = aprobado || rechazado;
  const pasos = [
    { titulo: "Recibida", nota: fecha(d.fechaCreacion) ? "" : "Fecha no registrada",
      fecha: d.fechaCreacion, estado: "hecho" },
    { titulo: "Revisión", nota: resuelto ? "Finalizada" : enRevision ? "En curso" : "Sin información",
      estado: resuelto ? "hecho" : enRevision ? "actual" : "pendiente" },
    { titulo: "Resolución", nota: aprobado ? "Aprobada" : rechazado ? "Rechazada" : "Pendiente",
      fecha: resuelto ? d.fechaDecision : null,
      estado: rechazado ? "rechazado" : aprobado ? (emitido ? "hecho" : "actual") : "pendiente" },
    { titulo: emitido ? "Certificado emitido" : "Certificado", nota: emitido ? "" : rechazado ? "No emitido" : aprobado ? "No disponible" : "Si se aprueba",
      fecha: emitido ? d.fechaEmision : null,
      estado: emitido ? "actual" : rechazado ? "no-aplica" : "pendiente" }
  ];
  const actual = rechazado ? 2 : emitido ? 3 : enRevision ? 1 : aprobado ? 2 : -1;
  const cont = $("det-historial");
  cont.replaceChildren();
  const titulo = nodo("h3", "recorrido-titulo", "Recorrido del trámite");
  titulo.id = "recorrido-titulo";
  const lista = nodo("ol", "recorrido-lista");
  lista.setAttribute("aria-label", "Etapas de la solicitud");

  pasos.forEach((paso, i) => {
    const fila = nodo("li", "recorrido-paso " + paso.estado);
    if (i === actual) fila.setAttribute("aria-current", "step");
    const simbolo = nodo("span", "recorrido-icono");
    simbolo.setAttribute("aria-hidden", "true");
    simbolo.append(icono(paso.estado === "hecho" ? TRAZO_OK
      : paso.estado === "rechazado" ? TRAZO_X
      : i === 1 ? TRAZO_RELOJ : i === 3 ? TRAZO_CERTIFICADO : TRAZO_DOCUMENTO));
    const texto = nodo("div", "recorrido-texto");
    texto.append(nodo("strong", "recorrido-nombre", paso.titulo));
    if (paso.nota) texto.append(nodo("small", "recorrido-nota", paso.nota));
    const registrada = fecha(paso.fecha);
    if (registrada) {
      const dia = nodo("time", "recorrido-fecha", registrada);
      dia.dateTime = String(paso.fecha).trim();
      texto.append(dia);
    }
    if (i === 3 && emitido) {
      const vigencia = vigenciaCertificado(d).tipo;
      texto.append(nodo("span", "recorrido-disponible " + vigencia,
        vigencia === "vigente" ? "Disponible" : vigencia === "vencido" ? "Vencido"
          : vigencia === "revocado" ? "Revocado" : "Consultar PDF"));
    }
    fila.append(simbolo, texto);
    lista.append(fila);
  });
  cont.append(titulo, lista);
  if (enRevision || (aprobado && !emitido)) {
    cont.append(nodo("p", "recorrido-ayuda", enRevision
      ? "El certificado estará disponible si se aprueba la solicitud."
      : "La solicitud fue aprobada, pero el certificado aún no está disponible."));
  }
}

function pintarDetalle(d) {
  const x = extras(d);
  const fechaDato = (v) => fecha(v) || "No indicada";
  $("consulta-nave").textContent = valorConsulta(d.nombreNave);
  $("consulta-imo").textContent = hayDato(d.imo)
    ? String(d.imo).trim().replace(/^IMO\s*/i, "") : "No indicado";
  $("consulta-tipo").textContent = valorConsulta(d.tipoNave);
  $("consulta-base").textContent = valorConsulta(d.documentoBase);
  $("consulta-fecha").textContent = fechaDato(d.fechaCelebracion);
  $("consulta-transmitente").textContent = valorConsulta(d.nombreTransmitente);
  $("consulta-propietario").textContent = valorConsulta(d.propietario);
  $("consulta-precio-bloque").hidden = !hayDato(d.precioVenta);
  $("consulta-precio").textContent = hayDato(d.precioVenta) && Number.isFinite(Number(d.precioVenta))
    ? new Intl.NumberFormat("es", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(d.precioVenta))
    : valorConsulta(d.precioVenta);
  $("consulta-documento-texto").textContent = d.tieneDocumento
    ? "Documento adjunto (PDF)" : "Sin documento adjunto";

  const grupos = [
    grupoConsulta("Datos técnicos", [
      ["Distintivo", d.distintivoLlamada], ["Patente", d.patenteNavegacion],
      ["Tonelaje bruto", d.arqueoBruto, numeroConsulta], ["Tonelaje neto", d.arqueoNeto, numeroConsulta],
      ["Eslora (m)", d.eslora, numeroConsulta], ["Manga (m)", d.manga, numeroConsulta],
      ["Puntal (m)", d.puntal, numeroConsulta], ["Inscrita en Panamá", x.inscritaEnPanama],
      ["Ficha de la nave", x.ficha], ["Observaciones", x.observaciones]
    ]),
    grupoConsulta("Responsables", [
      ["Solicitante", d.solicitante], ["Abogado", d.abogado]
    ]),
    grupoConsulta("Autenticación", [
      ["Fecha de aceptación", x.fechaAceptacion, fechaDato],
      ["Persona que autentica", x.autenticaNombre], ["Cargo", x.autenticaCargo],
      ["Legalización", d.tipoLegalizacion], ["Autenticación consular o notarial", x.autenticaDetalle]
    ]),
    grupoConsulta("Transmitente", [
      ["Tipo", d.tipoTransmitente], ["Domicilio", d.domicilioTransmitente],
      ["Inscrita en", x.transmitenteInscritaEn], ["Ficha o folio real", x.transmitenteFicha],
      ["Representante", x.transmitenteRepresentante], ["Cargo", x.transmitenteCargo]
    ]),
    grupoConsulta("Propietario", [
      ["Tipo", d.tipoAdquirente], ["Domicilio", d.domicilioPropietario],
      ["Inscrita en", x.adquirenteInscritaEn], ["Ficha o folio real", x.adquirenteFicha],
      ["Representante", x.adquirenteRepresentante], ["Cargo", x.adquirenteCargo]
    ]),
    grupoConsulta("Datos del trámite", [
      ["Trámite", d.tramite], ["Presentación", x.presentacion],
      ["Fecha de creación", d.fechaCreacion, fechaDato],
      ["Fecha de resolución", d.fechaDecision, fechaDato], ["Liquidación", d.liquidacion],
      ["Certificado", d.certificadoFolio], ["Fecha de emisión", d.fechaEmision, fechaDato],
      ["Vigente hasta", d.fechaVencimiento, fechaDato], ["Estado del certificado", d.estadoCertificado],
      ["Fecha de revocación", d.fechaRevocacion, fechaDato], ["Motivo de revocación", d.motivoRevocacion]
    ])
  ].filter(Boolean);
  $("consulta-grupos").replaceChildren(...grupos);
  if (!grupos.length) {
    $("consulta-grupos").append(nodo("p", "consulta-sin-datos", "No hay información complementaria registrada."));
  }
  mostrarFichaConsulta(false);
  $("consulta-ficha").scrollTop = 0;
}

function mostrarFichaConsulta(abierta, devolverFoco = false) {
  const ficha = $("consulta-ficha");
  const control = $("consulta-toggle");
  const enfocar = devolverFoco || (!abierta && ficha.contains(document.activeElement));
  ficha.hidden = !abierta;
  $("consulta-expediente").dataset.fichaAbierta = String(abierta);
  control.setAttribute("aria-expanded", String(abierta));
  $("consulta-toggle-texto").textContent = abierta ? "Ocultar ficha" : "Ver ficha completa";
  if (enfocar) control.focus({ preventScroll: true });
}

function mostrarVistaDetalle(abierta) {
  $("vista-lista").hidden = abierta;
  $("vista-detalle").hidden = !abierta;
  document.querySelector(".solicitudes-resumen").hidden = abierta;
  document.body.classList.toggle("mostrando-detalle", abierta);
  const destino = abierta ? $("det-titulo")
    : botonOrigen?.isConnected ? botonOrigen : $("buscar");
  destino.focus({ preventScroll: abierta });
}

async function abrir(id) {
  aviso("");
  const origen = document.activeElement;
  try {
    const r = await fetch("/api/naves/" + id);
    if (!r.ok) throw new Error();
    const d = await r.json();

    pintarEncabezado(d);
    const ruta = "/api/naves/" + encodeURIComponent(id);

    const doc = $("ver-doc");
    doc.hidden = !d.tieneDocumento;
    doc.href = ruta + "/documento";

    const cert = $("ver-cert");
    cert.hidden = !d.certificadoFolio;
    cert.href = ruta + "/certificado";

    const cor = $("corregir");
    cor.hidden = d.estado !== "Rechazado";
    cor.href = "nueva-solicitud.html?corregir=" + encodeURIComponent(id);

    // El enlace se ofrece cuando existe un código y el certificado no está revocado.
    codigoActual = d.certificadoFolio && vigenciaCertificado(d).tipo !== "revocado"
      ? d.codigoVerificacion : null;
    $("copiar-enlace").hidden = !codigoActual;

    pintarHistorial(d);
    pintarDetalle(d);
    botonOrigen = origen?.matches("#tabla-body button, .vencimiento-ver") ? origen : null;
    mostrarVistaDetalle(true);
    window.scrollTo(0, 0);
  } catch {
    aviso("No se pudo cargar la solicitud.");
  }
}

$("consulta-toggle").addEventListener("click", () => mostrarFichaConsulta($("consulta-ficha").hidden));
$("consulta-cerrar").addEventListener("click", () => mostrarFichaConsulta(false, true));
$("consulta-ficha").addEventListener("keydown", (evento) => {
  if (evento.key === "Escape") {
    evento.preventDefault();
    mostrarFichaConsulta(false, true);
  }
});

$("volver").addEventListener("click", () => {
  mostrarVistaDetalle(false);
});
$("buscar").addEventListener("input", pintarTabla);
$("filtro-estado").addEventListener("change", pintarTabla);
$("vencimientos-toggle").addEventListener("click", () => mostrarVencimientos($("vencimientos-lista").hidden));
$("vencimientos-reintentar").addEventListener("click", cargarVencimientos);
$("vencimientos-lista").addEventListener("keydown", (evento) => {
  if (evento.key === "Escape") {
    evento.preventDefault();
    mostrarVencimientos(false);
  }
});

$("copiar-enlace").addEventListener("click", async () => {
  if (!codigoActual) return;
  const enlace = location.origin + "/verificar.html?codigo=" + encodeURIComponent(codigoActual);
  try {
    await navigator.clipboard.writeText(enlace);
    aviso("Enlace de verificación copiado.", true);
  } catch {
    window.prompt("Copie el enlace de verificación:", enlace);
  }
});

cargar();
cargarVencimientos();

;(function () {
  const $b = (id) => document.getElementById(id);
  const panel = $b("borradores");
  if (!panel) return;
  let borradores = [];
  let cargando = false;
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const formatoFecha = new Intl.DateTimeFormat("es-PA", { dateStyle: "medium", timeStyle: "short" });

  function nodo(etiqueta, clase, texto) {
    const n = document.createElement(etiqueta);
    if (clase) n.className = clase;
    if (texto !== undefined) n.textContent = texto;
    return n;
  }

  function desplegar(abierto) {
    const lista = $b("borradores-lista");
    if (!abierto && lista.contains(document.activeElement)) $b("borradores-toggle").focus();
    lista.hidden = !abierto;
    $b("borradores-toggle").setAttribute("aria-expanded", String(abierto));
    $b("borradores-toggle-texto").textContent = abierto ? "Ocultar borradores" : "Ver borradores";
  }

  function pintar() {
    const lista = $b("borradores-lista");
    lista.replaceChildren();
    panel.hidden = !borradores.length;
    panel.dataset.estado = borradores.length ? "listo" : "vacio";
    $b("borradores-cantidad").textContent = borradores.length;
    $b("borradores-cantidad").hidden = false;
    $b("borradores-toggle").hidden = !borradores.length;
    $b("borradores-estado").textContent = borradores.length === 1
      ? "Tienes 1 solicitud guardada sin enviar."
      : "Tienes " + borradores.length + " solicitudes guardadas sin enviar.";

    borradores.forEach((b) => {
      const fila = nodo("li", "borrador-fila");
      const identidad = nodo("div", "borrador-identidad");
      identidad.append(
        nodo("strong", "", String(b.nombreNave || "").trim() || "Solicitud sin nombre"),
        nodo("span", "borrador-meta", "Paso " + b.paso + " de 3 · " + (b.tieneDocumento ? "Con PDF" : "Sin PDF"))
      );
      const fecha = nodo("time", "borrador-fecha", formatoFecha.format(new Date(b.actualizado)));
      fecha.dateTime = b.actualizado;
      fecha.setAttribute("aria-label", "Último guardado: " + fecha.textContent);
      const acciones = nodo("div", "borrador-fila-acciones");
      const continuar = nodo("a", "btn o x borrador-continuar", "Continuar");
      continuar.href = "nueva-solicitud.html?borrador=" + encodeURIComponent(b.id);
      continuar.dataset.borrador = b.id;
      continuar.setAttribute("aria-label", "Continuar borrador de " + (String(b.nombreNave || "").trim() || "solicitud sin nombre"));
      const eliminar = nodo("button", "btn o x borrador-eliminar", "Eliminar");
      eliminar.type = "button";
      eliminar.setAttribute("aria-label", "Eliminar borrador de " + (String(b.nombreNave || "").trim() || "solicitud sin nombre"));
      eliminar.addEventListener("click", () => borrar(b, fila));
      acciones.append(continuar, eliminar);
      fila.append(identidad, fecha, acciones);
      lista.append(fila);
    });
    if (!borradores.length) desplegar(false);
  }

  async function cargar() {
    if (cargando) return;
    cargando = true;
    const devolverFoco = document.activeElement === $b("borradores-reintentar");
    if (devolverFoco) $b("borradores-titulo").focus();
    panel.dataset.estado = "cargando";
    panel.setAttribute("aria-busy", "true");
    $b("borradores-reintentar").hidden = true;
    $b("borradores-toggle").hidden = true;
    $b("borradores-cantidad").hidden = true;
    $b("borradores-estado").textContent = "Consultando tus borradores…";
    desplegar(false);
    try {
      const r = await fetch("/api/borradores");
      if (r.status === 401) { location.href = "/login.html"; return; }
      if (!r.ok) throw new Error();
      const datos = await r.json();
      if (!Array.isArray(datos) || datos.some((b) => !b || !uuid.test(b.id)
        || !Number.isInteger(b.version) || b.version < 0 || !Number.isInteger(b.paso)
        || b.paso < 1 || b.paso > 3 || !Number.isFinite(Date.parse(b.actualizado)))) throw new Error();
      borradores = datos;
      pintar();
      if (devolverFoco) (borradores.length ? $b("borradores-toggle") : $b("buscar")).focus();
    } catch {
      panel.hidden = false;
      panel.dataset.estado = "error";
      $b("borradores-estado").textContent = "No se pudieron consultar tus borradores. Puedes reintentar.";
      $b("borradores-reintentar").hidden = false;
      if (devolverFoco) $b("borradores-reintentar").focus();
    } finally {
      cargando = false;
      panel.setAttribute("aria-busy", "false");
    }
  }

  async function borrar(borrador, fila) {
    if (fila.inert || !confirm("¿Eliminar este borrador y su PDF adjunto?")) return;
    const indice = borradores.findIndex((b) => b.id === borrador.id);
    fila.inert = true;
    try {
      const r = await fetch("/api/borradores/" + encodeURIComponent(borrador.id) + "?version=" + borrador.version, { method: "DELETE" });
      if (r.status === 401) { location.href = "/login.html"; return; }
      if (!r.ok) {
        const error = await r.json().catch(() => ({}));
        $b("borradores-estado").textContent = error.mensaje || "No se pudo eliminar el borrador.";
        return;
      }
      borradores = borradores.filter((b) => b.id !== borrador.id);
      pintar();
      $b("borradores-estado").textContent = "Borrador eliminado.";
      const siguiente = $b("borradores-lista").querySelectorAll(".borrador-continuar");
      (siguiente[Math.min(indice, siguiente.length - 1)] || $b("buscar")).focus();
    } catch {
      $b("borradores-estado").textContent = "No se pudo conectar con el servidor. El borrador sigue en la lista.";
    } finally {
      fila.inert = false;
    }
  }

  $b("borradores-toggle").addEventListener("click", () => desplegar($b("borradores-lista").hidden));
  $b("borradores-reintentar").addEventListener("click", cargar);
  $b("borradores-lista").addEventListener("keydown", (e) => {
    if (e.key === "Escape") { e.preventDefault(); desplegar(false); }
  });
  cargar();
})();