// HU-21: bitácora de auditoría (solo lectura, solo funcionario)
const $ = (id) => document.getElementById(id);
let filas = [];

// Todo se escribe con textContent para evitar XSS
function celda(texto) {
  const td = document.createElement("td");
  td.textContent = texto || "—";
  return td;
}

function mensaje(texto) {
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 6;
  td.textContent = texto;
  tr.append(td);
  return tr;
}

function fechaHora(valor) {
  if (!valor) return "—";
  const d = new Date(valor);
  return Number.isNaN(d.getTime()) ? valor : d.toLocaleString("es-PA", { dateStyle: "medium", timeStyle: "short" });
}

function pintar() {
  const texto = $("buscar").value.trim().toLowerCase();
  const accion = $("filtro-accion").value;
  const cuerpo = $("bitacora-body");
  const visibles = filas.filter((f) =>
    (!accion || f.accion === accion) &&
    [f.tramite, f.nombreNave, f.usuario, f.accion, f.detalle].join(" ").toLowerCase().includes(texto));

  cuerpo.replaceChildren();
  if (visibles.length === 0) {
    cuerpo.append(mensaje(filas.length === 0 ? "Todavía no hay acciones registradas." : "Ninguna acción coincide con la búsqueda."));
    return;
  }
  visibles.forEach((f) => {
    const tr = document.createElement("tr");
    tr.append(celda(fechaHora(f.fechaHora)), celda(f.usuario), celda(f.accion), celda(f.tramite), celda(f.nombreNave), celda(f.detalle));
    cuerpo.append(tr);
  });
}

async function cargar() {
  const naveId = new URLSearchParams(location.search).get("naveId");
  const url = "/api/auditoria" + (naveId ? "?naveId=" + encodeURIComponent(naveId) : "");
  try {
    const r = await fetch(url, { cache: "no-store" });
    if (r.status === 401) { location.href = "/login.html"; return; }
    if (!r.ok) throw new Error();
    filas = await r.json();
    pintar();
  } catch {
    $("bitacora-body").replaceChildren(mensaje("No se pudo cargar la bitácora."));
  }
}

$("buscar").addEventListener("input", pintar);
$("filtro-accion").addEventListener("change", pintar);
cargar();
