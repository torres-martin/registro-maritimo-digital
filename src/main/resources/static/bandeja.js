const $ = (id) => document.getElementById(id);

const CLASES = {
  "En calificación": "en-revision",
  "En revisión": "en-revision",
  "Aprobado": "aprobado",
  "Rechazado": "rechazado"
};

let naves = [];

// Todo se escribe con textContent para evitar XSS
function celda(texto) {
  const td = document.createElement("td");
  td.textContent = texto || "—";
  return td;
}

function filaMensaje(texto) {
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 8;
  td.className = "vacio";
  td.style.textAlign = "center";
  td.style.padding = "20px";
  td.style.color = "var(--mu)";
  td.textContent = texto;
  tr.appendChild(td);
  return tr;
}

function pintarResumen() {
  $("n-total").textContent = naves.length;
  $("n-revision").textContent = naves.filter((n) => n.estado === "En calificación" || n.estado === "En revisión").length;
  $("n-aprobado").textContent = naves.filter((n) => n.estado === "Aprobado").length;
  $("n-rechazado").textContent = naves.filter((n) => n.estado === "Rechazado").length;
}

function pintarTabla() {
  const texto = $("buscar").value.trim().toLowerCase();
  const estado = $("filtro-estado").value;
  const cuerpo = $("tabla-body");

  const visibles = naves.filter((n) => {
    const coincideTexto = [n.tramite, n.nombreNave, n.imo, n.propietario]
      .join(" ")
      .toLowerCase()
      .includes(texto);

    const coincideEstado = !estado || n.estado === estado || (estado === "En revisión" && n.estado === "En calificación");
    return coincideTexto && coincideEstado;
  });

  cuerpo.replaceChildren();
  if (visibles.length === 0) {
    cuerpo.append(filaMensaje(naves.length === 0
      ? "No hay solicitudes registradas."
      : "Ninguna solicitud coincide con la búsqueda."));
    return;
  }

  visibles.forEach((n) => {
    const tr = document.createElement("tr");

    const tdCodigo = document.createElement("td");
    const codigo = document.createElement("span");
    codigo.className = "codigo";
    codigo.textContent = n.tramite || "TMP";
    tdCodigo.appendChild(codigo);

    const tdEstado = document.createElement("td");
    const insignia = document.createElement("span");
    insignia.className = "estado " + (CLASES[n.estado] || "en-revision");
    insignia.textContent = n.estado || "En calificación";
    tdEstado.appendChild(insignia);

    const tdAccion = document.createElement("td");
    const enlace = document.createElement("a");
    enlace.className = "btn o x";

    if (n.estado === "Aprobado") {
      enlace.href = "/api/naves/" + encodeURIComponent(n.id) + "/certificado";
      enlace.target = "_blank";
      enlace.textContent = "Certificado PDF";
    } else {
      enlace.href = "revision.html?id=" + encodeURIComponent(n.id);
      enlace.textContent = "Revisar";
    }
    tdAccion.appendChild(enlace);

    tr.append(
      tdCodigo,
      celda(n.nombreNave),
      celda(n.imo),
      celda(n.documentoBase || n.tipoNave),
      celda(n.propietario),
      tdEstado,
      celda(n.fechaCreacion || "Reciente"),
      tdAccion
    );

    cuerpo.append(tr);
  });
}

async function cargar() {
  try {
    const r = await fetch("/api/naves/todas");
    if (r.status === 401) {
      location.href = "/login.html";
      return;
    }
    if (!r.ok) {
      console.error("GET /api/naves/todas", r.status, await r.text());
      throw new Error("Error " + r.status);
    }

    naves = await r.json();
    pintarResumen();
    pintarTabla();
  } catch (error) {
    $("tabla-body").replaceChildren(
      filaMensaje("No se pudo cargar la lista (" + error.message + ").")
    );
  }
}

$("buscar")?.addEventListener("input", pintarTabla);
$("filtro-estado")?.addEventListener("change", pintarTabla);

cargar();