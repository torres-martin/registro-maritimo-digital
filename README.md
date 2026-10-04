# Registro Marítimo Digital

Sistema web para digitalizar el proceso de registro de naves ante la Autoridad Marítima de Panamá. El armador envía su solicitud con el documento en PDF, el funcionario la revisa y la aprueba o la rechaza, y al aprobarla se emite un certificado con código de verificación y código QR.

## Funcionalidades por rol

### Armador
* Registrar una solicitud con los datos de la nave, las partes y el documento base en PDF.
* Ver sus solicitudes con su estado y el detalle completo.
* Descargar el certificado cuando la solicitud fue aprobada.

### Funcionario AMP
* Ver la bandeja con todas las solicitudes, con buscador y filtro por estado.
* Revisar el detalle y abrir el documento adjunto.
* Aprobar o rechazar. El rechazo exige una observación.
* Al aprobar se genera el certificado con una vigencia de seis meses.

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

## Instalación y ejecución local

1. Clonar el repositorio y entrar a la carpeta:

```bash
git clone https://github.com/torres-martin/registro-maritimo-digital.git
cd registro-maritimo-digital
```

2. Abrir XAMPP y presionar Start solo en MySQL y Apache.

3. Crear la base de datos vacía. Se puede hacer desde phpMyAdmin o con este comando:

```sql
CREATE DATABASE amp_registro_db;
```

Las tablas no se crean a mano porque la aplicación las genera sola al iniciar.

4. Revisar en src/main/resources/application.properties que el usuario y la contraseña coincidan con los de MySQL. En XAMPP por defecto es root sin contraseña.

5. Iniciar la aplicación desde la terminal.

En Windows:

```bash
.\mvnw spring-boot:run
```

6. Esperar el mensaje Started RegistronavesApplication y abrir en el navegador:

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

## Flujo de prueba sugerido

1. Entrar como armador y crear una solicitud con un PDF.
2. Cerrar sesión y entrar como funcionario.
3. Abrir la solicitud desde la bandeja con el botón Revisar y aprobarla.
4. Volver a entrar como armador y descargar el certificado.
5. Probar el código del certificado en la pantalla pública de verificación.

## Estructura del proyecto

```
registronaves/
    src/
        main/
            java/        controladores, servicios, modelos y seguridad
            resources/   pantallas HTML, estilos, scripts y application.properties
        test/            pruebas
    docs-sprint1/        documentación y wireframes del Sprint 1
    pom.xml              dependencias y configuración de Maven
```

## Trabajo en equipo

1. La base de datos es local, así que cada integrante tiene la suya y no ve las solicitudes de los demás.
2. Antes de empezar se hace git pull. Al terminar se hace commit y push.
3. No se sube la contraseña personal de MySQL en application.properties.

## Limitaciones conocidas

* La protección CSRF está desactivada y queda como mejora pendiente.
* Los usuarios de prueba usan una contraseña simple y solo sirven para desarrollo.
* Los PDFs se guardan en la carpeta uploads del equipo donde corre la aplicación.

