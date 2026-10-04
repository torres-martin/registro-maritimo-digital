const $ = (id) => document.getElementById(id);

function aviso(texto) {
  const caja = $("error-login");
  caja.className = texto ? "aviso mal" : "aviso";
  caja.textContent = texto;
}

$("form-login").addEventListener("submit", async (e) => {
  e.preventDefault();
  aviso("");

  const correo = $("correo").value.trim();
  const password = $("password").value;
  if (!correo || !password) {
    aviso("Ingresa tu correo y tu contraseña.");
    return;
  }

  const boton = $("entrar");
  boton.disabled = true;
  try {
    const r = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ correo, password })
    });
    const json = await r.json().catch(() => ({}));
    if (r.ok) {
      location.href = json.destino || "/login.html";
      return;
    }
    aviso(json.mensaje || "No se pudo iniciar sesión.");
  } catch {
    aviso("No se pudo conectar con el servidor.");
  } finally {
    boton.disabled = false;
  }
});