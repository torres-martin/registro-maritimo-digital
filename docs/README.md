# Documentación por sprint

Entregables de gestión del proyecto: presentaciones, facturas, informes y backlog.

| Sprint | Archivos |
| --- | --- |
| Sprint 0 | Sprint0-amp.pdf |
| Sprint 2 | sprint2-amp.pdf, Factura-Sprint2-RegistroNaves.xlsx |
| Sprint 3 | sprint3-amp.pdf, Factura-Sprint3-RegistroNaves.xlsx |
| Sprint 4 | Sprint4-amp.pdf |
| Sprint 5 | Factura-Sprint5-RegistroNaves.xlsx |
| Sprint 6 | Sprint6-RegistroNaves.pptx, Sprint6-RegistroNaves.docx, Factura-Sprint6-RegistroNaves.xlsx |
| Sprint 8 | Backlog-Sprint8-RegistroNaves.xlsx, Sprint8-RegistroNaves.pptx |

La documentación y los wireframes del Sprint 1 están en `docs-sprint1/`.

## Sprint 8

### Backlog

15 puntos en 48 horas (4 roles × 12 h), a B/. 98,40 por punto.

| Prioridad | Trabajo | Puntos | B/. |
| --- | --- | --- | --- |
| P1 | Límite de consultas en la verificación pública | 3 | 295,20 |
| P1 | Dividir NaveService | 4 | 393,60 |
| P1 | Análisis estático (SonarQube o SonarCloud en el CI) | 2 | 196,80 |
| P1 | DEF-05 Katalon en el pipeline | 1 | 98,40 |
| P2 | Prueba de usabilidad | 3 | 295,20 |
| P2 | Regresión completa | 2 | 196,80 |
| | **Total** | **15** | **1.476,00** |

### Cambio de prioridad

DEF-05 (Katalon en el pipeline) pasa de P2 a P1. Sin Katalon en el CI, la regresión depende de que alguien la corra en local.

Necesita la licencia de Katalon Runtime Engine. Si no se consigue, el defecto se documenta y se cierra como «No se corrige».

### Presentación

`Sprint8-RegistroNaves.pptx` usa el formato del Sprint 6. Tiene 6 diapositivas: portada, alcance, triple restricción, presupuesto, factura y plan de cierre.

La factura queda **por aceptar** por el Product Owner (Martin Torres). Las evidencias y métricas que lista son las previstas para el sprint, no resultados medidos.

### Supuestos del presupuesto

* Las tarifas y las horas son las mismas del Sprint 6: PO 35, SM 30, Calidad 28 y Seguridad 30 B/./h, 12 h cada rol.
* El acumulado de B/. 11.709,60 (119 puntos) supone que el Sprint 7 fue de 15 puntos, según su backlog. Si cambió, hay que corregir el acumulado en las diapositivas 4 y 5.
* La diferencia del Sprint 2 (B/. 338,40) no aparece en la presentación porque está pendiente de decisión del PO.

### Plan de cierre

* **Sprint 9, preparación para producción:** HTTPS, configuración por ambiente (contraseñas fuera de application.properties), copias de respaldo de la base, manual de usuario corto y defectos que salgan del Sprint 8.
* **Sprint 10, entrega final:** demo completa del ciclo, presupuesto final, lecciones aprendidas y entrega del código y la documentación a la AMP.
