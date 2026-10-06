const $ = (id) => document.getElementById(id);
const CLASES = { "En revisión": "en-revision", "Aprobado": "aprobado", "Rechazado": "rechazado" };
let naves = [];

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

function pintarTabla() {
  const texto = $("buscar").value.trim().toLowerCase();
  const estado = $("filtro-estado").value;
  const cuerpo = $("tabla-body");

  const visibles = naves.filter((n) => {
    const coincide = [n.tramite, n.nombreNave, n.imo].join(" ").toLowerCase().includes(texto);
    return coincide && (!estado || n.estado === estado);
  });

  cuerpo.replaceChildren();
  if (visibles.length === 0) {
    cuerpo.append(filaMensaje(naves.length === 0
      ? "Aún no has enviado solicitudes."
      : "Ninguna solicitud coincide con la búsqueda."));
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
    const ver = document.createElement("button");
    ver.type = "button";
    ver.className = "btn o x";
    ver.textContent = "Ver";
    ver.addEventListener("click", () => abrir(n.id));
    tdVer.appendChild(ver);

    // Si la solicitud fue aprobada, se puede descargar el certificado
    if (n.estado === "Aprobado") {
      const cert = document.createElement("a");
      cert.className = "btn o x";
      cert.style.marginLeft = "6px";
      cert.href = "/api/naves/" + encodeURIComponent(n.id) + "/certificado";
      cert.target = "_blank";
      cert.rel = "noopener";
      cert.textContent = "Certificado";
      tdVer.appendChild(cert);
    }

    // HU-17: una solicitud rechazada se puede corregir y reenviar
    if (n.estado === "Rechazado") {
      const corregir = document.createElement("a");
      corregir.className = "btn o x";
      corregir.style.marginLeft = "6px";
      corregir.href = "nueva-solicitud.html?corregir=" + encodeURIComponent(n.id);
      corregir.textContent = "Corregir y reenviar";
      tdVer.appendChild(corregir);
    }

    tr.append(tdCodigo, celda(n.nombreNave), celda(n.imo), celda(n.documentoBase),
              tdEstado, celda(n.fechaCreacion), tdVer);
    cuerpo.append(tr);
  });
}

async function cargar() {
  try {
    const r = await fetch("/api/naves/mias");
    if (!r.ok) throw new Error();
    naves = await r.json();
    pintarTabla();
  } catch {
    $("tabla-body").replaceChildren(filaMensaje("No se pudo cargar la lista."));
  }
}

// ---------- detalle ----------

function extras(d) {
  try {
    return d.adicionales ? JSON.parse(d.adicionales) : {};
  } catch {
    return {};
  }
}

function seccion(titulo, filas) {
  const validas = filas.filter(([, v]) => v !== null && v !== undefined && v !== "");
  if (validas.length === 0) return null;
  const s = document.createElement("section");
  const h = document.createElement("h3");
  h.textContent = titulo;
  const dl = document.createElement("dl");
  dl.className = "kv";
  validas.forEach(([k, v]) => {
    const dt = document.createElement("dt");
    dt.textContent = k;
    const dd = document.createElement("dd");
    dd.textContent = v;
    dl.append(dt, dd);
  });
  s.append(h, dl);
  return s;
}

let codigoActual = null;

function fecha(iso) {
  if (!iso) return "";
  const [a, m, d] = String(iso).split("-");
  return d && m && a ? d + "/" + m + "/" + a : iso;
}

// seguimiento de la solicitud por etapas
const TRAZO_OK = "m6 12 4 4 8-9";
const TRAZO_X = "M7 7l10 10M17 7 7 17";

function icono(trazo) {
  const ns = "http://www.w3.org/2000/svg";
  const s = document.createElementNS(ns, "svg");
  s.setAttribute("viewBox", "0 0 24 24");
  s.setAttribute("width", "16");
  s.setAttribute("height", "16");
  s.setAttribute("fill", "none");
  s.setAttribute("stroke", "currentColor");
  s.setAttribute("stroke-width", "3");
  s.setAttribute("stroke-linecap", "round");
  s.setAttribute("stroke-linejoin", "round");
  s.setAttribute("aria-hidden", "true");
  const p = document.createElementNS(ns, "path");
  p.setAttribute("d", trazo);
  s.appendChild(p);
  return s;
}

function pintarHistorial(d) {
  const enRevision = d.estado === "En revisión";
  const aprobado = d.estado === "Aprobado";
  const rechazado = d.estado === "Rechazado";
  const fRecibida = fecha(d.fechaCreacion);
  const fDecision = fecha(d.fechaDecision || (aprobado ? d.fechaEmision : ""));

  // Cada etapa usa solo datos que el sistema guarda
  const pasos = [
    { titulo: "Recibida", detalle: "La solicitud y su documento fueron recibidos.",
      estado: "hecho", chip: fRecibida || "Listo" },
    { titulo: "Digitalización y control de calidad", detalle: "El PDF se valida (formato y tamaño) al enviarse.",
      estado: "hecho", chip: fRecibida || "Listo" },
    { titulo: "En revisión", detalle: "Un funcionario de la AMP aprueba o rechaza la solicitud.",
      estado: enRevision ? "actual" : "hecho", chip: enRevision ? "En curso" : (fDecision || "Listo") }
  ];

  if (enRevision) {
    pasos.push({ titulo: "Inscrita o devuelta",
      detalle: "Si se aprueba, se emite el certificado. Si se rechaza, se te envían las observaciones.",
      estado: "", chip: "Pendiente" });
  } else if (aprobado) {
    pasos.push({ titulo: "Inscrita", detalle: "La solicitud fue aprobada.",
      estado: "hecho", chip: fDecision || "Listo" });
  } else if (rechazado) {
    pasos.push({ titulo: "Devuelta",
      detalle: d.observacion ? "Observación: " + d.observacion : "La solicitud fue rechazada.",
      estado: "mal", chip: fDecision || "Rechazada" });
  }

  if (d.certificadoFolio && d.revocado) {
    pasos.push({ titulo: "Certificado revocado",
      detalle: "El certificado " + d.certificadoFolio + " ya no es válido."
        + (d.motivoRevocacion ? " Motivo: " + d.motivoRevocacion : ""),
      estado: "mal", chip: fecha(d.fechaRevocacion) || "Revocado" });
  } else if (d.certificadoFolio) {
    pasos.push({ titulo: "Documento entregado",
      detalle: "El certificado " + d.certificadoFolio + " está disponible para descargar.",
      estado: "hecho", chip: fecha(d.fechaEmision) || "Listo" });
  } else if (rechazado) {
    pasos.push({ titulo: "Documento entregado",
      detalle: "No se emite certificado para solicitudes rechazadas.",
      estado: "na", chip: "No aplica" });
  } else {
    pasos.push({ titulo: "Documento entregado",
      detalle: "El certificado electrónico con código QR queda disponible para descargar.",
      estado: "", chip: "Pendiente" });
  }

  const cont = $("det-historial");
  cont.replaceChildren();

  const titulo = document.createElement("h3");
  titulo.className = "seg-titulo";
  titulo.textContent = "Estado actual: " + d.estado.toLowerCase();
  const sub = document.createElement("p");
  sub.className = "seg-sub";
  sub.textContent = "Sigue las mismas etapas que el flujo de inscripción de documentos de la AMP.";
  cont.append(titulo, sub);

  pasos.forEach((p, i) => {
    const fila = document.createElement("div");
    fila.className = "seg-paso " + p.estado;

    const num = document.createElement("span");
    num.className = "seg-num";
    if (p.estado === "hecho") num.appendChild(icono(TRAZO_OK));
    else if (p.estado === "mal") num.appendChild(icono(TRAZO_X));
    else num.textContent = String(i + 1);

    const txt = document.createElement("div");
    txt.className = "seg-txt";
    const t = document.createElement("b");
    t.textContent = p.titulo;
    const s = document.createElement("small");
    s.textContent = p.detalle;
    txt.append(t, s);

    const chip = document.createElement("span");
    chip.className = "seg-chip";
    chip.textContent = p.chip;

    fila.append(num, txt, chip);
    cont.appendChild(fila);
  });

  const info = document.createElement("div");
  info.className = "seg-info";
  info.textContent = "Si el funcionario rechaza la solicitud, recibirás las observaciones y podrás corregirla y reenviarla. "
    + "Si la aprueba, se emite el certificado electrónico con código QR.";
  cont.appendChild(info);
}

function pintarDetalle(d) {
  const x = extras(d);
  const cuerpo = $("det-cuerpo");
  cuerpo.replaceChildren();

  [
    seccion("Solicitud", [
      ["Trámite", d.tramite], ["Documento base", d.documentoBase], ["Presentación", x.presentacion],
      ["Certificado", d.certificadoFolio], ["Vigente hasta", d.fechaVencimiento],
      ["Estado del certificado", d.estadoCertificado],
      ["Motivo de revocación", d.motivoRevocacion],
      ["Fecha de creación", d.fechaCreacion], ["Liquidación", d.liquidacion]
    ]),
    seccion("Nave", [
      ["Nombre", d.nombreNave], ["IMO", d.imo], ["Tipo", d.tipoNave],
      ["Distintivo de llamada", d.distintivoLlamada], ["Patente de navegación", d.patenteNavegacion],
      ["Tonelaje bruto", d.arqueoBruto], ["Tonelaje neto", d.arqueoNeto],
      ["Eslora (m)", d.eslora], ["Manga (m)", d.manga], ["Puntal (m)", d.puntal],
      ["Inscrita en Panamá", x.inscritaEnPanama], ["Ficha", x.ficha],
      ["Precio de venta", d.precioVenta], ["Observaciones", x.observaciones]
    ]),
    seccion("Transmitente", [
      ["Tipo", d.tipoTransmitente], ["Nombre", d.nombreTransmitente], ["Domicilio", d.domicilioTransmitente],
      ["Inscrita en", x.transmitenteInscritaEn], ["Ficha o folio real", x.transmitenteFicha],
      ["Representante", x.transmitenteRepresentante], ["Cargo", x.transmitenteCargo]
    ]),
    seccion("Adquiriente", [
      ["Tipo", d.tipoAdquirente], ["Nombre", d.propietario], ["Domicilio", d.domicilioPropietario],
      ["Inscrita en", x.adquirenteInscritaEn], ["Ficha o folio real", x.adquirenteFicha],
      ["Representante", x.adquirenteRepresentante], ["Cargo", x.adquirenteCargo]
    ]),
    seccion("Autenticación", [
      ["Fecha de celebración", d.fechaCelebracion], ["Fecha de aceptación", x.fechaAceptacion],
      ["Persona que autentica", x.autenticaNombre], ["Cargo", x.autenticaCargo],
      ["Tipo de legalización", d.tipoLegalizacion], ["Autenticación consular o notarial", x.autenticaDetalle]
    ]),
    seccion("Solicitante", [["Solicitante", d.solicitante], ["Abogado a cargo", d.abogado]])
  ].filter(Boolean).forEach((s) => cuerpo.appendChild(s));
}

async function abrir(id) {
  aviso("");
  try {
    const r = await fetch("/api/naves/" + id);
    if (!r.ok) throw new Error();
    const d = await r.json();

    $("det-titulo").textContent = d.nombreNave + " · " + d.tramite;
    $("det-estado").replaceChildren(insignia(d.estado));

    const obs = $("det-obs");
    obs.replaceChildren();
    if (d.observacion) {
      const caja = document.createElement("div");
      caja.className = "obs";
      const t = document.createElement("strong");
      t.textContent = "Observación";
      const p = document.createElement("div");
      p.textContent = d.observacion;
      caja.append(t, p);
      obs.appendChild(caja);
    }

    const doc = $("ver-doc");
    doc.hidden = !d.tieneDocumento;
    doc.href = "/api/naves/" + id + "/documento";

    const cert = $("ver-cert");
    cert.hidden = !d.certificadoFolio;
    cert.href = "/api/naves/" + id + "/certificado";

    const cor = $("corregir");
    cor.hidden = d.estado !== "Rechazado";
    cor.href = "nueva-solicitud.html?corregir=" + encodeURIComponent(id);

    // HU-19: el enlace solo se ofrece si el certificado existe y no está revocado
    codigoActual = d.certificadoFolio && !d.revocado ? d.codigoVerificacion : null;
    $("copiar-enlace").hidden = !codigoActual;

    pintarHistorial(d);
    pintarDetalle(d);
    $("vista-lista").hidden = true;
    $("vista-detalle").hidden = false;
    window.scrollTo(0, 0);
  } catch {
    aviso("No se pudo cargar la solicitud.");
  }
}

$("volver").addEventListener("click", () => {
  $("vista-detalle").hidden = true;
  $("vista-lista").hidden = false;
});
$("buscar").addEventListener("input", pintarTabla);
$("filtro-estado").addEventListener("change", pintarTabla);

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