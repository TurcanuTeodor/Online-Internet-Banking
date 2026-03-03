package ro.app.account.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ro.app.account.model.enums.CurrencyType;
import ro.app.account.exception.BusinessRuleViolationException;

@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    private final String ecbUrl;

    public ExchangeRateService(RestTemplate restTemplate, @Value("${app.fx.ecb-url}") String ecbUrl) {
        this.restTemplate = restTemplate;
        this.ecbUrl = ecbUrl;
    }

    @Cacheable(value = "exchangeRates", key = "#from.name() + ':' + #to.name()")
    public BigDecimal getRate(CurrencyType from, CurrencyType to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (from == to) {
            return BigDecimal.ONE;
        }

        Map<CurrencyType, BigDecimal> rates = fetchRates();
        BigDecimal fromRate = rates.get(from);
        BigDecimal toRate = rates.get(to);

        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("Unsupported currency for ECB rates");
        }

        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }

    @CacheEvict(value = "exchangeRates", allEntries = true)
    public void clearExchangeRatesCache() {
        // Cache cleared by Spring @CacheEvict
    }

    private Map<CurrencyType, BigDecimal> fetchRates() {
        String xml;
        try {
            xml = restTemplate.getForObject(ecbUrl, String.class);
        } catch (RestClientException e) {
            throw new BusinessRuleViolationException("Could not retrieve the exchange rate: external service unavailable");
        }

        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("ECB rates response is empty");
        }
        if (!xml.contains("<Cube")) {
            throw new IllegalStateException("ECB rates response is not valid XML");
        }

        Map<CurrencyType, BigDecimal> rates = new EnumMap<>(CurrencyType.class);
        rates.put(CurrencyType.EUR, BigDecimal.ONE);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList cubes = doc.getElementsByTagName("Cube");
            for (int i = 0; i < cubes.getLength(); i++) {
                Element el = (Element) cubes.item(i);
                if (el.hasAttribute("currency") && el.hasAttribute("rate")) {
                    String currency = el.getAttribute("currency");
                    String rate = el.getAttribute("rate");
                    try {
                        CurrencyType type = CurrencyType.fromCode(currency);
                        rates.put(type, new BigDecimal(rate));
                    } catch (IllegalArgumentException ex) {
                        continue; // silent skip unsupported currencies
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse ECB rates", ex);
        }

        return rates;
    }
}
