// DEF-07 (Sprint 7): agrega el token CSRF a toda petición que modifica datos.
// Se carga antes que cualquier otro script de la página.
(function () {
  const SEGUROS = ["GET", "HEAD", "OPTIONS", "TRACE"];
  const original = window.fetch.bind(window);

  function leerToken() {
    const par = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
    return par ? decodeURIComponent(par.substring("XSRF-TOKEN=".length)) : "";
  }

  async function token() {
    let valor = leerToken();
    if (!valor) {
      // Sin cookie (por ejemplo, se borraron las cookies): se pide una nueva
      await original("/api/csrf", { cache: "no-store", credentials: "same-origin" });
      valor = leerToken();
    }
    return valor;
  }

  window.fetch = async function (recurso, opciones) {
    const metodo = ((opciones && opciones.method) || (recurso instanceof Request ? recurso.method : "GET")).toUpperCase();
    const url = new URL(recurso instanceof Request ? recurso.url : String(recurso), location.href);
    if (SEGUROS.includes(metodo) || url.origin !== location.origin) {
      return original(recurso, opciones);
    }
    const cabeceras = new Headers((opciones && opciones.headers) || (recurso instanceof Request ? recurso.headers : undefined));
    cabeceras.set("X-XSRF-TOKEN", await token());
    return original(recurso, Object.assign({}, opciones, { headers: cabeceras }));
  };
})();
