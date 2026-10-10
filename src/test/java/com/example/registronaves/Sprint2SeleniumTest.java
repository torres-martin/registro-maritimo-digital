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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//Pruebas de interfaz del Sprint 2 (HU-06 a HU-10).

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Sprint2SeleniumTest {

    private static final String BASE = "http://localhost:8080";
    private static final String CLAVE = "1234567";

    private static WebDriver driver;
    private static WebDriverWait espera;
    private static final Random AZAR = new Random();

    // preparación

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

    // ayudas generales

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

    // ayudas del armador (registro de una solicitud) 

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

    /** El armador registra una solicitud y se cierra su sesión. */
    private static void crearSolicitud(String nombreNave, String imo) throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        llenarHastaPaso3(nombreNave, imo);
        subirPdf(pdfValido());
        enviarSolicitud();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));
        limpiarAlmacenamientoYSesion();
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

    private static void aceptarConfirmacion() {
        espera.until(ExpectedConditions.alertIsPresent()).accept();
    }

    //  HU-06: bandeja del funcionario 

    @Test
    @Order(1)
    void cp08_funcionarioVeLaBandejaYAbreElDetalle() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Bandeja " + imo;
        crearSolicitud(nave, imo);

        iniciarSesion("funcionario@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/bandeja.html"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        assertTrue(Integer.parseInt(texto("n-total")) >= 1, "El resumen no cuenta solicitudes");
        assertTrue(Integer.parseInt(texto("n-revision")) >= 1, "No hay solicitudes En revisión");
        captura("CP-08a-bandeja");

        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        driver.findElement(By.cssSelector("#tabla-body a[href^='revision.html']")).click();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("sub"), nave));
        assertTrue(texto("cuerpo").contains(imo), "El detalle no muestra el IMO");
        assertTrue(texto("estado").contains("En revisión"));
        captura("CP-08b-detalle-solicitud");
    }

    //  HU-07: abrir el PDF

    @Test
    @Order(2)
    void cp09_funcionarioAbreElPdfDeLaSolicitud() throws IOException, InterruptedException {
        String imo = imoNuevo();
        String nave = "Nave Documento " + imo;
        crearSolicitud(nave, imo);
        abrirRevision(nave, imo);

        WebElement enlace = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ver-doc")));
        String href = enlace.getAttribute("href");
        assertTrue(href.contains("/api/naves/") && href.endsWith("/documento"), "Enlace: " + href);
        captura("CP-09a-boton-documento");

        // Se pide el documento con la sesión del funcionario y se revisa que sea un PDF real
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const url = arguments[0], cb = arguments[arguments.length - 1];" +
                "fetch(url).then(res => res.arrayBuffer().then(b =>" +
                "  cb([res.status, new TextDecoder().decode(b.slice(0, 4))])))" +
                ".catch(() => cb([0, '']));", href);
        List<?> datos = (List<?>) r;
        assertEquals(200L, ((Number) datos.get(0)).longValue(), "El documento no respondió 200");
        assertEquals("%PDF", datos.get(1), "El archivo entregado no es un PDF");

        driver.get(href);
        Thread.sleep(1500); // deja que Chrome cargue el visor de PDF
        captura("CP-09b-pdf-abierto");
    }

    //  HU-08: aprobar

    @Test
    @Order(3)
    void cp10_funcionarioApruebaUnaSolicitud() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Aprobada " + imo;
        crearSolicitud(nave, imo);
        abrirRevision(nave, imo);

        clic("aprobar");
        aceptarConfirmacion();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("aviso"), "Solicitud aprobada."));

        assertTrue(texto("estado").contains("Aprobado"), "Estado: " + texto("estado"));
        // Ya resuelta: no se vuelve a mostrar la decisión, así no se puede aprobar dos veces
        assertFalse(driver.findElement(By.id("decision")).isDisplayed(), "Siguen visibles los botones de decisión");
        captura("CP-10-solicitud-aprobada");
    }

    // HU-09: rechazar 

    @Test
    @Order(4)
    void cp11_rechazarExigeUnaObservacion() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Sin Observacion " + imo;
        crearSolicitud(nave, imo);
        abrirRevision(nave, imo);

        clic("rechazar"); // sin escribir observación: no debe aparecer ninguna confirmación
        espera.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("err-observacion"), "Indique la observación del rechazo."));
        assertTrue(texto("estado").contains("En revisión"), "La solicitud cambió de estado");
        captura("CP-11-rechazo-sin-observacion");
    }

    @Test
    @Order(5)
    void cp12_rechazoConObservacionQueElArmadorPuedeVer() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Rechazada " + imo;
        String observacion = "Falta la firma del notario en el documento";
        crearSolicitud(nave, imo);
        abrirRevision(nave, imo);

        escribir("observacion", observacion);
        clic("rechazar");
        aceptarConfirmacion();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("aviso"), "Solicitud rechazada."));
        assertTrue(texto("estado").contains("Rechazado"), "Estado: " + texto("estado"));
        captura("CP-12a-solicitud-rechazada");

        // El armador entra y ve la observación en el detalle de su solicitud
        limpiarAlmacenamientoYSesion();
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        driver.findElement(By.cssSelector("#tabla-body button")).click(); // botón "Ver"
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("det-obs"), observacion));
        assertTrue(texto("det-estado").contains("Rechazado"));
        captura("CP-12b-armador-ve-observacion");
    }

    // HU-10: consulta de la aseguradora

    @Test
    @Order(6)
    void cp13_aseguradoraConsultaLasNavesEnSoloLectura() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Consulta " + imo;
        crearSolicitud(nave, imo);

        iniciarSesion("aseguradora@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/consulta.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nave));
        assertTrue(texto("tabla-body").contains("En revisión"), "No se ve el estado de la nave");
        assertTrue(driver.findElements(By.linkText("Revisar")).isEmpty(), "La aseguradora ve acciones de decisión");
        captura("CP-13-consulta-aseguradora");
    }

    @Test
    @Order(7)
    void cp14_aseguradoraNoPuedeAbrirLaBandeja() throws IOException {
        iniciarSesion("aseguradora@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/consulta.html"));
        driver.get(BASE + "/bandeja.html");
        espera.until(ExpectedConditions.urlContains("/consulta.html"));
        assertTrue(driver.getCurrentUrl().endsWith("/consulta.html"));
        captura("CP-14-acceso-denegado-aseguradora");
    }
}