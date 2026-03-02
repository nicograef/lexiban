package de.nicograef.iban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.nicograef.iban.model.IbanNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for ExternalIbanValidator using MockRestServiceServer to intercept
 * HTTP calls.
 *
 * <p>
 * Verifies correct mapping of the openiban.com JSON response to
 * ValidationResult, including edge
 * cases (null bankData, API errors, invalid IBANs).
 */
class ExternalIbanValidatorTest {

    private MockRestServiceServer mockServer;
    private ExternalIbanValidator validator;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        validator = new ExternalIbanValidator(builder);
    }

    @Test
    void validIbanWithBankData() {
        mockServer
                .expect(
                        requestTo(
                                ExternalIbanValidator.BASE_URL
                                        + "DE89370400440532013000?getBIC=true&validateBankCode=true"))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "valid": true,
                                          "messages": ["Bank code valid: 37040044"],
                                          "iban": "DE89370400440532013000",
                                          "bankData": {
                                            "bankCode": "37040044",
                                            "name": "Commerzbank",
                                            "zip": "50447",
                                            "city": "Köln",
                                            "bic": "COBADEFF3701"
                                          },
                                          "checkResults": {"bankCode": true}
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var result = validator.validate(new IbanNumber("DE89370400440532013000"));

        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertEquals("DE89370400440532013000", result.get().iban());
        assertEquals("Commerzbank", result.get().bankName());
        assertNull(result.get().reason());
        mockServer.verify();
    }

    @Test
    void invalidIbanReturnsJoinedMessages() {
        mockServer
                .expect(
                        requestTo(
                                ExternalIbanValidator.BASE_URL
                                        + "DE00370400440532013000?getBIC=true&validateBankCode=true"))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "valid": false,
                                          "messages": ["Validation failed", "Checksum mismatch"],
                                          "iban": "DE00370400440532013000",
                                          "bankData": null,
                                          "checkResults": {}
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var result = validator.validate(new IbanNumber("DE00370400440532013000"));

        assertTrue(result.isPresent());
        assertFalse(result.get().valid());
        assertEquals("Validation failed; Checksum mismatch", result.get().reason());
        assertNull(result.get().bankName());
        mockServer.verify();
    }

    @Test
    void nullBankDataReturnsNullBankName() {
        mockServer
                .expect(
                        requestTo(
                                ExternalIbanValidator.BASE_URL
                                        + "GB29NWBK60161331926819?getBIC=true&validateBankCode=true"))
                .andRespond(
                        withSuccess(
                                """
                                        {"valid": true, "messages": [], "iban": "GB29NWBK60161331926819", "bankData": null}
                                        """,
                                MediaType.APPLICATION_JSON));

        var result = validator.validate(new IbanNumber("GB29NWBK60161331926819"));

        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertNull(result.get().bankName());
        mockServer.verify();
    }

    @Test
    void apiErrorReturnsEmpty() {
        mockServer
                .expect(
                        requestTo(
                                ExternalIbanValidator.BASE_URL
                                        + "DE89370400440532013000?getBIC=true&validateBankCode=true"))
                .andRespond(withServerError());

        var result = validator.validate(new IbanNumber("DE89370400440532013000"));

        assertTrue(result.isEmpty());
        mockServer.verify();
    }

    @Test
    void validIbanWithEmptyMessages() {
        mockServer
                .expect(
                        requestTo(
                                ExternalIbanValidator.BASE_URL
                                        + "DE89370400440532013000?getBIC=true&validateBankCode=true"))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "valid": true,
                                          "messages": [],
                                          "iban": "DE89370400440532013000",
                                          "bankData": {"bankCode": "", "name": ""}
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var result = validator.validate(new IbanNumber("DE89370400440532013000"));

        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertEquals("", result.get().bankName());
        assertNull(result.get().reason());
        mockServer.verify();
    }
}
