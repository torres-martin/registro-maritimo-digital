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

3. Crear la base de datos vacía. Se puede hacer desde phpMyAdmin o con este comando:

```sql
CREATE DATABASE amp_registro_db;
```

Las tablas no se crean a mano porque la aplicación las genera sola al iniciar.

Si la base ya existía desde antes del Sprint 6, ejecutar una sola vez en phpMyAdmin el script docs/sprint6-columnas.sql (DEF-09: la MariaDB de XAMPP no crea sola esas columnas). En una base nueva no hace falta y daría error de columna duplicada.

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
4. Ejecutar las suites Sprint2, Sprint3 y Sprint4 desde Test Suites.

El CI de GitHub Actions solo ejecuta Sprint1SeleniumTest; Katalon no corre en el pipeline.

## Estructura del proyecto

```
registronaves/
    src/
        main/
            java/        controladores, servicios, modelos y seguridad
            resources/   pantallas HTML, estilos, scripts y application.properties
        test/            pruebas (backend y Sprint1SeleniumTest)
    katalon/             proyecto de Katalon con las suites de los Sprints 2 a 4
    docs/                scripts de apoyo (sprint6-columnas.sql)
    docs-sprint1/        documentación y wireframes del Sprint 1
    pom.xml              dependencias y configuración de Maven
```

## Trabajo en equipo

1. La base de datos es local, así que cada integrante tiene la suya y no ve las solicitudes de los demás.
2. Antes de empezar se hace git pull. Al terminar se hace commit y push.
3. No se sube la contraseña personal de MySQL en application.properties.

## Limitaciones conocidas

* La protección CSRF está desactivada (DEF-07).
* Las columnas nuevas se agregan con un script manual en bases existentes (DEF-09); falta usar migraciones.
* Los usuarios de prueba usan una contraseña simple y solo sirven para desarrollo.
* Los PDFs se guardan en la carpeta uploads del equipo donde corre la aplicación.

