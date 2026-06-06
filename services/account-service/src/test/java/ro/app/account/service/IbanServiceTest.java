package ro.app.account.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Teste unitare pentru IbanService.
 *
 * IbanService nu are dependinte externe injectate — este un serviciu pur,
 * deci NU avem nevoie de Mockito. Folosim JUnit 4 simplu.
 *
 * Tehnici aplicate:
 * - Analiza valorilor limita: IBAN de lungime 24 (valida) vs. alte lungimi
 * - Echivalenta de clase: null, string gol, IBAN valid, IBAN cu cifre de control gresite
 * - Testam proprietatile structurale ale IBAN-ului romanesc (ISO 13616)
 */
@RunWith(JUnit4.class)
public class IbanServiceTest {

    private IbanService ibanService;

    @Before
    public void setUp() {
        ibanService = new IbanService();
    }

    // ── generateIban ──────────────────────────────────────────────────────────

    @Test
    public void generateIban_produces24CharacterString() {
        // Arrange — predicat care nu gaseste niciun duplicat
        String iban = ibanService.generateIban(s -> false);

        // Assert — Boundary: IBAN romanesc are exact 24 caractere
        Assert.assertEquals("IBAN must have 24 characters", 24, iban.length());
    }

    @Test
    public void generateIban_startsWithROCountryCode() {
        String iban = ibanService.generateIban(s -> false);

        // Assert — primele 2 caractere sunt codul de tara RO
        Assert.assertTrue("IBAN must start with RO", iban.startsWith("RO"));
    }

    @Test
    public void generateIban_containsBankCode() {
        String iban = ibanService.generateIban(s -> false);

        // Assert — caracterele 4-7 contin codul bancii "BANK"
        Assert.assertTrue("IBAN must contain BANK", iban.contains("BANK"));
    }

    @Test
    public void generateIban_generatedIbanPassesOwnValidation() {
        // Arrange
        String iban = ibanService.generateIban(s -> false);

        // Act
        boolean valid = ibanService.isValid(iban);

        // Assert — un IBAN generat de serviciu trebuie sa fie valid prin ISO 13616
        Assert.assertTrue("Generated IBAN must be valid", valid);
    }

    @Test
    public void generateIban_retries_whenFirstIbanExists() {
        // Arrange — simuleaza coliziune: primul IBAN generat "exista" deja, al doilea nu
        final int[] callCount = {0};
        String iban = ibanService.generateIban(s -> {
            callCount[0]++;
            return callCount[0] == 1; // Prima incercare -> coliziune, a doua -> acceptata
        });

        // Assert — serviciul a generat un IBAN nou dupa coliziune
        Assert.assertNotNull(iban);
        Assert.assertEquals(24, iban.length());
        Assert.assertEquals("Should have tried 2 times", 2, callCount[0]);
    }

    // ── isValid ───────────────────────────────────────────────────────────────

    @Test
    public void isValid_nullInput_returnsFalse() {
        Assert.assertFalse(ibanService.isValid(null));
    }

    @Test
    public void isValid_wrongLength_returnsFalse() {
        // Boundary: 23 caractere (sub limita minima de 24)
        Assert.assertFalse(ibanService.isValid("RO49BANK000000000000001"));
    }

    @Test
    public void isValid_invalidCheckDigits_returnsFalse() {
        // IBAN cu cifre de control incorecte (00 in loc de 49)
        Assert.assertFalse(ibanService.isValid("RO00BANK0000000000000001"));
    }
}
