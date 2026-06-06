package ro.app.account.model.enums;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Teste unitare pentru CurrencyType enum.
 *
 * Tehnici aplicate:
 * - Echivalenta de clase: cod valid (EUR/USD/RON/GBP), cod lowercase, null, blank, invalid
 * - Decision Coverage: toate ramurile din fromCode() sunt testate
 *
 * Nu avem nevoie de Mockito — enum este o clasa pura Java.
 */
@RunWith(JUnit4.class)
public class CurrencyTypeTest {

    // ── fromCode — partiții valide ─────────────────────────────────────────────

    @Test
    public void fromCode_EUR_returnsEurEnum() {
        CurrencyType result = CurrencyType.fromCode("EUR");
        Assert.assertEquals(CurrencyType.EUR, result);
    }

    @Test
    public void fromCode_USD_returnsUsdEnum() {
        CurrencyType result = CurrencyType.fromCode("USD");
        Assert.assertEquals(CurrencyType.USD, result);
    }

    @Test
    public void fromCode_RON_returnsRonEnum() {
        CurrencyType result = CurrencyType.fromCode("RON");
        Assert.assertEquals(CurrencyType.RON, result);
    }

    @Test
    public void fromCode_GBP_returnsGbpEnum() {
        CurrencyType result = CurrencyType.fromCode("GBP");
        Assert.assertEquals(CurrencyType.GBP, result);
    }

    @Test
    public void fromCode_lowercaseCode_returnsEnum() {
        // fromCode trimite la valueOf(toUpperCase) — testam normalizarea
        CurrencyType result = CurrencyType.fromCode("eur");
        Assert.assertEquals(CurrencyType.EUR, result);
    }

    @Test
    public void fromCode_codeWithSpaces_normalizesAndReturnsEnum() {
        CurrencyType result = CurrencyType.fromCode("  USD  ");
        Assert.assertEquals(CurrencyType.USD, result);
    }

    // ── fromCode — partiții invalide ─────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void fromCode_nullCode_throwsIllegalArgument() {
        CurrencyType.fromCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromCode_blankCode_throwsIllegalArgument() {
        CurrencyType.fromCode("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromCode_invalidCode_throwsIllegalArgument() {
        CurrencyType.fromCode("JPY"); // Nu e suportata in sistemul nostru
    }

    // ── getCode / getLabel ────────────────────────────────────────────────────

    @Test
    public void getCode_EUR_returnsEURString() {
        Assert.assertEquals("EUR", CurrencyType.EUR.getCode());
    }

    @Test
    public void getCode_USD_returnsUSDString() {
        Assert.assertEquals("USD", CurrencyType.USD.getCode());
    }

    @Test
    public void getLabel_EUR_returnsHumanReadableLabel() {
        Assert.assertEquals("Euro", CurrencyType.EUR.getLabel());
    }

    @Test
    public void getLabel_RON_returnsHumanReadableLabel() {
        Assert.assertEquals("Romanian Leu", CurrencyType.RON.getLabel());
    }
}
