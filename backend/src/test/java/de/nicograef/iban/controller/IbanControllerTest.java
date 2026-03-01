package de.nicograef.iban.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.nicograef.iban.model.Iban;
import de.nicograef.iban.repository.IbanRepository;
import de.nicograef.iban.service.IbanValidationService;

/**
 * Integration-style tests for IbanController using MockMvc.
 *
 * ── How this works (for someone used to Vitest/Jest + Supertest) ──
 *
 * @WebMvcTest(IbanController.class)
 *                                   Starts a "thin" Spring context that only
 *                                   loads the web layer (controller +
 *                                   JSON serialization + validation). NO
 *                                   database, NO real services.
 *                                   ≈ In TS: testing an Express router in
 *                                   isolation without starting the full app.
 *
 * @MockitoBean
 *              Creates a mock (fake) implementation of the annotated dependency
 *              and registers it in the Spring context. The controller receives
 *              these mocks instead of real service/repository beans.
 *              ≈ vi.mock('./service') in Vitest.
 *
 *              Note: ExternalIbanApiService is no longer injected into the
 *              controller — it's now inside IbanValidationService. The
 *              controller only depends on IbanValidationService +
 *              IbanRepository.
 */
@WebMvcTest(IbanController.class)
class IbanControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private IbanValidationService validationService;

        @MockitoBean
        private IbanRepository ibanRepository;

        // ── POST /api/ibans — validate + save (or cache hit) ──

        @Test
        void validateValidIban() throws Exception {
                when(validationService.validateOrLookup(any()))
                                .thenReturn(new IbanValidationService.ValidationResult(
                                                true, "DE89370400440532013000", "Commerzbank", "37040044",
                                                null));

                mockMvc.perform(post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"iban": "DE89370400440532013000"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.iban").value("DE89370400440532013000"))
                                .andExpect(jsonPath("$.bankName").value("Commerzbank"))
                                .andExpect(jsonPath("$.reason").isEmpty());
        }

        @Test
        void validateInvalidIban() throws Exception {
                when(validationService.validateRaw("INVALID"))
                                .thenReturn(new IbanValidationService.ValidationResult(
                                                false, "INVALID", null, null,
                                                "IBAN zu kurz: 7 Zeichen (Minimum: 15)"));

                mockMvc.perform(post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"iban": "INVALID"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(false))
                                .andExpect(jsonPath("$.reason").value("IBAN zu kurz: 7 Zeichen (Minimum: 15)"));
        }

        @Test
        void validateEmptyIbanReturnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"iban": ""}
                                                """))
                                .andExpect(status().isBadRequest());
        }

        // ── GET /api/ibans — list all saved IBANs ──

        @Test
        void getAllIbansReturnsEmptyList() throws Exception {
                when(ibanRepository.findAll()).thenReturn(List.of());

                mockMvc.perform(get("/api/ibans"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void getAllIbansReturnsSavedEntries() throws Exception {
                Iban entity = new Iban("DE89370400440532013000", "Commerzbank", "37040044", true, null);
                when(ibanRepository.findAll()).thenReturn(List.of(entity));

                mockMvc.perform(get("/api/ibans"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].iban").value("DE89370400440532013000"))
                                .andExpect(jsonPath("$[0].bankName").value("Commerzbank"))
                                .andExpect(jsonPath("$[0].valid").value(true));
        }
}
