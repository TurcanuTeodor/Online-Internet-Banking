package ro.app.account.service;

import java.security.SecureRandom;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

@Service
public class IbanService {

    private static final SecureRandom random = new SecureRandom();
    private static final String COUNTRY_CODE = "RO";
    private static final String BANK_CODE = "BANK";
    private static final int ACCOUNT_NUMBER_LENGTH = 16;

    // Generate unique IBAN
    public String generateIban(Predicate<String> existsCheck) {
        String iban;

        do {
            String accountNumber = generateAccountNumber();
            String bban = BANK_CODE + accountNumber;
            String checkDigits = calculateCheckDigits(COUNTRY_CODE, bban);
            iban = COUNTRY_CODE + checkDigits + bban;
        } while (existsCheck.test(iban));

        return iban;
    }

    // Validate IBAN using ISO 13616 (mod 97-10)
    public boolean isValid(String iban) {
        if (iban == null || iban.length() != 24) {
            return false;
        }

        String reformatted = iban.substring(4) + iban.substring(0, 4);

        StringBuilder numeric = new StringBuilder();

        for (char ch : reformatted.toCharArray()) {
            if (Character.isLetter(ch)) {
                numeric.append(Character.getNumericValue(ch));
            } else if (Character.isDigit(ch)) {
                numeric.append(ch);
            } else {
                return false;
            }
        }

        return mod97(numeric.toString()) == 1;
    }

    private String calculateCheckDigits(String countryCode, String bban) {
        String reformatted = bban + countryCode + "00";

        StringBuilder numeric = new StringBuilder();

        for (char ch : reformatted.toCharArray()) {
            if (Character.isLetter(ch)) {
                numeric.append(Character.getNumericValue(ch));
            } else {
                numeric.append(ch);
            }
        }

        int remainder = mod97(numeric.toString());
        int checkDigit = 98 - remainder;

        return String.format("%02d", checkDigit);
    }

    private String generateAccountNumber() {
        long randomValue = random.nextLong();
        if (randomValue == Long.MIN_VALUE) { randomValue = 0; }
        long number = Math.abs(randomValue) % 10_000_000_000_000_000L;

        return String.format("%016d", number);
    }

    // Safe mod97
    private int mod97(String input) {
        int remainder = 0;

        for (int i = 0; i < input.length(); i++) {
            int digit = Character.getNumericValue(input.charAt(i));
            remainder = (remainder * 10 + digit) % 97;
        }

        return remainder;
    }
}
