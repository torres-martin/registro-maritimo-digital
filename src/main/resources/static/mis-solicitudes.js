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

function aviso(texto) {
  const caja = $("aviso");
  caja.className = texto ? "aviso mal" : "aviso";
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

function pintarDetalle(d) {
  const x = extras(d);
  const cuerpo = $("det-cuerpo");
  cuerpo.replaceChildren();

  [
    seccion("Solicitud", [
      ["Trámite", d.tramite], ["Documento base", d.documentoBase], ["Presentación", x.presentacion],
      ["Certificado", d.certificadoFolio], ["Vigente hasta", d.fechaVencimiento],
      ["Estado del certificado", d.estadoCertificado],
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

cargar();