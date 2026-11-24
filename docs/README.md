# Sprint 8: backlog, factura y plan de cierre

| Archivo | Contenido |
| --- | --- |
| `Backlog-Sprint8-RegistroNaves.xlsx` | Backlog del Sprint 8 y plan de los Sprints 9 y 10 |
| `Sprint8-RegistroNaves.pptx` | Presentación con alcance, triple restricción, presupuesto, factura y plan de cierre |

Los documentos de los Sprints 0 a 6 están en `docs/` de la rama `main`, y los del Sprint 7 en la rama `sprint7`. La documentación del Sprint 1 está en `docs-sprint1/`.

## Backlog adoptado

15 puntos en 48 horas, a B/. 100,00 por punto. El equipo es Martin Torres (Product Owner) y Tamara Vega, Diego López y Joseph Urriola (Developers), 12 h cada uno.

| Prioridad | Trabajo | Puntos | B/. | Estado en `s8-cambios` |
| --- | --- | --- | --- | --- |
| P1 | Límite de consultas en la verificación pública | 3 | 300,00 | Hecho (`fd42cf6`); falta la prueba con Burp |
| P1 | Dividir NaveService | 4 | 400,00 | Pendiente |
| P1 | Análisis estático (SonarQube o SonarCloud en el CI) | 2 | 200,00 | Pendiente |
| P1 | DEF-05 Katalon en el pipeline | 1 | 100,00 | Pendiente |
| P2 | Prueba de usabilidad | 3 | 300,00 | Pendiente |
| P2 | Regresión completa (Katalon y Selenium) | 2 | 200,00 | Pendiente; va después de dividir NaveService |
| | **Total** | **15** | **1.500,00** | |

### Límite de consultas (P1, hecho)

* `config/LimiteConsultasFilter.java` limita `/api/publico/verificar/` a 10 consultas por IP y por minuto (`app.verificacion.max-por-minuto`).
* Pasado el límite responde **429** con la cabecera `Retry-After` y el mensaje «Demasiadas consultas».
* `sprint8/Sprint8LimiteConsultasTest` lo prueba con un límite de 5: S8-LIM-01 (429 y `Retry-After`) y S8-LIM-02 (el límite es por IP).
* El backlog pide además probarlo con Burp. Esa evidencia todavía no está.

### DEF-05 pasa a P1

DEF-05 (Katalon en el pipeline) pasa de P2 a P1. Sin Katalon en el CI, la regresión depende de que alguien la corra en local.

Necesita la licencia de Katalon Runtime Engine. Si no se consigue, el defecto se documenta y se cierra como «No se corrige».

### Diferencias con la propuesta del Sprint 7

El README propuso un backlog al cerrar el Sprint 7. El adoptado cambia esto:

* Salen los dos P0: el PR de `sprint7-cierre` a `main` y ejecutar las suites Katalon Sprint6 y Sprint4.
* Cambian los puntos: dividir NaveService pasa de 5 a 4, el límite de consultas de 2 a 3, DEF-05 de 2 a 1 y la usabilidad de 2 a 3.
* El análisis estático sube de P2 (1 punto) a P1 (2 puntos).
* Entra la regresión completa (P2, 2 puntos).

## Factura

La factura del Sprint 8 está en la diapositiva 5 de la presentación: B/. 1.500 en Neuronix, **por aceptar** por el Product Owner (Martin Torres). Las evidencias y métricas que lista son las previstas para el sprint; solo el límite de consultas tiene ya código y prueba.

### Cálculo (mismo método que la factura del Sprint 7)

| Integrante | Rol | Tarifa (B/./h) | Horas | B/. |
| --- | --- | --- | --- | --- |
| Martin Torres | Product Owner | 35 | 12 | 420,00 |
| Tamara Vega | Developer | 30 | 12 | 360,00 |
| Diego López | Developer | 30 | 12 | 360,00 |
| Joseph Urriola | Developer | 30 | 12 | 360,00 |
| | **Total** | | **48** | **1.500,00** |

* Costo por punto: B/. 1.500,00 ÷ 15 puntos = B/. 100,00 (3,2 h por punto). Cada actividad cuesta sus puntos × 100.
* Acumulado del MVP: B/. 10.953,60 hasta el Sprint 7 (factura S7-001, 104 puntos) + B/. 1.500,00 = **B/. 12.453,60 por 119 puntos**.
* Frente al Sprint 7 (6 personas, 72 h, B/. 2.196) el sprint vale menos porque el equipo queda en 4: salen el Scrum Master y QA.
* La diferencia del Sprint 2 (B/. 338,40) no aparece en la presentación porque está pendiente de decisión del PO.

## Plan de cierre

* **Sprint 9, preparación para producción:** HTTPS, configuración por ambiente (contraseñas fuera de application.properties), copias de respaldo de la base, manual de usuario corto y defectos que salgan del Sprint 8.
* **Sprint 10, entrega final:** demo completa del ciclo, presupuesto final, lecciones aprendidas y entrega del código y la documentación a la AMP.
