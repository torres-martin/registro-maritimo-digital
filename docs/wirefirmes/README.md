# Registro Marítimo Digital

Proyecto académico basado en el **Caso 5: Registro de Naves** de la Autoridad Marítima de Panamá (AMP).

## Contexto del caso

| | |
|---|---|
| **Institución** | Autoridad Marítima de Panamá (AMP) |
| **Personas afectadas** | Navieras, armadores y aseguradoras |
| **Problema** | Trámites manuales y certificaciones físicas |
| **Impacto económico** | Panamá recibe millones de balboas por abanderamiento |
| **Base legal** | Ley 55 de 2008 |
| **Objetivo** | Registro marítimo digital con certificaciones electrónicas |

**Pregunta guía:** ¿Cómo convertiríamos el registro panameño en un referente digital mundial?

## Equipo

| Integrantes | Cedula |
|---| ---|
| Martin Torres | 8-1014-2334|
| Neishany López | 8-998-24 |
| Ameth Diaz | 8-1012-447 |
| Yomayri Martinez | 8-1026-1938 |

## Estructura del repositorio

```
registro-maritimo-digital/
├── docs/
│   ├── wireframes/     # Dibujos de las pantallas principales
│   ├── sprints/        # Informe de cada sprint
│   └── seguridad/     
├── src/                # Código fuente (a partir del Sprint 2)
├── .gitignore
└── README.md
```

---

## Sprint 1: Preparación y diseño

**Objetivo del sprint:** Dejar todo listo para empezar y dibujar cómo se verán las pantallas del sistema.

### Incremento entregado

Los dibujos de las 4 pantallas principales y el repositorio en GitHub donde el equipo guarda el trabajo.

| # | Pantalla | Qué muestra |
|---|---|---|
| 1 | **Inicio de sesión** | Selección de tipo de usuario (naviera, armador, aseguradora o funcionario AMP) y mensaje de error con los intentos restantes antes del bloqueo. |
| 2 | **Registro de nave** | Datos de la nave (nombre, IMO, tipo, arqueo, eslora, año) y carga de documentos. Muestra errores por campo y por archivo con formato no permitido. |
| 3 | **Revisión AMP** | Bandeja de solicitudes y detalle con verificación de documentos, observaciones y botones para aprobar, devolver o rechazar. |
| 4 | **Certificado electrónico** | Vista previa del certificado con código QR y código de verificación, descarga en PDF e historial de la solicitud. |

<!-- Reemplazar con los nombres reales de las imágenes exportadas -->
![Inicio de seccion](docs/wirefirmes/1_·_Inicio_de_sesión.png)
![Registro de nave](docs/wireframes/02-registro-nave.png)
![Revisión AMP](docs/wireframes/03-revision-amp.png)
![Certificado electrónico](docs/wireframes/04-certificado.png)

### Evidencias de calidad

Cada dibujo se revisó en equipo con tres preguntas antes de darlo por terminado.

| Pantalla | ¿Se entiende? | ¿Botones claros? | ¿Qué hacer si hay error? | Observación |
|---|:---:|:---:|:---:|---|
| 1. Inicio de sesión | ✅ | ✅ | ✅ | Muestra el error y cuántos intentos quedan antes del bloqueo. |
| 2. Registro de nave | ✅ | ✅ | ✅ | Error por campo (IMO) y por archivo con formato no permitido. |
| 3. Revisión AMP | ✅ | ✅ | ✅ | Devolver o rechazar exige escribir observaciones para el solicitante. |
| 4. Certificado | ✅ | ✅ | ✅ | Incluye QR y código para verificar que es auténtico. |

**Revisado por:** Martin Torres · Neishany Lopez · Ameth Diaz · Yoma
**Fecha de revisión:** [FECHA]

### Evidencias de seguridad

> **Estado:** definidas, sin probar. Se validarán cuando exista la primera página funcionando.

| # | Regla | Cómo se aplicará | Dónde se ve |
|---|---|---|---|
| 1 | **Cada usuario ve solo lo suyo** | Acceso por rol: una naviera solo ve sus naves y solicitudes; solo el funcionario AMP aprueba o rechaza. | Inicio de sesión · Revisión AMP |
| 2 | **Contraseñas guardadas protegidas** | Nunca se guardan en texto plano (se cifran con hash). La cuenta se bloquea 15 minutos tras varios intentos fallidos. | Inicio de sesión |
| 3 | **Solo se aceptan archivos válidos** | Solo PDF de hasta 10 MB. Cualquier otro formato se rechaza con un mensaje claro. | Registro de nave |

### Evidencias de DevOps

- Repositorio creado en GitHub con los 4 integrantes como colaboradores.
- Estructura inicial de carpetas (`docs/`, `src/`) y primeras subidas de archivos.

<!-- Agregar la captura real del repositorio -->
![Captura del repositorio](docs/sprints/captura-repositorio.png)

### Métricas

| Métrica | Resultado |
|---|---|
| Pantallas diseñadas | 4 |
| Tareas terminadas | 5 de 5 |
| Integrantes participando | 4 de 4 |

### Retrospectiva

- **Lo bueno:** nos organizamos rápido.
- **A mejorar:** los dibujos tomaron más tiempo del esperado.
- **Qué haremos distinto:** empezar por lo más difícil primero.
