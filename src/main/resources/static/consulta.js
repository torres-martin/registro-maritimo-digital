const $ = (id) => document.getElementById(id);
const CLASES = {
  "En revisión": "en-revision", "Aprobado": "aprobado", "Rechazado": "rechazado",
  "Vigente": "aprobado", "Vencido": "rechazado", "Revocado": "rechazado"
};
let naves = [];

// Todo se escribe con textContent (nunca innerHTML) para evitar XSS
function celda(texto) {
  const td = document.createElement("td");
  td.textContent = texto || "—";
  return td;
}

function formatear(iso) {
  if (!iso) return "";
  const [a, m, d] = String(iso).split("-");
  return d && m && a ? d + "/" + m + "/" + a : iso;
}

function filaMensaje(texto) {
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 8;
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

    // HU-20: estado y vencimiento del certificado
    const tdCert = document.createElement("td");
    if (n.estadoCertificado) {
      const c = document.createElement("span");
      c.className = "estado " + (CLASES[n.estadoCertificado] || "");
      c.textContent = n.estadoCertificado;
      tdCert.appendChild(c);
    } else {
      tdCert.textContent = "—";
    }

    tr.append(tdCodigo, celda(n.nombreNave), celda(n.imo), celda(n.tipoNave),
              celda(n.propietario), tdEstado, tdCert, celda(formatear(n.fechaVencimiento)));
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