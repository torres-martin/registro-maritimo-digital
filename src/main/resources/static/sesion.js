(function () {
  // El cierre de sesión se conecta primero para que el botón nunca quede sin función
  document.getElementById("salir")?.addEventListener("click", async (e) => {
    e.preventDefault();
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } finally {
      location.href = "/login.html";
    }
  });

  (async function () {
    try {
      const r = await fetch("/api/auth/yo");
      if (r.status === 401 || r.status === 403) {
        location.href = "/login.html";
        return;
      }
      if (!r.ok) return;
      const u = await r.json();
      const caja = document.getElementById("usuario");
      if (caja) caja.textContent = u.nombre + " · " + u.rolEtiqueta;
    } catch { /* sin conexión: no se hace nada */ }
  })();
})();