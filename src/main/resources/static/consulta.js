const $ = (id) => document.getElementById(id);
const CLASES = { "En revisión": "en-revision", "Aprobado": "aprobado", "Rechazado": "rechazado" };
let naves = [];

// Todo se escribe con textContent (nunca innerHTML) para evitar XSS
function celda(texto) {
  const td = document.createElement("td");
  td.textContent = texto || "—";
  return td;
}

function filaMensaje(texto) {
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 6;
  td.className = "vacio";
  td.textContent = texto;
  tr.appendChild(td);
  return tr;
}

function pintarResumen() {
  const cuenta = (estado) => naves.filter((n) => n.estado === estado).length;
  $("n-total").textContent = naves.length;
  $("n-revision").textContent = cuenta("En revisión");
  $("n-aprobado").textContent = cuenta("Aprobado");
  $("n-rechazado").textContent = cuenta("Rechazado");
}

function pintarTabla() {
  const texto = $("buscar").value.trim().toLowerCase();
  const estado = $("filtro-estado").value;
  const cuerpo = $("tabla-body");

  const visibles = naves.filter((n) => {
    const coincide = [n.tramite, n.nombreNave, n.imo, n.propietario].join(" ").toLowerCase().includes(texto);
    return coincide && (!estado || n.estado === estado);
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
    codigo.textContent = n.tramite;
    tdCodigo.appendChild(codigo);

    const tdEstado = document.createElement("td");
    const insignia = document.createElement("span");
    insignia.className = "estado " + (CLASES[n.estado] || "");
    insignia.textContent = n.estado;
    tdEstado.appendChild(insignia);

    tr.append(tdCodigo, celda(n.nombreNave), celda(n.imo), celda(n.tipoNave),
              celda(n.propietario), tdEstado);
    cuerpo.append(tr);
  });
}

async function cargar() {
  try {
    const r = await fetch("/api/naves/consulta");
    if (!r.ok) throw new Error();
    naves = await r.json();
    pintarResumen();
    pintarTabla();
  } catch {
    $("tabla-body").replaceChildren(filaMensaje("No se pudo cargar la consulta."));
  }
}

$("buscar").addEventListener("input", pintarTabla);
$("filtro-estado").addEventListener("change", pintarTabla);
cargar();