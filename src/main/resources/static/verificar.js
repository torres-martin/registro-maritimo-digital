const $ = (id) => document.getElementById(id);
const CLASES = { "Vigente": "aprobado", "Vencido": "rechazado", "Revocado": "rechazado" };

function mensaje(texto) {
  const caja = document.createElement("div");
  caja.className = "aviso mal";
  caja.textContent = texto;
  $("resultado").replaceChildren(caja);
}

function formatear(iso) {
  const [a, m, d] = String(iso).split("-");
  return d && m && a ? d + "/" + m + "/" + a : iso;
}

function mostrar(d) {
  const dl = document.createElement("dl");
  dl.className = "kv";
  dl.style.marginTop = "20px";

  const fila = (etiqueta, nodo) => {
    const dt = document.createElement("dt");
    dt.textContent = etiqueta;
    const dd = document.createElement("dd");
    dd.append(nodo);
    dl.append(dt, dd);
  };

  const insignia = document.createElement("span");
  insignia.className = "estado " + (CLASES[d.estado] || "");
  insignia.textContent = d.estado;

  fila("Certificado", document.createTextNode(d.folio));
  fila("Nave", document.createTextNode(d.nombreNave));
  fila("IMO", document.createTextNode(d.imo));
  fila("Estado", insignia);
  fila("Emitido", document.createTextNode(formatear(d.fechaEmision)));
  fila("Vigente hasta", document.createTextNode(formatear(d.fechaVencimiento)));
  $("resultado").replaceChildren(dl);
}

async function verificar(codigo) {
  const boton = $("verificar");
  boton.disabled = true;
  try {
    const r = await fetch("/api/publico/verificar/" + encodeURIComponent(codigo));
    const json = await r.json().catch(() => ({}));
    if (r.ok) {
      mostrar(json);
    } else {
      mensaje(json.mensaje || "No se pudo verificar el certificado.");
    }
  } catch {
    mensaje("No se pudo conectar con el servidor.");
  } finally {
    boton.disabled = false;
  }
}

$("form-verificar").addEventListener("submit", (e) => {
  e.preventDefault();
  const codigo = $("codigo").value.trim().toUpperCase();
  if (!codigo) {
    mensaje("Ingresa el código de verificación.");
    return;
  }
  verificar(codigo);
});

// Permite abrir la página con ?codigo=XXXX-XXXX-XXXX (es lo que lleva el QR)
const inicial = new URLSearchParams(location.search).get("codigo");
if (inicial) {
  $("codigo").value = inicial.toUpperCase();
  verificar(inicial.trim().toUpperCase());
}