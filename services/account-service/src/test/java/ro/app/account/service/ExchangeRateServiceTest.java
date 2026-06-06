package ro.app.account.service;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

/**
 * Teste unitare pentru ExchangeRateService — doar logica pura, fara HTTP.
 *
 * Notam ca getRate(from, to) face un apel HTTP real la ECB in productie.
 * In teste unitare, acoperim doar ramurile "fara apel HTTP":
 *   - aceeasi moneda -> 1.0 (short-circuit)
 *   - moneda null -> exceptie
 */
@RunWith(JUnit4.class)
public class ExchangeRateServiceTest {

    /**
     * Construieste un ExchangeRateService cu un RestClient.Builder mock.
     * Nu avem nevoie de @MockitoJUnitRunner deoarece costruim manual.
     */
    private ExchangeRateService buildService() {
        RestClient.Builder builder = Mockito.mock(RestClient.Builder.class);
        RestClient client = Mockito.mock(RestClient.class);
        Mockito.when(builder.build()).thenReturn(client);
        return new ExchangeRateService(builder, "http://ecb.test/rates.xml");
    }

    @Test
    public void getRate_sameFromAndTo_returnsOne() {
        // Arrange
        ExchangeRateService service = buildService();

        // Act — short-circuit: EUR -> EUR = 1.0, fara apel HTTP
        BigDecimal rate = service.getRate(
                ro.app.account.model.enums.CurrencyType.EUR,
                ro.app.account.model.enums.CurrencyType.EUR);

        // Assert
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(rate));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getRate_nullFromCurrency_throwsIllegalArgument() {
        ExchangeRateService service = buildService();
        service.getRate(null, ro.app.account.model.enums.CurrencyType.EUR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getRate_nullToCurrency_throwsIllegalArgument() {
        ExchangeRateService service = buildService();
        service.getRate(ro.app.account.model.enums.CurrencyType.EUR, null);
    }
}
