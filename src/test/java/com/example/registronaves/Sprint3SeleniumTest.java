package com.example.registronaves;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de interfaz del Sprint 3 (HU-11 a HU-15).
// Comando: .\mvnw.cmd test -Dtest=Sprint3SeleniumTest

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Sprint3SeleniumTest {

    private static final String BASE = "http://localhost:8080";
    private static final String CLAVE = "1234567";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static WebDriver driver;
    private static WebDriverWait espera;
    private static final Random AZAR = new Random();

    // Solicitud aprobada que comparten las pruebas de certificado
    private static String imoCert;
    private static String naveCert;

    // PREPARACIÓN

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
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(15));
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

    // ayudas

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

    // El armador entra y busca su solicitud por IMO en Mis solicitudes. 
    private static void abrirListaComoArmador(String nombreNave, String imo) {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nombreNave));
    }

    // Id de la solicitud, tomado del enlace "Certificado" de la fila. 
    private static String idDelCertificado() {
        String href = driver.findElement(By.cssSelector("#tabla-body a[href$='/certificado']")).getAttribute("href");
        Matcher m = Pattern.compile("/api/naves/(\\d+)/certificado").matcher(href);
        assertTrue(m.find(), "Enlace de certificado inesperado: " + href);
        return m.group(1);
    }

    // ayudas del funcionario

    // Entra como funcionario, busca la solicitud en la bandeja y la aprueba. 
    private static void aprobarSolicitud(String nombreNave, String imo) {
        iniciarSesion("funcionario@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/bandeja.html"));
        escribir("buscar", imo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("tabla-body"), nombreNave));
        driver.findElement(By.cssSelector("#tabla-body a[href^='revision.html']")).click();
        espera.until(ExpectedConditions.urlContains("/revision.html"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("sub"), nombreNave));
        clic("aprobar");
        aceptarConfirmacion();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("aviso"), "Solicitud aprobada."));
        limpiarAlmacenamientoYSesion();
    }

    // Crea y aprueba una solicitud la primera vez; después reutiliza la misma. 
    private static void asegurarCertificado() throws IOException {
        if (imoCert != null) return;
        String imo = imoNuevo();
        String nave = "Nave Certificada " + imo;
        crearSolicitud(nave, imo);
        aprobarSolicitud(nave, imo);
        imoCert = imo;
        naveCert = nave;
    }

    // ayudas para llamar a la API con la sesión del navegador

    // Pide una ruta de la API con la sesión abierta y devuelve un campo del JSON como texto. 
    private static String campoApi(String ruta, String campo) {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const ruta = arguments[0], campo = arguments[1], cb = arguments[arguments.length - 1];" +
                "fetch(ruta).then(res => res.json()).then(j => cb(j[campo] == null ? '' : String(j[campo])))" +
                ".catch(() => cb(''));", ruta, campo);
        return r == null ? "" : r.toString();
    }

    // Pide una ruta de la API y devuelve [estado HTTP, cuerpo].
    private static List<?> respuestaApi(String ruta) {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const ruta = arguments[0], cb = arguments[arguments.length - 1];" +
                "fetch(ruta).then(res => res.text().then(t => cb([res.status, t])))" +
                ".catch(() => cb([0, '']));", ruta);
        return (List<?>) r;
    }

    private static long estado(List<?> respuesta) {
        return ((Number) respuesta.get(0)).longValue();
    }

    private static String cuerpo(List<?> respuesta) {
        return String.valueOf(respuesta.get(1));
    }

    // Busca el id de una solicitud del armador por su IMO. 
    private static String idPorImo(String imo) {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const imo = arguments[0], cb = arguments[arguments.length - 1];" +
                "fetch('/api/naves/mias').then(res => res.json())" +
                ".then(l => { const n = l.find(x => String(x.imo).includes(imo)); cb(n ? String(n.id) : ''); })" +
                ".catch(() => cb(''));", imo);
        String id = r == null ? "" : r.toString();
        assertFalse(id.isBlank(), "No se encontró la solicitud con IMO " + imo);
        return id;
    }

    /** Descarga un PDF con la sesión del navegador y devuelve sus bytes. */
    private static byte[] descargarPdf(String ruta) {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const ruta = arguments[0], cb = arguments[arguments.length - 1];" +
                "fetch(ruta).then(res => res.blob()).then(b => {" +
                "  const f = new FileReader();" +
                "  f.onload = () => cb(f.result);" +
                "  f.onerror = () => cb('');" +
                "  f.readAsDataURL(b);" +
                "}).catch(() => cb(''));", ruta);
        String datos = r == null ? "" : r.toString();
        assertTrue(datos.startsWith("data:application/pdf;base64,"),
                "No se descargó un PDF desde " + ruta);
        return Base64.getDecoder().decode(datos.substring(datos.indexOf(',') + 1));
    }

    // ayudas para revisar el PDF del certificado 

    private static String textoDelPdf(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(lector).getTextFromPage(1);
        } finally {
            lector.close();
        }
    }

    // El QR se inserta como imagen: la primera página debe tener al menos un objeto externo.
    private static boolean tieneImagen(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfDictionary recursos = lector.getPageN(1).getAsDict(PdfName.RESOURCES);
            PdfDictionary objetos = recursos == null ? null : recursos.getAsDict(PdfName.XOBJECT);
            return objetos != null && objetos.size() > 0;
        } finally {
            lector.close();
        }
    }

    private static String sinEspacios(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static void sinDetallesTecnicos(String mensaje) {
        for (String t : List.of("Exception", "org.springframework", "java.", "SQL", "trace", "Unsupported")) {
            assertFalse(mensaje.contains(t), "El mensaje muestra detalles técnicos (" + t + "): " + mensaje);
        }
    }

    // Muestra un texto en la pantalla para dejarlo como captura de evidencia.
    private static void mostrarTexto(String texto) {
        ((JavascriptExecutor) driver).executeScript(
                "document.body.innerHTML = '';" +
                "const pre = document.createElement('pre');" +
                "pre.style.padding = '24px'; pre.style.whiteSpace = 'pre-wrap';" +
                "pre.textContent = arguments[0];" +
                "document.body.appendChild(pre);", texto);
    }

    //  HU-11: certificado electrónico en PDF 

    @Test
    @Order(1)
    void cp15_armadorDescargaElCertificadoDeUnaSolicitudAprobada() throws IOException, InterruptedException {
        asegurarCertificado();
        abrirListaComoArmador(naveCert, imoCert);
        captura("CP-15a-boton-certificado");

        String id = idDelCertificado();
        byte[] pdf = descargarPdf("/api/naves/" + id + "/certificado");
        assertTrue(pdf.length > 2000, "El certificado pesa muy poco: " + pdf.length + " bytes");

        String folio = campoApi("/api/naves/" + id, "certificadoFolio");
        assertTrue(folio.matches("RMD-\\d{4}-\\d{6}"), "Folio con formato inesperado: " + folio);
        assertTrue(sinEspacios(textoDelPdf(pdf)).contains(folio), "El folio no aparece dentro del PDF");

        driver.get(BASE + "/api/naves/" + id + "/certificado");
        Thread.sleep(1500); // deja que Chrome cargue el visor de PDF
        captura("CP-15b-certificado-pdf");
    }

    @Test
    @Order(2)
    void cp16_unaSolicitudEnRevisionNoTieneCertificado() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Sin Certificado " + imo;
        crearSolicitud(nave, imo);

        abrirListaComoArmador(nave, imo);
        assertTrue(driver.findElements(By.cssSelector("#tabla-body a[href$='/certificado']")).isEmpty(),
                "Se ofrece el certificado de una solicitud En revisión");
        captura("CP-16-sin-certificado-en-revision");
    }

    // HU-12: código de verificación y QR 

    @Test
    @Order(3)
    void cp17_elCertificadoTieneCodigoSeguroYQr() throws IOException {
        asegurarCertificado();
        abrirListaComoArmador(naveCert, imoCert);
        String id = idDelCertificado();

        // El alfabeto del código no incluye 0, 1, I ni O para que sea fácil de dictar
        String codigo = campoApi("/api/naves/" + id, "codigoVerificacion");
        assertTrue(codigo.matches("[A-HJ-NP-Z2-9]{4}(-[A-HJ-NP-Z2-9]{4}){2}"),
                "Código con formato inesperado: " + codigo);

        byte[] pdf = descargarPdf("/api/naves/" + id + "/certificado");
        assertTrue(sinEspacios(textoDelPdf(pdf)).contains(codigo), "El código no aparece dentro del PDF");
        assertTrue(tieneImagen(pdf), "El PDF no incluye la imagen del QR");

        // La prueba no escanea el QR: abre el mismo enlace con el que el sistema lo genera
        driver.get(BASE + "/verificar.html?codigo=" + codigo);
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("resultado"), naveCert));
        captura("CP-17-enlace-del-qr-abre-la-verificacion");
    }

    // HU-13: vigencia de 6 meses

    @Test
    @Order(4)
    void cp18_elCertificadoVenceALosSeisMeses() throws IOException {
        asegurarCertificado();
        abrirListaComoArmador(naveCert, imoCert);
        String id = idDelCertificado();
        LocalDate vence = LocalDate.now().plusMonths(6);

        assertEquals(vence.toString(), campoApi("/api/naves/" + id, "fechaVencimiento"),
                "La vigencia no es de 6 meses");

        // El armador ve el vencimiento en el detalle de su solicitud
        driver.findElement(By.cssSelector("#tabla-body button")).click(); // botón "Ver"
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("det-titulo"), naveCert));
        String detalle = texto("det-cuerpo");
        assertTrue(detalle.contains("Vigente hasta") && detalle.contains(vence.toString()),
                "El detalle no muestra el vencimiento: " + detalle);
        captura("CP-18a-vigencia-en-el-detalle");

        // Y también aparece escrito en el certificado
        byte[] pdf = descargarPdf("/api/naves/" + id + "/certificado");
        assertTrue(sinEspacios(textoDelPdf(pdf)).contains(vence.format(FECHA)),
                "El certificado no muestra la fecha " + vence.format(FECHA));
    }

    //  HU-14: verificación pública.

    @Test
    @Order(5)
    void cp19_cualquierPersonaVerificaUnCertificadoConSuCodigo() throws IOException {
        asegurarCertificado();
        abrirListaComoArmador(naveCert, imoCert);
        String id = idDelCertificado();
        String codigo = campoApi("/api/naves/" + id, "codigoVerificacion");
        String folio = campoApi("/api/naves/" + id, "certificadoFolio");

        // Sin sesión: la página es pública
        limpiarAlmacenamientoYSesion();
        driver.get(BASE + "/verificar.html");
        assertTrue(driver.getCurrentUrl().contains("/verificar.html"), "La página pública pidió iniciar sesión");

        escribir("codigo", codigo.toLowerCase()); // el sistema acepta minúsculas
        clic("verificar");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("resultado"), naveCert));

        String resultado = texto("resultado");
        assertTrue(resultado.contains(folio), "No muestra el folio: " + resultado);
        assertTrue(resultado.contains(imoCert), "No muestra el IMO: " + resultado);
        assertTrue(resultado.contains("Vigente"), "No muestra el estado Vigente: " + resultado);
        assertTrue(resultado.contains(LocalDate.now().format(FECHA)), "No muestra la fecha de emisión");
        assertTrue(resultado.contains(LocalDate.now().plusMonths(6).format(FECHA)), "No muestra la vigencia");
        captura("CP-19-verificacion-publica-valida");
    }

    @Test
    @Order(6)
    void cp20_unCodigoQueNoExisteSeAvisaComoNoValido() throws IOException {
        driver.get(BASE + "/verificar.html");
        escribir("codigo", "ZZZZ-ZZZZ-ZZZZ");
        clic("verificar");
        espera.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("resultado"), "No se encontró ningún certificado con ese código."));
        sinDetallesTecnicos(texto("resultado"));
        captura("CP-20-codigo-no-valido");
    }

    // HU-15: mensajes claros en español 

    @Test
    @Order(7)
    void cp21_losErroresDeLaApiLlegaranEnEspanolSinDetallesTecnicos() throws IOException {
        String imo = imoNuevo();
        String nave = "Nave Errores " + imo;
        crearSolicitud(nave, imo);
        abrirListaComoArmador(nave, imo);
        String id = idPorImo(imo);

        // Una solicitud que no existe
        List<?> noExiste = respuestaApi("/api/naves/99999999");
        assertEquals(404L, estado(noExiste));
        assertTrue(cuerpo(noExiste).contains("Solicitud no encontrada."), cuerpo(noExiste));
        sinDetallesTecnicos(cuerpo(noExiste));
        driver.get(BASE + "/api/naves/99999999");
        captura("CP-21a-solicitud-inexistente");

        // Un id con formato inválido
        List<?> formato = respuestaApi("/api/naves/abc");
        assertEquals(400L, estado(formato));
        assertTrue(cuerpo(formato).contains("tiene un formato inválido."), cuerpo(formato));
        sinDetallesTecnicos(cuerpo(formato));
        driver.get(BASE + "/api/naves/abc");
        captura("CP-21b-formato-invalido");

        // Pedir el certificado de una solicitud que todavía no lo tiene
        List<?> sinCert = respuestaApi("/api/naves/" + id + "/certificado");
        assertEquals(404L, estado(sinCert));
        assertTrue(cuerpo(sinCert).contains("todavía no tiene certificado."), cuerpo(sinCert));
        sinDetallesTecnicos(cuerpo(sinCert));
        driver.get(BASE + "/api/naves/" + id + "/certificado");
        captura("CP-21c-solicitud-sin-certificado");
    }

    @Test
    @Order(8)
    void cp22_unErrorInesperadoTambienLlegaConMensajeClaro() throws IOException {
        iniciarSesion("naviera@amp.com", CLAVE);
        espera.until(ExpectedConditions.urlContains("/mis-solicitudes.html"));

        // Se envía el registro con un formato que el servidor no espera (JSON en vez de formulario)
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const cb = arguments[arguments.length - 1];" +
                "fetch('/api/naves', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' })" +
                ".then(res => res.text().then(t => cb([res.status, t])))" +
                ".catch(() => cb([0, '']));");
        List<?> respuesta = (List<?>) r;
        String texto = cuerpo(respuesta);

        assertTrue(estado(respuesta) >= 400, "El servidor aceptó un formato inválido");
        assertTrue(texto.contains("\"mensaje\""),
                "La respuesta no usa el mensaje en español de ManejadorErrores: " + texto);
        sinDetallesTecnicos(texto);
        mostrarTexto("POST /api/naves con formato inesperado\nEstado HTTP: " + estado(respuesta) + "\n" + texto);
        captura("CP-22-error-inesperado");
    }
}