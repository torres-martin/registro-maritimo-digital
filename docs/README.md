# Sprint 8: entrega con evidencia, factura y plan de cierre

| Archivo | Contenido |
| --- | --- |
| `Backlog-Sprint8-RegistroNaves.xlsx` | Backlog planificado del Sprint 8 y plan de los Sprints 9 y 10 |
| `Sprint8-RegistroNaves.pptx` | Presentación con lo entregado, triple restricción, presupuesto, factura y plan de cierre |

Los documentos de los Sprints 0 a 6 están en `docs/` de la rama `main`, y los del Sprint 7 en la rama `sprint7`. La documentación del Sprint 1 está en `docs-sprint1/`.

## Criterio de la factura

**Solo se factura lo que tiene evidencia en el repositorio y en el CI.** El backlog planificado tenía 15 puntos; al cierre, solo el límite de consultas tiene código y pruebas que corren en el pipeline. Los otros 12 puntos no se cobran y pasan al Sprint 9.

El equipo del Sprint 8 es Martin Torres (Product Owner) y Tamara Vega, Diego López y Joseph Urriola (Developers).

## Backlog y estado al cierre

| Prioridad | Trabajo | Puntos | Facturado (B/.) | Estado y evidencia |
| --- | --- | --- | --- | --- |
| P1 | Límite de consultas en la verificación pública | 3 | 300,00 | **Entregado.** Commit `fd42cf6`; pruebas S8-LIM-01 y S8-LIM-02 en el CI |
| P1 | Dividir NaveService | 4 | — | Sin evidencia: pasa al Sprint 9 |
| P1 | Análisis estático (SonarQube o SonarCloud en el CI) | 2 | — | Sin evidencia: pasa al Sprint 9 |
| P1 | DEF-05 Katalon en el pipeline | 1 | — | Sin licencia de Runtime Engine: pasa al Sprint 9 |
| P2 | Prueba de usabilidad | 3 | — | Sin usuarios todavía: pasa al Sprint 9 |
| P2 | Regresión completa (Katalon y Selenium) | 2 | — | Va después de dividir NaveService: pasa al Sprint 9 |
| | **Facturado** | **3 de 15** | **300,00** | |

Sin cobro, también quedaron en la rama:

* `534fdec`: la regresión Selenium de los Sprints 2 a 4 corre con Chrome en modo headless en el CI.
* `ef351d4`: el CI ejecuta las pruebas del Sprint 8 (`Sprint8*Test`) junto con las del Sprint 7. Antes `Sprint8LimiteConsultasTest` no corría en el pipeline.

### Límite de consultas (entregado)

* `config/LimiteConsultasFilter.java` limita `/api/publico/verificar/` a 10 consultas por IP y por minuto (`app.verificacion.max-por-minuto`).
* Pasado el límite responde **429** con la cabecera `Retry-After` y el mensaje «Demasiadas consultas».
* `sprint8/Sprint8LimiteConsultasTest` lo prueba con un límite de 5: S8-LIM-01 (429 y `Retry-After`) y S8-LIM-02 (el límite es por IP).
* El backlog pide además una prueba con Burp. No está hecha y pasa al Sprint 9; no cambia el cobro, porque las pruebas automáticas ya cubren el comportamiento.

### DEF-05 pasa a P1

DEF-05 (Katalon en el pipeline) pasa de P2 a P1. Sin Katalon en el CI, la regresión depende de que alguien la corra en local. Necesita la licencia de Katalon Runtime Engine; si no se consigue, el defecto se documenta y se cierra como «No se corrige».

### Diferencias con la propuesta del Sprint 7

El README propuso un backlog al cerrar el Sprint 7. El adoptado cambia esto:

* Salen los dos P0: el PR de `sprint7-cierre` a `main` y ejecutar las suites Katalon Sprint6 y Sprint4.
* Cambian los puntos: dividir NaveService pasa de 5 a 4, el límite de consultas de 2 a 3, DEF-05 de 2 a 1 y la usabilidad de 2 a 3.
* El análisis estático sube de P2 (1 punto) a P1 (2 puntos).
* Entra la regresión completa (P2, 2 puntos).

## Factura

La factura está en la diapositiva 5 de la presentación: **B/. 300 en Neuronix**, por aceptar por el Product Owner (Martin Torres).

### Cálculo (mismo método que la factura del Sprint 7)

El costo por punto sale del sprint completo: 4 integrantes × 12 h = 48 h; 420 + 3 × 360 = B/. 1.500 ÷ 15 puntos = **B/. 100 por punto** (3,2 h por punto). Se cobra el tiempo de lo entregado: 3 puntos × 3,2 h = 9,6 h, 2,4 h por integrante.

| Integrante | Rol | Tarifa (B/./h) | Horas | B/. |
| --- | --- | --- | --- | --- |
| Martin Torres | Product Owner | 35 | 2,4 | 84,00 |
| Tamara Vega | Developer | 30 | 2,4 | 72,00 |
| Diego López | Developer | 30 | 2,4 | 72,00 |
| Joseph Urriola | Developer | 30 | 2,4 | 72,00 |
| | **Total** | | **9,6** | **300,00** |

* Comprobación: 3 puntos × B/. 100 = B/. 300.
* Acumulado del MVP: B/. 10.953,60 hasta el Sprint 7 (factura S7-001, 104 puntos) + B/. 300,00 = **B/. 11.253,60 por 107 puntos**.
* Los 12 puntos pendientes (B/. 1.200 de referencia) no se cobran en el Sprint 8.
* La diferencia del Sprint 2 (B/. 338,40) no aparece en la presentación porque está pendiente de decisión del PO.

## Plan de cierre

* **Sprint 9, pendientes y producción:** primero lo pendiente del Sprint 8 (dividir NaveService y regresión completa, SonarQube, DEF-05, usabilidad y Burp del límite); después HTTPS, configuración por ambiente (contraseñas fuera de application.properties), copias de respaldo de la base y manual de usuario corto.
* **Sprint 10, entrega final:** demo completa del ciclo, presupuesto final, lecciones aprendidas y entrega del código y la documentación a la AMP.
