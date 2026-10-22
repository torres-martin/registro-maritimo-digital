(function () {
  // Invalida la sesión del servidor y regresa al inicio de sesión.
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
      const rol = document.getElementById("usuario-rol");
      const iniciales = document.getElementById("usuario-iniciales");

      if (caja) {
        caja.textContent = rol
          ? u.nombre
          : u.nombre + " · " + u.rolEtiqueta;
      }

      if (rol) {
        rol.textContent = (u.rolEtiqueta || "").split(" / ")[0];
      }

      if (iniciales) {
        iniciales.textContent = (u.nombre || "")
          .trim()
          .split(/\s+/)
          .slice(0, 2)
          .map((parte) => Array.from(parte)[0] || "")
          .join("")
          .toLocaleUpperCase("es");
      }
    } catch {
      // Conserva la página cuando no se puede consultar la sesión.
    }
  })();
})();