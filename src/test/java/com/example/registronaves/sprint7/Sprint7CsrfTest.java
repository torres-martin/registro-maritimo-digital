package com.example.registronaves.sprint7;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

// DEF-07: CSRF activo con el token en la cookie XSRF-TOKEN y la cabecera X-XSRF-TOKEN.
// Corre contra el servidor real (puerto aleatorio) para comprobar las cookies tal como las ve el navegador.
// No usa MockMvc ni csrf() de spring-security-test, que cambian el repositorio de tokens del contexto.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sprint7CsrfTest {

    private static final String LOGIN = "{\"correo\":\"funcionario@amp.com\",\"password\":\"1234567\"}";

    @LocalServerPort
    int puerto;

    private final HttpClient http = HttpClient.newHttpClient();

    private String url(String ruta) {
        return "http://localhost:" + puerto + ruta;
    }

    private String cookieXsrf(String ruta) throws Exception {
        HttpResponse<Void> r = http.send(HttpRequest.newBuilder(URI.create(url(ruta))).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        String setCookie = r.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElse(null);
        assertNotNull(setCookie, "La respuesta de " + ruta + " no trae la cookie XSRF-TOKEN");
        assertFalse(setCookie.toLowerCase().contains("httponly"), "csrf.js debe poder leer la cookie");
        return setCookie.substring("XSRF-TOKEN=".length(), setCookie.indexOf(';'));
    }

    private int login(String cookie, String cabecera) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(LOGIN));
        if (cookie != null) b.header("Cookie", "XSRF-TOKEN=" + cookie);
        if (cabecera != null) b.header("X-XSRF-TOKEN", cabecera);
        return http.send(b.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    @DisplayName("S7-CSRF-01 La pantalla de inicio de sesión y /api/csrf entregan la cookie XSRF-TOKEN legible por JS")
    void loginEntregaCookie() throws Exception {
        assertFalse(cookieXsrf("/login.html").isBlank());
        assertFalse(cookieXsrf("/api/csrf").isBlank());
    }

    @Test
    @DisplayName("S7-CSRF-03 El inicio de sesión exige el token y funciona con cookie + cabecera")
    void loginConToken() throws Exception {
        assertEquals(403, login(null, null));
        String token = cookieXsrf("/login.html");
        assertEquals(200, login(token, token));
    }

    @Test
    @DisplayName("S7-CSRF-04 Un token que no coincide con la cookie se rechaza (403)")
    void tokenFalso() throws Exception {
        String token = cookieXsrf("/login.html");
        assertEquals(403, login(token, "token-inventado"));
    }
}
