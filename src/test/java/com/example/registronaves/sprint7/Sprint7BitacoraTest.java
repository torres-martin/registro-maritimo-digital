package com.example.registronaves.sprint7;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU-21: la bitácora registra quién hizo qué y cuándo (validación pendiente desde el Sprint 6)
class Sprint7BitacoraTest extends Sprint7Base {

    @Test
    @DisplayName("S7-AUD-01 El envío queda en la bitácora con el armador, la fecha y el IMO")
    void registraEnvio() throws Exception {
        String imo = imoValido();
        long id = registrar(armador(), "Nave Bitacora Envio", imo);
        mvc.perform(get("/api/auditoria").param("naveId", String.valueOf(id)).with(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accion").value("Enviar solicitud"))
                .andExpect(jsonPath("$[0].usuario").value(ARMADOR))
                .andExpect(jsonPath("$[0].detalle").value("IMO IMO" + imo))
                .andExpect(jsonPath("$[0].fechaHora", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("S7-AUD-02 Aprobar y revocar quedan registrados con el funcionario")
    void registraAprobacionYRevocacion() throws Exception {
        long id = registrar("Nave Bitacora Revocar");
        decidir(id, "Aprobado", "");
        revocar(id, "Emitido por error");
        mvc.perform(get("/api/auditoria").param("naveId", String.valueOf(id)).with(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].accion", containsInAnyOrder("Enviar solicitud", "Aprobar", "Revocar")))
                .andExpect(jsonPath("$[?(@.accion == 'Revocar')].usuario", contains(FUNCIONARIO)))
                .andExpect(jsonPath("$[?(@.accion == 'Revocar')].detalle", contains("Emitido por error")))
                .andExpect(jsonPath("$[?(@.accion == 'Aprobar')].detalle", contains(startsWith("Certificado RMD-"))));
    }

    @Test
    @DisplayName("S7-AUD-03 El rechazo queda registrado con su observación")
    void registraRechazo() throws Exception {
        long id = registrar("Nave Bitacora Rechazo");
        decidir(id, "Rechazado", "Falta el certificado de arqueo");
        mvc.perform(get("/api/auditoria").param("naveId", String.valueOf(id)).with(funcionario()))
                .andExpect(jsonPath("$[?(@.accion == 'Rechazar')].detalle", contains("Falta el certificado de arqueo")))
                .andExpect(jsonPath("$[?(@.accion == 'Rechazar')].usuario", contains(FUNCIONARIO)));
    }

    @Test
    @DisplayName("S7-AUD-04 Sin filtro, la bitácora lista todas las acciones de la más reciente a la más antigua")
    void listaGeneralOrdenada() throws Exception {
        long id = registrar("Nave Bitacora General");
        mvc.perform(get("/api/auditoria").with(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].naveId").value((int) id))
                .andExpect(jsonPath("$[0].accion").value("Enviar solicitud"));
    }
}
