const $ = (id) => document.getElementById(id);
const CLASES = { "En revisión": "en-revision", "Aprobado": "aprobado", "Rechazado": "rechazado" };
const id = new URLSearchParams(location.search).get("id");

if (!id || !/^\d+$/.test(id)) {
  location.href = "bandeja.html";
}

function aviso(texto, ok) {
  const caja = $("aviso");
  caja.className = texto ? (ok ? "aviso ok" : "aviso mal") : "aviso";
  caja.textContent = texto;
}

function extras(d) {
  try {
    return d.adicionales ? JSON.parse(d.adicionales) : {};
  } catch {
    return {};
  }
}

// Todo se escribe con textContent (nunca innerHTML) para evitar XSS
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

function pintar(d) {
  const x = extras(d);

  $("sub").textContent = d.tramite + " · " + d.nombreNave;

  const insignia = document.createElement("span");
  insignia.className = "estado " + (CLASES[d.estado] || "");
  insignia.textContent = d.estado;
  $("estado").replaceChildren(insignia);

  const doc = $("ver-doc");
  doc.hidden = !d.tieneDocumento;
  doc.href = "/api/naves/" + id + "/documento";

  const cert = $("ver-cert");
  cert.hidden = !d.certificadoFolio;
  cert.href = "/api/naves/" + id + "/certificado";

  const obs = $("obs");
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

  const cuerpo = $("cuerpo");
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

  $("decision").hidden = d.estado !== "En revisión";
}

async function cargar() {
  try {
    const r = await fetch("/api/naves/" + id);
    if (r.status === 404) {
      aviso("Solicitud no encontrada.", false);
      return;
    }
    if (!r.ok) throw new Error();
    pintar(await r.json());
  } catch {
    aviso("No se pudo cargar la solicitud.", false);
  }
}

async function decidir(estado) {
  aviso("");
  $("err-observacion").textContent = "";
  $("observacion").closest(".campo").classList.remove("campo-error");

  const observacion = $("observacion").value.trim();
  const liquidacion = $("liquidacion").value.trim();

  if (estado === "Rechazado" && !observacion) {
    $("err-observacion").textContent = "Indique la observación del rechazo.";
    $("observacion").closest(".campo").classList.add("campo-error");
    $("observacion").focus();
    return;
  }
  if (!confirm("La decisión no se puede cambiar. ¿Continuar?")) return;

  $("aprobar").disabled = true;
  $("rechazar").disabled = true;
  try {
    const r = await fetch("/api/naves/" + id + "/estado", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ estado, observacion, liquidacion })
    });
    const json = await r.json().catch(() => ({}));
    if (r.ok) {
      pintar(json);
      aviso(estado === "Aprobado" ? "Solicitud aprobada." : "Solicitud rechazada.", true);
    } else {
      aviso(json.mensaje || "No se pudo registrar la decisión.", false);
    }
  } catch {
    aviso("No se pudo conectar con el servidor.", false);
  } finally {
    $("aprobar").disabled = false;
    $("rechazar").disabled = false;
  }
}

$("aprobar").addEventListener("click", () => decidir("Aprobado"));
$("rechazar").addEventListener("click", () => decidir("Rechazado"));

cargar();