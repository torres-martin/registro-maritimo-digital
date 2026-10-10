package com.example.registronaves;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


 // Pruebas de interfaz del Sprint 4 (HU-16 a HU-20).
// Comando: .\mvnw.cmd test -Dtest=Sprint4SeleniumTest

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Sprint4SeleniumTest {

    private static final String BASE = "http://localhost:8080";
    private static final String CLAVE = "1234567";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static WebDriver driver;
    private static WebDriverWait espera;
    private static final Random AZAR = new Random();

    // PREPARACION

    @BeforeAll
    static void preparar() throws Exception {
        Files.createDirectories(Path.of("evidencias"));

        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--window-size=1366,900");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        opciones.setExperimentalOption("prefs", prefs);

        opciones.addArguments("--disable-save-password-bubble");
        opciones.addArguments("--disable-notifications");
        opciones.addArguments("--disable-popup-blocking");
        opciones.addArguments("--disable-blink-features=AutomationControlled");

        driver = new ChromeDriver(opciones);
        espera = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
    }

    @AfterAll
    static void cerrar() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    void sesionLimpia() {
        limpiarAlmacenamientoYSesion();
    }

    private static void limpiarAlmacenamientoYSesion() {
        try {
            driver.get(BASE + "/login.html");
            driver.manage().deleteAllCookies();
            ((JavascriptExecutor) driver).executeScript(
                    "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch(e) {}");
        } catch (Exception ignored) {
        }
    }

    // ayudas de selenium

    private static void escribir(String id, String texto) {
        WebElement e = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
        e.clear();
        e.sendKeys(texto);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", e);
    }

    private static void clic(String id) {
        espera.until(ExpectedConditions.elementToBeClickable(By.id(id))).click();
    }

    private static String texto(String id) {
        return driver.findElement(By.id(id)).getText();
    }

    private static void captura(String nombre) throws IOException {
        Path origen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
        Files.copy(origen, Path.of("evidencias", nombre + ".png"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void iniciarSesion(String correo, String clave) {
        driver.get(BASE + "/login.html");
        escribir("correo", correo);
        escribir("password", clave);
        clic("entrar");
    }

    private static String imoNuevo() {
        return String.format("%07d", 1000000 + AZAR.nextInt(8999999));
    }

    private static Path pdfValido() throws IOException {
        Path f = Files.createTempFile("solicitud", ".pdf");
        Files.writeString(f, "%PDF-1.4\n%Documento de prueba\n%%EOF\n");
        return f;
    }

    private static void aceptarConfirmacion() {
        espera.until(ExpectedConditions.alertIsPresent()).accept();
    }

    // ayudas del armador

    private static void llenarHastaPaso3(String nombreNave, String imo) {
        driver.get(BASE + "/nueva-solicitud.html");

        Select selectDoc = new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("documentoBase"))));
        selectDoc.selectByVisibleText("Venta judicial");

        WebElement fecha = driver.findElement(By.id("fechaCelebracion"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '2025-01-15';" +
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", fecha);

        escribir("nombreTransmitente", "Transmitente de Prueba S.A.");
        escribir("propietario", "Naviera de Prueba S.A.");
        clic("siguiente");

        escribir("nombreNave", nombreNave);
        escribir("imo", imo);
        Select selectTipo = new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("tipoNave"))));
        selectTipo.selectByVisibleText("Granelero");
        clic("siguiente");

        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("solicitante")));
    }

    private static void subirPdf(Path archivo) {
        WebElement campo = driver.findElement(By.id("pdf"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].hidden = false; arguments[0].style.display = 'block';", campo);
        campo.sendKeys(archivo.toAbsolutePath().toString());
    }

    private static void enviarSolicitud() {
        escribir("solicitante", "Solicitante de Prueba");
        escribir("abogado", "Abogado de Prueba");
        clic("siguiente");
        espera.until(d -> d.findElement(By.id("ok-tramite")).isDisplayed()
                || !d.findElement(By.id("resultado")).getText().isBlank());
    }

    // El armador registra una solicitud y se cierra su sesión. 
    private static void crearSolicitud(String nombreNave, String imo) throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        llenarHastaPaso3(nombreNave, imo);
        subirPdf(pdfValido());
        enviarSolicitud();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));
        limpiarAlmacenamientoYSesion();
    }

    /** El armador entra, busca su solicitud por IMO y abre el detalle con el botón Ver. */
    private static void abrirDetalleComoArmador(String nombreNave, String imo) {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nombreNave));
        driver.findElement(By.cssSelector("#tabla-body button")).click(); // botón "Ver"
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("det-titulo"), nombreNave));
    }

    // ayudas del funcionario

    // Entra como funcionario, busca la solicitud en la bandeja y abre su revisión. 
    private static void abrirRevision(String nombreNave, String imo) {
        iniciarSesion("funcionario@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/bandeja.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nombreNave));
        driver.findElement(By.cssSelector("#tabla-body a[href^='revision.html']")).click();
        espera.until(ExpectedConditions.urlContains("/revision.html"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("sub"), nombreNave));
    }

    private static void aprobarDesdeRevision() {
        clic("aprobar");
        aceptarConfirmacion();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("aviso"), "Solicitud aprobada."));
    }

    /** El funcionario aprueba la solicitud y se cierra su sesión. */
    private static void aprobarSolicitud(String nombreNave, String imo) {
        abrirRevision(nombreNave, imo);
        aprobarDesdeRevision();
        limpiarAlmacenamientoYSesion();
    }

    /** El funcionario rechaza la solicitud con una observación y se cierra su sesión. */
    private static void rechazarSolicitud(String nombreNave, String imo, String observacion) {
        abrirRevision(nombreNave, imo);
        escribir("observacion", observacion);
        clic("rechazar");
        aceptarConfirmacion();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("aviso"), "Solicitud rechazada."));
        limpiarAlmacenamientoYSesion();
    }

    // HU-16: seguimiento por etapas 

    @Test
    @Order(1)
    void cp23_armadorVeElSeguimientoDeUnaSolicitudEnRevision() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Seguimiento " + imo;
        crearSolicitud(nave, imo);

        abrirDetalleComoArmador(nave, imo);
        String seguimiento = texto("det-historial");

        assertTrue(seguimiento.contains("Estado actual: en revisión"), "Seguimiento: " + seguimiento);
        for (String etapa : List.of("Recibida", "Digitalización y control de calidad", "En revisión",
                "Inscrita o devuelta", "Documento entregado")) {
            assertTrue(seguimiento.contains(etapa), "Falta la etapa: " + etapa);
        }
        assertTrue(seguimiento.contains("En curso"), "No se marca la etapa actual");
        assertTrue(seguimiento.contains("Pendiente"), "No se marcan las etapas pendientes");
        assertTrue(seguimiento.contains(LocalDate.now().format(FECHA)), "No se muestra la fecha de recepción");
        captura("CP-23a-seguimiento-en-revision");
    }

    @Test
    @Order(2)
    void cp24_seguimientoDeUnaSolicitudAprobadaMuestraElDocumentoEntregado() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Seguimiento Aprobada " + imo;
        crearSolicitud(nave, imo);
        aprobarSolicitud(nave, imo);

        abrirDetalleComoArmador(nave, imo);
        String seguimiento = texto("det-historial");

        assertTrue(seguimiento.contains("Estado actual: aprobado"), "Seguimiento: " + seguimiento);
        assertTrue(seguimiento.contains("Inscrita"), "Falta la etapa Inscrita");
        assertTrue(seguimiento.contains("Documento entregado"), "Falta la etapa Documento entregado");
        assertTrue(seguimiento.contains("disponible para descargar"), "No indica que el certificado está listo");
        assertFalse(seguimiento.contains("Pendiente"), "Quedan etapas pendientes en una solicitud aprobada");
        captura("CP-24-seguimiento-aprobada");
    }

    // HU-17: corregir y reenviar 

    @Test
    @Order(3)
    void cp25_armadorCorrigeYReenviaUnaSolicitudRechazada() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Corregir " + imo;
        String observacion = "Falta la firma del notario en el documento";
        crearSolicitud(nave, imo);
        rechazarSolicitud(nave, imo, observacion);

        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        assertFalse(driver.findElements(By.cssSelector("#tabla-body a[href*='corregir=']")).isEmpty(),
                "La solicitud rechazada no ofrece Corregir y reenviar");
        captura("CP-25a-boton-corregir-en-lista");

        // En el detalle se ve la etapa Devuelta y el botón para corregir
        driver.findElement(By.cssSelector("#tabla-body button")).click();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("det-titulo"), nave));
        assertTrue(texto("det-historial").contains("Devuelta"), "El seguimiento no muestra la etapa Devuelta");
        clic("corregir");

        // El formulario abre con los datos cargados y la observación visible
        espera.until(ExpectedConditions.urlContains("corregir="));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("correccion")));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("correccion"), observacion));
        assertTrue(texto("correccion").contains("Corrigiendo la solicitud"));
        assertEquals(nave, driver.findElement(By.id("nombreNave")).getAttribute("value"), "No se cargó el nombre de la nave");
        // El sistema guarda el IMO con el prefijo (IMO + 7 dígitos), igual que lo muestra la lista
        assertEquals("IMO" + imo, driver.findElement(By.id("imo")).getAttribute("value"), "No se cargó el IMO");
        captura("CP-25b-formulario-con-datos-cargados");

        // Se avanza hasta el paso 3, se vuelve a adjuntar el PDF y se reenvía
        clic("siguiente");
        clic("siguiente");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("solicitante")));
        subirPdf(pdfValido());
        enviarSolicitud();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));
        assertTrue(texto("ok-estado").contains("En revisión"), "Estado: " + texto("ok-estado"));
        captura("CP-25c-solicitud-reenviada");

        // La lista ahora tiene la solicitud original (Rechazado) y la nueva (En revisión)
        driver.get(BASE + "/mis-solicitudes.html");
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), "En revisión"));
        assertTrue(texto("tabla-body").contains("Rechazado"));
        captura("CP-25d-lista-con-la-solicitud-nueva");
    }

    @Test
    @Order(4)
    void cp26_unaSolicitudEnRevisionNoOfreceCorregir() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Sin Corregir " + imo;
        crearSolicitud(nave, imo);

        abrirDetalleComoArmador(nave, imo);
        assertFalse(driver.findElement(By.id("corregir")).isDisplayed(), "El detalle ofrece corregir una solicitud En revisión");

        clic("volver");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("tabla-body")));
        assertTrue(driver.findElements(By.cssSelector("#tabla-body a[href*='corregir=']")).isEmpty(),
                "La lista ofrece corregir una solicitud En revisión");
        captura("CP-26-sin-boton-corregir");
    }

    //  HU-18: revocar certificado 

    @Test
    @Order(5)
    void cp27_funcionarioRevocaUnCertificadoConMotivo() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Revocada " + imo;
        crearSolicitud(nave, imo);

        abrirRevision(nave, imo);
        aprobarDesdeRevision();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("revocacion")));

        // Sin motivo no se revoca
        clic("revocar");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("err-motivo"), "Indique el motivo de la revocación."));
        captura("CP-27a-revocar-sin-motivo");

        // Con motivo se revoca
        escribir("motivo-revocacion", "Certificado emitido por error en los datos de la nave");
        clic("revocar");
        aceptarConfirmacion();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("aviso"), "Certificado revocado."));
        assertFalse(driver.findElement(By.id("revocacion")).isDisplayed(), "Se puede revocar dos veces");
        captura("CP-27b-certificado-revocado");

        // El código del certificado se pide con la sesión del funcionario
        Object codigo = ((JavascriptExecutor) driver).executeAsyncScript(
                "const cb = arguments[arguments.length - 1];" +
                "const id = new URLSearchParams(location.search).get('id');" +
                "fetch('/api/naves/' + id).then(r => r.json()).then(j => cb(j.codigoVerificacion))" +
                ".catch(() => cb(''));");
        assertTrue(codigo != null && !codigo.toString().isBlank(), "No se obtuvo el código de verificación");

        // La página pública muestra Revocado
        limpiarAlmacenamientoYSesion();
        driver.get(BASE + "/verificar.html?codigo=" + codigo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("resultado"), "Revocado"));
        assertTrue(texto("resultado").contains(nave), "La verificación no muestra la nave");
        captura("CP-27c-verificacion-publica-revocado");

        // La aseguradora también lo ve como Revocado
        limpiarAlmacenamientoYSesion();
        iniciarSesion("aseguradora@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/consulta.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        assertTrue(texto("tabla-body").contains("Revocado"), "La consulta no muestra Revocado");
        captura("CP-27d-consulta-aseguradora-revocado");
    }

    // HU-19: copiar enlace de verificación 

    @Test
    @Order(6)
    void cp28_armadorCopiaElEnlaceDeVerificacion() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Enlace " + imo;
        crearSolicitud(nave, imo);
        aprobarSolicitud(nave, imo);

        // Se da permiso al navegador para usar el portapapeles
        try {
            ((ChromeDriver) driver).executeCdpCommand("Browser.grantPermissions", Map.of(
                    "origin", BASE,
                    "permissions", List.of("clipboardReadWrite", "clipboardSanitizedWrite")));
        } catch (Exception ignored) {
        }

        abrirDetalleComoArmador(nave, imo);
        assertTrue(driver.findElement(By.id("copiar-enlace")).isDisplayed(), "No aparece el botón de copiar enlace");
        captura("CP-28a-boton-copiar-enlace");

        clic("copiar-enlace");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("aviso"), "Enlace de verificación copiado."));
        captura("CP-28b-enlace-copiado");

        // Se lee el portapapeles y se comprueba que el enlace funciona
        Object copiado = ((JavascriptExecutor) driver).executeAsyncScript(
                "const cb = arguments[arguments.length - 1];" +
                "navigator.clipboard.readText().then(t => cb(t)).catch(() => cb(''));");
        String enlace = copiado == null ? "" : copiado.toString();
        assertTrue(enlace.contains("/verificar.html?codigo="), "El portapapeles tiene: '" + enlace + "'");

        driver.get(enlace);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("resultado"), nave));
        assertTrue(texto("resultado").contains("Vigente"), "El enlace no muestra el certificado vigente");
        captura("CP-28c-enlace-abre-la-verificacion");
    }

    @Test
    @Order(7)
    void cp29_sinCertificadoNoSeOfreceElEnlace() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Sin Enlace " + imo;
        crearSolicitud(nave, imo);

        abrirDetalleComoArmador(nave, imo);
        assertFalse(driver.findElement(By.id("copiar-enlace")).isDisplayed(),
                "Se ofrece el enlace sin certificado emitido");
        captura("CP-29-sin-enlace-en-revision");
    }

    // HU-20: consulta de la aseguradora con estado y vencimiento

    @Test
    @Order(8)
    void cp30_aseguradoraVeEstadoYVencimientoDelCertificado() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Vigente " + imo;
        crearSolicitud(nave, imo);
        aprobarSolicitud(nave, imo);

        iniciarSesion("aseguradora@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/consulta.html"));

        String encabezados = driver.findElement(By.tagName("thead")).getText();
        assertTrue(encabezados.contains("Certificado") && encabezados.contains("Vence"),
                "Faltan las columnas Certificado y Vence: " + encabezados);

        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        String fila = texto("tabla-body");
        assertTrue(fila.contains("Vigente"), "No se ve el estado Vigente: " + fila);
        String vence = LocalDate.now().plusMonths(6).format(FECHA);
        assertTrue(fila.contains(vence), "La fecha de vencimiento esperada es " + vence + ": " + fila);
        assertTrue(driver.findElements(By.linkText("Revisar")).isEmpty(), "La aseguradora ve acciones de decisión");
        captura("CP-30-consulta-vigente-con-vencimiento");
    }

    @Test
    @Order(9)
    void cp31_solicitudSinCertificadoNoMuestraEstadoNiVencimiento() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Sin Certificado " + imo;
        crearSolicitud(nave, imo);

        iniciarSesion("aseguradora@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/consulta.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        String fila = texto("tabla-body");
        assertTrue(fila.contains("En revisión"));
        assertFalse(fila.contains("Vigente") || fila.contains("Revocado") || fila.contains("Vencido"),
                "Una solicitud En revisión muestra estado de certificado: " + fila);
        captura("CP-31-consulta-sin-certificado");
    }
}