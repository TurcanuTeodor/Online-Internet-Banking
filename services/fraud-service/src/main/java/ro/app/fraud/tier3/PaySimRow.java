package ro.app.fraud.tier3;

/**
 * 
 * DE CE record?
 *   - Imutabil prin design (fraud data nu se modifica dupa citire)
 *   - Compact: Java genereaza automat constructor, getteri, equals, hashCode
 *   - Clar semantic: fiecare camp are un nume exact din domeniu
 */
public record PaySimRow(
        int step,             // ora simulata (1 step = 1 ora din cele 744 ale lunii)
        String type,             // PAYMENT | TRANSFER | CASH_OUT | DEBIT | CASH_IN
        double amount,           // suma tranzactiei (in moneda simulata)
        double oldbalanceOrg,    // balanta sender INAINTE de tranzactie
        double newbalanceOrig,   // balanta sender DUPA tranzactie
        double oldbalanceDest,   // balanta receiver INAINTE
        double newbalanceDest,   // balanta receiver DUPA
        int isFraud           // 0 = legitima, 1 = frauda (ground truth pentru antrenament)
) {}
