package com.example.registronaves.sprint7;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Plan de seguridad del Sprint 6 sobre la bitácora y la revocación (6 pruebas),
// automatizado para que corra en cada pull request.
class Sprint7SeguridadTest extends Sprint7Base {

    @Test
    @DisplayName("S7-SEG-01 Sin sesión no se puede leer la bitácora (401)")
    void bitacoraSinSesion() throws Exception {
        mvc.perform(get("/api/auditoria")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S7-SEG-02 El armador y la aseguradora no leen la bitácora (403)")
    void bitacoraOtrosRoles() throws Exception {
        mvc.perform(get("/api/auditoria").with(armador())).andExpect(status().isForbidden());
        mvc.perform(get("/api/auditoria").with(aseguradora())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("S7-SEG-03 La bitácora es de solo lectura, incluso para el funcionario (403)")
    void bitacoraSoloLectura() throws Exception {
        mvc.perform(post("/api/auditoria").with(funcionario()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"Aprobar\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/auditoria/1").with(funcionario()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/auditoria/1").with(funcionario()).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("S7-SEG-04 Solo el funcionario revoca: el dueño y la aseguradora reciben 403 y no cambia nada")
    void revocarSoloFuncionario() throws Exception {
        long id = registrar("Nave Seguridad Rol");
        decidir(id, "Aprobado", "");
        mvc.perform(put("/api/naves/" + id + "/revocar").with(armador()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/naves/" + id + "/revocar").with(aseguradora()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/naves/" + id).with(funcionario())).andExpect(jsonPath("$.revocado").value(false));
    }

    @Test
    @DisplayName("S7-SEG-05 Revocar exige motivo y no deja rastro si falta")
    void revocarExigeMotivo() throws Exception {
        long id = registrar("Nave Seguridad Motivo");
        decidir(id, "Aprobado", "");
        mvc.perform(put("/api/naves/" + id + "/revocar").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Indique el motivo de la revocación."));
        mvc.perform(get("/api/auditoria").param("naveId", String.valueOf(id)).with(funcionario()))
                .andExpect(jsonPath("$[?(@.accion == 'Revocar')]").isEmpty());
    }

    @Test
    @DisplayName("S7-SEG-06 No se revoca dos veces ni se revoca una solicitud sin certificado")
    void revocarEstadosInvalidos() throws Exception {
        long sinCertificado = registrar("Nave Seguridad Sin Cert");
        mvc.perform(put("/api/naves/" + sinCertificado + "/revocar").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Esta solicitud no tiene certificado emitido."));

        long id = registrar("Nave Seguridad Doble");
        decidir(id, "Aprobado", "");
        revocar(id, "Primera revocación");
        mvc.perform(put("/api/naves/" + id + "/revocar").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Segunda\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El certificado ya fue revocado."));
    }

    @Test
    @DisplayName("S7-CSRF-02 DEF-07: una petición sin token CSRF se rechaza (403) aunque haya sesión válida")
    void sinTokenCsrfSeRechaza() throws Exception {
        long id = registrar("Nave Csrf Sin Token");
        decidir(id, "Aprobado", "");
        mvc.perform(put("/api/naves/" + id + "/revocar").with(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Ataque CSRF\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/naves/" + id).with(funcionario())).andExpect(jsonPath("$.revocado").value(false));
    }
}
