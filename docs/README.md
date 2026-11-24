# Sprint 8: entrega con evidencia, factura y plan de cierre

| Archivo | Contenido |
| --- | --- |
| `Backlog-Sprint8-RegistroNaves.xlsx` | Backlog planificado del Sprint 8 y plan de los Sprints 9 y 10 |
| `Sprint8-RegistroNaves.pptx` | Presentación con lo entregado, triple restricción, presupuesto, factura y plan de cierre |
| `Sprint8-SonarQube-reporte.md` | Primer reporte del análisis estático (ejecución local) |

Los documentos de los Sprints 0 a 6 están en `docs/` de la rama `main`, y los del Sprint 7 en la rama `sprint7`. La documentación del Sprint 1 está en `docs-sprint1/`.

## Criterio de la factura

**Solo se factura lo que tiene evidencia en el repositorio y en el CI.** El backlog planificado tenía 15 puntos. Al cierre, los tres primeros P1 tienen código y pruebas o reportes que corren en el pipeline: 9 puntos. Los otros 6 no se cobran y pasan al Sprint 9.

El equipo del Sprint 8 es Martin Torres (Product Owner) y Tamara Vega, Diego López y Joseph Urriola (Developers).

## Backlog y estado al cierre

| Prioridad | Trabajo | Puntos | Facturado (B/.) | Estado y evidencia |
| --- | --- | --- | --- | --- |
| P1 | Límite de consultas en la verificación pública | 3 | 300,00 | **Entregado.** Commit `fd42cf6`; pruebas S8-LIM-01 y 02 en el CI |
| P1 | Dividir NaveService | 4 | 400,00 | **Entregado.** Commit `0deb116`; pruebas S8-DIV-01 a 03 en el CI |
| P1 | Análisis estático (SonarQube en el CI) | 2 | 200,00 | **Entregado.** Commit `37b5acc`; trabajo `analisis-estatico` del CI con su reporte |
| P1 | DEF-05 Katalon en el pipeline | 1 | — | Sin licencia de Runtime Engine: pasa al Sprint 9 |
| P2 | Prueba de usabilidad | 3 | — | Sin usuarios todavía: pasa al Sprint 9 |
| P2 | Regresión completa (Katalon y Selenium) | 2 | — | Selenium S1 a S4 pasa en el CI tras la división; falta Katalon: pasa al Sprint 9 |
| | **Facturado** | **9 de 15** | **900,00** | |

Sin cobro, también quedaron en la rama:

* `534fdec`: la regresión Selenium de los Sprints 2 a 4 corre con Chrome en modo headless en el CI.
* `ef351d4`: el CI ejecuta las pruebas del Sprint 8 (`Sprint8*Test`) junto con las del Sprint 7.

### Límite de consultas (entregado)

* `config/LimiteConsultasFilter.java` limita `/api/publico/verificar/` a 10 consultas por IP y por minuto (`app.verificacion.max-por-minuto`).
* Pasado el límite responde **429** con la cabecera `Retry-After` y el mensaje «Demasiadas consultas».
* `sprint8/Sprint8LimiteConsultasTest` lo prueba con un límite de 5: S8-LIM-01 (429 y `Retry-After`) y S8-LIM-02 (el límite es por IP).
* La prueba con Burp que pide el backlog queda para el Sprint 9; no cambia el cobro, porque las pruebas automáticas cubren el comportamiento.

### División de NaveService (entregado)

NaveService tenía 739 líneas, cerca del 30 % del backend. Se dividió por responsabilidad, sin cambiar la API ni el comportamiento:

| Clase | Responsabilidad | Líneas |
| --- | --- | --- |
| `RegistroService` | Registrar la solicitud, validar el IMO y guardar el PDF | 103 |
| `BorradorService` | Borradores del armador y su envío (usa `RegistroService`) | 287 |
| `RevisionService` | Aprobar o rechazar y emitir el certificado | 70 |
| `RevocacionService` | Revocar el certificado | 48 |
| `NaveService` | Solo consultas: listas, detalle, verificación pública, documento y PDF | 111 |
| `NaveVistas` | Mapas de la solicitud para la API (uso interno del paquete) | 95 |
| `ValidacionSolicitud` | Validación de campos, IMO y PDF (uso interno del paquete) | 120 |

* La clase más grande del backend es ahora `CertificadoService` (309 líneas, 12 % de 2.595).
* `NaveController` recibe los cinco servicios; las rutas de la API no cambian.
* `sprint8/Sprint8DivisionNaveServiceTest`: S8-DIV-01 recorre registrar, aprobar, verificar y revocar; S8-DIV-02 envía un borrador con su PDF; S8-DIV-03 comprueba que NaveService solo conserva las consultas.
* Backend en local con MariaDB 10.4: 22 de 22 pruebas (Sprints 7 y 8).

### Análisis estático con SonarQube (entregado)

`scripts/analisis-estatico.sh` levanta SonarQube Community en Docker, analiza el proyecto con Maven y escribe `sonar-reporte.md`. No necesita cuentas ni tokens externos.

* En el CI corre como el trabajo `analisis-estatico`, en paralelo a las pruebas. El reporte aparece en el resumen de la ejecución y se guarda como artefacto `reporte-sonarqube`.
* En local: `./scripts/analisis-estatico.sh` con Docker abierto (en Windows, `MVN=./mvnw.cmd bash scripts/analisis-estatico.sh`).

Primer resultado (`Sprint8-SonarQube-reporte.md`):

| Métrica | Valor |
| --- | --- |
| Líneas de código Java | 2.181 |
| Duplicación | 0 % |
| Bugs | 1 |
| Vulnerabilidades | 1 |
| Code smells | 4 |

* **Bug** en `AuthController.java:53`: calcula una duración con fechas sin zona horaria. Se corrige en el Sprint 9.
* **Vulnerabilidad** en `SeguridadConfig.java:42`: la cookie `XSRF-TOKEN` no es HttpOnly. Es intencional: `csrf.js` la lee para enviar el token en la cabecera (DEF-07). Revisada, se mantiene.

### DEF-05 pasa a P1

DEF-05 (Katalon en el pipeline) pasa de P2 a P1. Sin Katalon en el CI, la regresión depende de que alguien la corra en local. Necesita la licencia de Katalon Runtime Engine; si no se consigue, el defecto se documenta y se cierra como «No se corrige».

### Diferencias con la propuesta del Sprint 7

El README propuso un backlog al cerrar el Sprint 7. El adoptado cambia esto:

* Salen los dos P0: el PR de `sprint7-cierre` a `main` y ejecutar las suites Katalon Sprint6 y Sprint4.
* Cambian los puntos: dividir NaveService pasa de 5 a 4, el límite de consultas de 2 a 3, DEF-05 de 2 a 1 y la usabilidad de 2 a 3.
* El análisis estático sube de P2 (1 punto) a P1 (2 puntos).
* Entra la regresión completa (P2, 2 puntos).

## Factura

La factura está en la diapositiva 5 de la presentación: **B/. 900 en Neuronix**, por aceptar por el Product Owner (Martin Torres).

### Cálculo (mismo método que la factura del Sprint 7)

El costo por punto sale del sprint completo: 4 integrantes × 12 h = 48 h; 420 + 3 × 360 = B/. 1.500 ÷ 15 puntos = **B/. 100 por punto** (3,2 h por punto). Se cobra el tiempo de lo entregado: 9 puntos × 3,2 h = 28,8 h, 7,2 h por integrante.

| Integrante | Rol | Tarifa (B/./h) | Horas | B/. |
| --- | --- | --- | --- | --- |
| Martin Torres | Product Owner | 35 | 7,2 | 252,00 |
| Tamara Vega | Developer | 30 | 7,2 | 216,00 |
| Diego López | Developer | 30 | 7,2 | 216,00 |
| Joseph Urriola | Developer | 30 | 7,2 | 216,00 |
| | **Total** | | **28,8** | **900,00** |

* Comprobación: 9 puntos × B/. 100 = B/. 900.
* Acumulado del MVP: B/. 10.953,60 hasta el Sprint 7 (factura S7-001, 104 puntos) + B/. 900,00 = **B/. 11.853,60 por 113 puntos**.
* Los 6 puntos pendientes (B/. 600 de referencia) no se cobran en el Sprint 8.
* La diferencia del Sprint 2 (B/. 338,40) no aparece en la presentación porque está pendiente de decisión del PO.

## Plan de cierre

* **Sprint 9, pendientes y producción:** primero lo pendiente del Sprint 8 (DEF-05 y regresión completa con Katalon, usabilidad, Burp del límite y el bug de SonarQube); después HTTPS, configuración por ambiente (contraseñas fuera de application.properties), copias de respaldo de la base y manual de usuario corto.
* **Sprint 10, entrega final:** demo completa del ciclo, presupuesto final, lecciones aprendidas y entrega del código y la documentación a la AMP.
