package de.nicograef.iban.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.nicograef.iban.model.Iban;
import de.nicograef.iban.model.IbanFormatException;
import de.nicograef.iban.model.ValidationResult;
import de.nicograef.iban.repository.IbanRepository;
import de.nicograef.iban.service.IbanService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration-style tests for IbanController using MockMvc.
 *
 * <p>── How this works (for someone used to Vitest/Jest + Supertest)
 * ── @WebMvcTest(IbanController.class) Starts a "thin" Spring context that only loads the web layer
 * (controller + JSON serialization + validation). NO database, NO real services. ≈ In TS: testing
 * an Express router in isolation without starting the full app. @MockitoBean Creates a mock (fake)
 * implementation of the annotated dependency and registers it in the Spring context. The controller
 * receives these mocks instead of real service/repository beans. ≈ vi.mock('./service') in Vitest.
 *
 * <p>The controller only depends on IbanService + IbanRepository. All validation logic lives behind
 * the IbanValidator interface.
 */
@WebMvcTest(IbanController.class)
class IbanControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IbanService ibanService;

    @MockitoBean private IbanRepository ibanRepository;

    // ── POST /api/ibans — validate + save (or cache hit) ──

    @Test
    void validateValidIban() throws Exception {
        when(ibanService.validateOrLookup(anyString()))
                .thenReturn(
                        new ValidationResult(true, "DE89370400440532013000", "Commerzbank", null));

        mockMvc.perform(
                        post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {"iban": "DE89370400440532013000"}
                                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.iban").value("DE89370400440532013000"))
                .andExpect(jsonPath("$.bankName").value("Commerzbank"))
                .andExpect(jsonPath("$.reason").isEmpty());
    }

    @Test
    void structurallyInvalidIbanReturnsBadRequest() throws Exception {
        // "INVALID" is structurally not an IBAN → IbanFormatException → 400
        when(ibanService.validateOrLookup("INVALID"))
                .thenThrow(
                        new IbanFormatException(
                                "IBAN zu kurz: 7 Zeichen (Minimum: 15)", "INVALID"));

        mockMvc.perform(
                        post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {"iban": "INVALID"}
                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.iban").value("INVALID"))
                .andExpect(jsonPath("$.reason").value("IBAN zu kurz: 7 Zeichen (Minimum: 15)"));
    }

    @Test
    void semanticallyInvalidIbanReturnsOkWithValidFalse() throws Exception {
        // Wrong check digit — structurally valid but Mod-97 fails → 200 with
        // valid=false
        when(ibanService.validateOrLookup("DE00370400440532013000"))
                .thenReturn(
                        new ValidationResult(
                                false,
                                "DE00370400440532013000",
                                null,
                                "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)"));

        mockMvc.perform(
                        post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {"iban": "DE00370400440532013000"}
                                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(
                        jsonPath("$.reason")
                                .value("Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)"));
    }

    @Test
    void validateEmptyIbanReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/ibans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
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
        Iban entity = new Iban("DE89370400440532013000", "Commerzbank", true, null);
        when(ibanRepository.findAll()).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/ibans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iban").value("DE89370400440532013000"))
                .andExpect(jsonPath("$[0].bankName").value("Commerzbank"))
                .andExpect(jsonPath("$[0].valid").value(true));
    }
}
