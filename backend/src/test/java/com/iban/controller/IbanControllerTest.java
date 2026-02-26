package com.iban.controller;

import com.iban.repository.IbanRepository;
import com.iban.service.ExternalIbanApiService;
import com.iban.service.IbanValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 *                                   It auto-configures MockMvc for sending fake
 *                                   HTTP requests.
 *
 * @Autowired MockMvc mockMvc
 *            Spring injects a MockMvc instance that acts like a test HTTP
 *            client.
 *            ≈ supertest(app) in Node.js — you can perform().andExpect() like
 *            request(app).post('/api/ibans/validate').expect(200).
 *
 * @MockitoBean
 *              Creates a mock (fake) implementation of the annotated dependency
 *              and
 *              registers it in the Spring context. The controller receives
 *              these mocks
 *              instead of real service/repository beans.
 *              ≈ vi.mock('./service') in Vitest or jest.mock('./service') in
 *              Jest.
 *              Mockito is Java's equivalent of Jest's mocking capabilities.
 *
 *              when(...).thenReturn(...)
 *              Defines what a mock should return when called with specific
 *              arguments.
 *              ≈ vi.mocked(service.validate).mockReturnValue({...}) in Vitest.
 */
@WebMvcTest(IbanController.class)
class IbanControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private IbanValidationService validationService;

        @MockitoBean
        private ExternalIbanApiService externalApiService;

        @MockitoBean
        private IbanRepository ibanRepository;

        // ── Test: Valid IBAN returns 200 with correct JSON ──
        // ≈ it('should validate a valid IBAN', async () => {
        // vi.mocked(service.validate).mockReturnValue({ valid: true, ... })
        // const res = await request(app).post('/api/ibans/validate').send({ iban: '...'
        // })
        // expect(res.status).toBe(200)
        // expect(res.body.valid).toBe(true)
        // })
        @Test
        void validateValidIban() throws Exception {
                when(validationService.validate("DE89370400440532013000"))
                                .thenReturn(new IbanValidationService.ValidationResult(
                                                true, "DE89370400440532013000", "Commerzbank", "37040044", "local"));

                mockMvc.perform(post("/api/ibans/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"iban": "DE89370400440532013000"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.iban").value("DE89370400440532013000"))
                                .andExpect(jsonPath("$.bankName").value("Commerzbank"))
                                .andExpect(jsonPath("$.validationMethod").value("local"));
        }

        @Test
        void validateInvalidIban() throws Exception {
                when(validationService.validate("INVALID"))
                                .thenReturn(new IbanValidationService.ValidationResult(
                                                false, "INVALID", null, null, "local"));

                mockMvc.perform(post("/api/ibans/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"iban": "INVALID"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(false));
        }

        // ── Test: Empty IBAN triggers @NotBlank validation → 400 ──
        // No mock setup needed — the @Valid + @NotBlank annotation on IbanRequest
        // rejects the empty string before the controller method even runs.
        @Test
        void validateEmptyIbanReturnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/ibans/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"iban": ""}
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getAllIbansReturnsEmptyList() throws Exception {
                when(ibanRepository.findAll()).thenReturn(List.of());

                mockMvc.perform(get("/api/ibans"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }
}
