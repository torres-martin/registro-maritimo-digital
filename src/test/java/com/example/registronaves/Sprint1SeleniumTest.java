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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de interfaz del Sprint 1 (HU-01 a HU-05).
 * Requiere el sistema corriendo en http://localhost:8080 y MySQL encendido.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Sprint1SeleniumTest {

    private static final String BASE = "http://localhost:8080";
    private static final String CLAVE = "1234567";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/amp_registro_db?useSSL=false&serverTimezone=UTC";
    // Hash BCrypt de la clave 1234567 (para los usuarios extra de prueba)
    private static final String HASH = "$2a$10$27nkMGznRQUkCJI08gWopexBFmDVvj5aXQEk/F9vnqxkOvEKw25XW";

    private static WebDriver driver;
    private static WebDriverWait espera;
    private static final Random AZAR = new Random();

    // ---------- preparación ----------

    @BeforeAll
    static void preparar() throws Exception {
        prepararUsuarios();
        Files.createDirectories(Path.of("evidencias"));

        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--window-size=1366,900");
        
        if (System.getenv("CI") != null) {
            opciones.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
   }
        // Desactivar gestor de contraseñas y advertencias de seguridad de Chrome
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
    }

    @AfterAll
    static void cerrar() throws Exception {
        if (driver != null) driver.quit();
        prepararUsuarios(); // deja las cuentas desbloqueadas
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

    private static void prepararUsuarios() throws Exception {
        try (Connection c = DriverManager.getConnection(DB_URL, "root", "");
             Statement s = c.createStatement()) {
            s.executeUpdate("INSERT IGNORE INTO usuarios (correo, nombre, password_hash, rol, intentos_fallidos) "
                    + "VALUES ('naviera2@amp.com', 'Naviera del Atlántico S.A.', '" + HASH + "', 'ARMADOR', 0)");
            s.executeUpdate("INSERT IGNORE INTO usuarios (correo, nombre, password_hash, rol, intentos_fallidos) "
                    + "VALUES ('bloqueo@amp.com', 'Usuario de prueba de bloqueo', '" + HASH + "', 'ASEGURADORA', 0)");
            s.executeUpdate("UPDATE usuarios SET bloqueado_hasta = NULL, intentos_fallidos = 0");
        }
    }

    // ---------- ayudas ----------

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

    private static String mensajeDeLogin(String correo, String clave) {
        iniciarSesion(correo, clave);
        WebElement errorElement = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("error-login")));
        espera.until(d -> !errorElement.getText().trim().isEmpty());
        return errorElement.getText();
    }

    private static String imoNuevo() {
        return String.format("%07d", 1000000 + AZAR.nextInt(8999999));
    }

    private static Path pdfValido() throws IOException {
        Path f = Files.createTempFile("solicitud", ".pdf");
        Files.writeString(f, "%PDF-1.4\n%Documento de prueba\n%%EOF\n");
        return f;
    }

    private static void llenarHastaPaso3(String nombreNave, String imo) {
        driver.get(BASE + "/nueva-solicitud.html");
        
        // Si no está autenticado y redirigió a login, reintenta sesión
        if (driver.getCurrentUrl().contains("/login.html")) {
            iniciarSesion("naviera@amp.com", CLAVE);
            espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
            driver.get(BASE + "/nueva-solicitud.html");
        }

        // Paso 1: Documentación Base
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

        // Paso 2: Datos de la Nave
        escribir("nombreNave", nombreNave);
        escribir("imo", imo);
        Select selectTipo = new Select(espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("tipoNave"))));
        selectTipo.selectByVisibleText("Granelero");
        clic("siguiente");

        // Paso 3: Confirmación/Solicitante
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

    private static void registrar(String nombreNave, String imo) throws IOException {
        llenarHastaPaso3(nombreNave, imo);
        subirPdf(pdfValido());
        enviarSolicitud();
    }

    // ---------- HU-01: inicio de sesión por rol ----------

    @Test
    @Order(1)
    void cp01_cadaRolEntraASuPantalla() throws IOException {
        String[][] casos = {
                {"naviera@amp.com", "/mis-solicitudes.html", "CP-01a-armador"},
                {"funcionario@amp.com", "/bandeja.html", "CP-01b-funcionario"},
                {"aseguradora@amp.com", "/consulta.html", "CP-01c-aseguradora"}
        };
        for (String[] caso : casos) {
            limpiarAlmacenamientoYSesion();
            iniciarSesion(caso[0], CLAVE);
            espera.until(ExpectedConditions.urlContains(caso[1]));
            assertTrue(driver.getCurrentUrl().endsWith(caso[1]));
            captura(caso[2]);
        }
    }

    @Test
    @Order(2)
    void cp02_unRolNoPuedeAbrirPantallasDeOtro() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        driver.get(BASE + "/bandeja.html");
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        assertTrue(driver.getCurrentUrl().endsWith("/mis-solicitudes.html"));
        captura("CP-02-acceso-denegado");
    }

    // ---------- HU-03, HU-04, HU-05: registro de nave ----------

    @Test
    @Order(3)
    void cp04_armadorRegistraNaveConPdf() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));

        String imo = imoNuevo();
        registrar("Nave Prueba " + imo, imo);

        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));
        assertTrue(texto("ok-tramite").matches("TR-\\d{4}-\\d{4}"), "Trámite inesperado: " + texto("ok-tramite"));
        assertTrue(texto("ok-estado").equals("En revisión"));
        captura("CP-04-solicitud-creada");

        driver.get(BASE + "/mis-solicitudes.html");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), "Nave Prueba " + imo));
        captura("CP-04-mis-solicitudes");
    }

    @Test
    @Order(4)
    void cp05_noPermiteRepetirUnImo() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));

        String imo = imoNuevo();
        registrar("Nave Original " + imo, imo);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));

        registrar("Nave Repetida " + imo, imo);
        assertTrue(texto("resultado").contains("Ya existe una solicitud"), "Mensaje: " + texto("resultado"));
        captura("CP-05-imo-repetido");
    }

    @Test
    @Order(5)
    void cp06a_rechazaArchivoQueNoEsPdf() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));

        llenarHastaPaso3("Nave Texto", imoNuevo());
        Path txt = Files.createTempFile("nota", ".txt");
        Files.writeString(txt, "Esto no es un PDF");
        subirPdf(txt);
        assertTrue(texto("err-pdf").contains("PDF"), "Mensaje: " + texto("err-pdf"));
        captura("CP-06a-archivo-txt");
    }

    @Test
    @Order(6)
    void cp06b_rechazaTextoDisfrazadoDePdf() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));

        llenarHastaPaso3("Nave Falsa", imoNuevo());
        Path falso = Files.createTempFile("falso", ".pdf");
        Files.writeString(falso, "Esto es texto con extension pdf");
        subirPdf(falso);
        enviarSolicitud();
        assertTrue(texto("resultado").contains("PDF válido"), "Mensaje: " + texto("resultado"));
        captura("CP-06b-pdf-falso");
    }

    @Test
    @Order(7)
    void cp06c_rechazaPdfDeMasDe10Mb() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));

        llenarHastaPaso3("Nave Grande", imoNuevo());
        byte[] bytes = new byte[11 * 1024 * 1024];
        bytes[0] = '%'; bytes[1] = 'P'; bytes[2] = 'D'; bytes[3] = 'F';
        Path grande = Files.createTempFile("grande", ".pdf");
        Files.write(grande, bytes);
        subirPdf(grande);
        assertTrue(texto("err-pdf").contains("10 MB"), "Mensaje: " + texto("err-pdf"));
        captura("CP-06c-pdf-grande");
    }

    @Test
    @Order(8)
    void cp07_cadaArmadorVeSoloSusSolicitudes() throws IOException {
        String imoA = imoNuevo();
        String imoB = imoNuevo();
        String nombreA = "Nave de Naviera Uno " + imoA;
        String nombreB = "Nave de Naviera Dos " + imoB;

        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        registrar(nombreA, imoA);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));

        limpiarAlmacenamientoYSesion();

        iniciarSesion("naviera2@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        registrar(nombreB, imoB);
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("ok-tramite")));

        driver.get(BASE + "/mis-solicitudes.html");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nombreB));
        assertFalse(texto("tabla-body").contains(nombreA), "Se ve una solicitud ajena");
        captura("CP-07-solo-sus-solicitudes");
    }

    // ---------- HU-02: bloqueo de cuenta (al final) ----------

    @Test
    @Order(9)
    void cp03_bloqueaLaCuentaTrasTresIntentos() throws IOException {
        String m1 = mensajeDeLogin("bloqueo@amp.com", "clave-mala");
        assertTrue(m1.contains("Te quedan 2"), "Mensaje: " + m1);
        captura("CP-03a-intento-1");

        String m2 = mensajeDeLogin("bloqueo@amp.com", "clave-mala");
        assertTrue(m2.contains("Te quedan 1"), "Mensaje: " + m2);

        String m3 = mensajeDeLogin("bloqueo@amp.com", "clave-mala");
        assertTrue(m3.contains("bloque"), "Mensaje: " + m3);
        captura("CP-03b-cuenta-bloqueada");

        String m4 = mensajeDeLogin("bloqueo@amp.com", CLAVE);
        assertTrue(m4.contains("bloqueada"), "Con la clave correcta sigue bloqueada. Mensaje: " + m4);
        captura("CP-03c-sigue-bloqueada");
    }
}