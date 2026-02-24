package ro.app.banking.service;

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

import ro.app.banking.model.enums.CurrencyType;
import ro.app.banking.exception.BusinessRuleViolationException;

@Service
public class ExchangeRateService {

	private final RestTemplate restTemplate;
	private final String ecbUrl;

	public ExchangeRateService(RestTemplate restTemplate, @Value("${app.fx.ecb-url}") String ecbUrl) { //# issue 1
		this.restTemplate = restTemplate; //primit ca parametru (injected)
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

	/**
	 * Sterge manual toate ratele de schimb din cache.
	 * Util pentru: apelare manuala din admin panel, debugging, sau testing.
	 * 
	 * Nota: Cache-ul expira automat dupa 24h prin CacheConfig,
	 * aceasta metoda este doar pentru clear-uri manuale sau ad-hoc.
	 */
	@CacheEvict(value = "exchangeRates", allEntries = true)
	public void clearExchangeRatesCache() {
		// Cache-ul este sters automat de catre Spring prin @CacheEvict
	}

	// in metoda spune explicit parserului XML sa nu proceseze entitati externe
	private Map<CurrencyType, BigDecimal> fetchRates() {
		String xml;
		try {
			xml = restTemplate.getForObject(ecbUrl, String.class);
		} catch (RestClientException e) {
			throw new BusinessRuleViolationException("Could not retrieve the exchange rate: external service unavailable");
		}

		// Validare raspuns
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
			//previne atacuri XXE
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //interzice declaratii tip DOCTYPE
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //dezactiveaza includerea entitatilor externe generale
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // dezactiveaza entitatile externe de tip parametru
			
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
						continue; //silent skip unsupported currencies
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to parse ECB rates", ex);
		}

		return rates;
	}
}
