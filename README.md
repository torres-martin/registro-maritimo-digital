# Registro Marítimo Digital

Sistema web para digitalizar el proceso de registro de naves ante la Autoridad Marítima de Panamá. El armador envía su solicitud con el documento en PDF, el funcionario la revisa y la aprueba o la rechaza, y al aprobarla se emite un certificado con código de verificación y código QR.

## Funcionalidades por rol

### Armador
* Registrar una solicitud con los datos de la nave, las partes y el documento base en PDF.
* Ver sus solicitudes con su estado y el detalle completo.
* Descargar el certificado cuando la solicitud fue aprobada.
* Corregir y reenviar una solicitud rechazada. La corrección queda vinculada a la solicitud de origen.

### Funcionario AMP
* Ver la bandeja con todas las solicitudes, con buscador y filtro por estado.
* Revisar el detalle y abrir el documento adjunto.
* Aprobar o rechazar. El rechazo exige una observación.
* Al aprobar se genera el certificado con una vigencia de seis meses.
* Revocar una solicitud aprobada desde la bandeja, con motivo obligatorio. Queda en estado Revocado.
* Consultar la bitácora de auditoría (botón Bitácora de auditoría en la bandeja): quién envió, corrigió, aprobó, rechazó o revocó cada solicitud, con fecha y hora. Es de solo lectura.

### Aseguradora
* Consultar las naves registradas y su estado.

### Público
* Verificar un certificado con su código, sin iniciar sesión.

## Tecnologías

* Backend: Java 17, Spring Boot, Spring Web, Spring Data JPA y Spring Security
* Base de datos: MySQL
* Certificados: OpenPDF y ZXing para el código QR
* Frontend: HTML, CSS y JavaScript
* Gestor de dependencias: Maven, mediante el wrapper incluido
* Control de versiones: Git y GitHub

## Requisitos previos

Fuera de VS Code:

1. JDK 17 o superior.
2. XAMPP, solo para tener MySQL.
3. Git.

Dentro de VS Code:

1. Extension Pack for Java.
2. Spring Boot Extension Pack, opcional.

No hace falta instalar Maven porque el proyecto incluye mvnw. Tampoco hace falta Apache de XAMPP, porque la aplicación trae su propio servidor.

## Instalación y ejecución local

1. Clonar el repositorio y entrar a la carpeta:

```bash
git clone https://github.com/torres-martin/registro-maritimo-digital.git
cd registro-maritimo-digital
```

2. Abrir XAMPP y presionar Start en MySQL. Apache solo hace falta para usar phpMyAdmin; si phpMyAdmin no abre (localhost rechazó la conexión), es porque Apache está apagado.

3. No hace falta crear la base ni correr scripts. Al iniciar, la aplicación crea la base `amp_registro_db` si no existe y Flyway aplica las migraciones de `src/main/resources/db/migration` (ver la sección Migraciones de base de datos).

4. Revisar en src/main/resources/application.properties que el usuario y la contraseña coincidan con los de MySQL. En XAMPP por defecto es root sin contraseña.

5. Iniciar la aplicación desde la terminal.

En Windows:

```bash
.\mvnw spring-boot:run
```

6. Esperar el mensaje Started RegistronavesApplication y abrir en el navegador. La terminal se queda ocupada mientras la aplicación corre; no se cierra.

```
http://localhost:8080/login.html
```

La primera ejecución tarda más porque descarga las dependencias, así que se necesita conexión a internet.

## Migraciones de base de datos (Flyway)

Desde el Sprint 7 el esquema lo maneja Flyway (DEF-09). Hibernate ya no crea ni modifica tablas; solo valida que coincidan con las entidades (`ddl-auto=validate`).

* Los scripts están en `src/main/resources/db/migration` y se aplican solos al arrancar, en orden de versión.
* Flyway registra lo aplicado en la tabla `flyway_schema_history`.
* Las bases creadas antes del Sprint 7 se adoptan automáticamente (`baseline-on-migrate`); no hay que borrarlas.

Reglas para el equipo:

1. Nunca editar un script que ya está en `main`. Cualquier cambio de tablas va en un archivo nuevo: `V3__descripcion.sql`, `V4__...`, con dos guiones bajos.
2. Si se agrega o cambia un campo en una entidad, en el mismo commit va la migración. Si falta, la aplicación no arranca y el error dice qué columna no coincide.
3. Avisar en el equipo el número de versión antes de crearla, para que dos personas no hagan el mismo `V3`.
4. Los scripts usan sintaxis de MariaDB (la de XAMPP). El CI también usa MariaDB 10.4.

## Usuarios de prueba

Se crean automáticamente la primera vez que se inicia la aplicación. La contraseña de los tres es 1234567.

| Rol | Correo |
| --- | --- |
| Armador | naviera@amp.com |
| Funcionario | funcionario@amp.com |
| Aseguradora | aseguradora@amp.com |

Después de tres intentos fallidos la cuenta se bloquea por 15 minutos.

### Usuarios adicionales para Katalon y Selenium

Katalon necesita además estos dos usuarios, también con la contraseña 1234567. No se crean al iniciar la aplicación (sembrarlos en DatosIniciales rompió el CI).

| Rol | Correo | Para qué sirve |
| --- | --- | --- |
| Armador | naviera2@amp.com | Comprobar que un armador no ve las solicitudes de otro |
| Aseguradora | bloqueo@amp.com | Probar el bloqueo tras tres intentos fallidos |

Se crean de una de estas dos formas:

* Ejecutar una vez la prueba Sprint1SeleniumTest con la aplicación corriendo: `.\mvnw test -Dtest=Sprint1SeleniumTest`
* Ejecutar en phpMyAdmin, sobre amp_registro_db:

```sql
INSERT IGNORE INTO usuarios (correo, nombre, password_hash, rol, intentos_fallidos)
VALUES ('naviera2@amp.com', 'Naviera del Atlántico S.A.',
        '$2a$10$27nkMGznRQUkCJI08gWopexBFmDVvj5aXQEk/F9vnqxkOvEKw25XW', 'ARMADOR', 0);

INSERT IGNORE INTO usuarios (correo, nombre, password_hash, rol, intentos_fallidos)
VALUES ('bloqueo@amp.com', 'Usuario de prueba de bloqueo',
        '$2a$10$27nkMGznRQUkCJI08gWopexBFmDVvj5aXQEk/F9vnqxkOvEKw25XW', 'ASEGURADORA', 0);
```

### IMO válidos para probar

El sistema valida el dígito de control del número IMO, así que un número inventado se rechaza. Estos son válidos: 9074729, 9321483 y 9811000. Cada IMO solo puede registrarse una vez por base de datos.

## Flujo de prueba sugerido

La base nueva no trae solicitudes; hay que crearlas con este flujo.

1. Entrar como armador y crear una solicitud con un IMO válido y un PDF.
2. Cerrar sesión y entrar como funcionario.
3. Abrir la solicitud desde la bandeja con el botón Revisar y aprobarla.
4. Volver a entrar como armador y descargar el certificado.
5. Probar el código del certificado en la pantalla pública de verificación.
6. Como funcionario, pulsar Revocar en la bandeja, escribir el motivo y comprobar que la solicitud queda en Revocado.
7. Abrir la bitácora de auditoría y comprobar que aparecen el envío, la aprobación y la revocación con usuario, fecha y hora.

## Pruebas con Katalon

1. Tener la aplicación corriendo en http://localhost:8080 (el perfil default de Katalon apunta ahí).
2. Crear los usuarios adicionales (ver arriba).
3. En Katalon Studio: File > Open Project y elegir la carpeta katalon (RegistroNaves-QA.prj).
4. Ejecutar las suites Sprint2, Sprint3, Sprint4 y Sprint6 desde Test Suites. Sprint6 (KT-32 a KT-38) valida la bitácora; KT-23, KT-24 y KT-27 de Sprint4 se ajustaron en el Sprint 7 al recorrido de 4 etapas y a la revocación en «Gestionar certificado».

El CI de GitHub Actions ejecuta las pruebas backend del Sprint 7 (`Sprint7*Test`) y la regresión Selenium de los Sprints 1 a 4. Katalon no corre en el pipeline (DEF-05); la suite Sprint6 (KT-32 a KT-38, bitácora) se ejecuta en local.

## Protección CSRF (DEF-07)

Desde el Sprint 7 CSRF está activo. El servidor entrega el token en la cookie `XSRF-TOKEN` y `static/csrf.js` lo agrega en la cabecera `X-XSRF-TOKEN` a toda petición POST, PUT o DELETE. Toda pantalla nueva debe cargar `csrf.js` antes que sus otros scripts. Una llamada sin token responde 403.

## Estado del Sprint 7 y traspaso al Sprint 8

El Sprint 7 entregó los 7 trabajos (15 puntos) que dejó el Sprint 6:

| Prioridad | Trabajo | Evidencia |
| --- | --- | --- |
| P0 | Validar HU-21 (bitácora) | `Sprint7BitacoraTest` 4/4 · Katalon KT-32 a KT-38 |
| P0 | 6 pruebas de seguridad sobre bitácora y revocación | `Sprint7SeguridadTest` (automatizadas en lugar de Burp manual) |
| P0 | DEF-03 corrección vinculada a la rechazada | `Sprint7CorreccionTest` 3/3 |
| P1 | DEF-07 protección CSRF | `Sprint7CsrfTest` 3/3 y S7-CSRF-02 |
| P1 | DEF-09 migraciones con Flyway | V1 y V2 validadas en MariaDB 10.4 |
| P1 | KT-23/24 a 4 etapas y regresión completa | Selenium 33/33 |
| P1 | Pruebas backend en el CI | `.github/workflows/ci.yml` |

En el sprint se registró y cerró **DEF-10**: la regresión Selenium de los Sprints 2 a 4 estaba rota desde el Sprint 6 (IMO sin dígito de control y selectores anteriores al rediseño). Queda abierto **DEF-05** (Katalon en el pipeline).

Pendientes al cerrar el Sprint 7:

* Abrir el pull request de `sprint7-cierre` a `main`. El CI de GitHub todavía no se ha ejecutado con estos cambios; las 50 pruebas se corrieron en local.
* Ejecutar en Katalon las suites Sprint6 (KT-32 a KT-38) y Sprint4 (KT-23, KT-24, KT-27): están escritas, pero no ejecutadas.

Backlog propuesto para el Sprint 8 (15 puntos):

| Prioridad | Trabajo | Puntos |
| --- | --- | --- |
| P0 | PR de `sprint7-cierre` a `main` con el CI en verde | 1 |
| P0 | Ejecutar las suites Katalon Sprint6 y Sprint4 y guardar el reporte | 2 |
| P1 | Dividir `NaveService` (30 % del backend) en servicios de solicitud, revisión y certificado | 5 |
| P1 | Límite de consultas en la verificación pública (429) | 2 |
| P1 | DEF-05: Katalon en el pipeline (Katalon Runtime Engine) | 2 |
| P2 | Prueba de usabilidad con usuarios reales | 2 |
| P2 | Análisis estático del código (SpotBugs o SonarCloud) | 1 |

El backlog que se adoptó para el Sprint 8 cambia puntos y prioridades frente a esta propuesta; está en `docs/Backlog-Sprint8-RegistroNaves.xlsx` y se explica en `docs/README.md`.

El informe completo (pruebas, métricas, registro de defectos, factura, traspaso y evidencias) está en `docs/Sprint7-RegistroNaves.docx` de la rama `sprint7`, junto con la presentación, las facturas y el guion de la charla.

## Estructura del proyecto

```
registronaves/
    src/
        main/
            java/        controladores, servicios, modelos y seguridad
            resources/   pantallas HTML, estilos, scripts y application.properties
                db/migration/  migraciones de Flyway (V1, V2...)
        test/            pruebas: sprint7/ (backend) y Sprint1 a Sprint4SeleniumTest
    katalon/             proyecto de Katalon con las suites Sprint2, Sprint3, Sprint4 y Sprint6
    docs/                documentos, facturas y presentaciones de cada sprint (ver docs/README.md)
    docs-sprint1/        documentación y wireframes del Sprint 1
    pom.xml              dependencias y configuración de Maven
```

## Trabajo en equipo

1. La base de datos es local, así que cada integrante tiene la suya y no ve las solicitudes de los demás.
2. Antes de empezar se hace git pull. Al terminar se hace commit y push.
3. No se sube la contraseña personal de MySQL en application.properties.

## Limitaciones conocidas

* Katalon no corre en el pipeline (DEF-05); se ejecuta en local.
* Los usuarios de prueba usan una contraseña simple y solo sirven para desarrollo.
* Los PDFs se guardan en la carpeta uploads del equipo donde corre la aplicación.

