package ro.app.fraud.tier3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Citeste dataset-ul PaySim din fisier CSV local
 *
 * DE CE sub-sampling la 150.000 de linii?
 * PaySim are ~6.3 milioane de randuri. Daca antrenezi pe toate,
 * fenomenul de "masking" apare: modelul vede atat de multe tranzactii
 * normale incat cele frauduloase devin "invizibile" statistic.
 * 150.000 de linii = ~2.4% din dataset, suficient pentru a capta
 * distributia fara a sacrifica precizia.
 *
 * Structura CSV PaySim (header exact):
 * step,type,amount,nameOrig,oldbalanceOrg,newbalanceOrig,
 * nameDest,oldbalanceDest,newbalanceDest,isFraud,isFlaggedFraud
 *
 */
public final class PaySimCsvReader {

    private static final Logger log = LoggerFactory.getLogger(PaySimCsvReader.class);

    // Indecsii coloanelor in CSV (0-based)
    private static final int COL_STEP = 0;
    private static final int COL_TYPE = 1;
    private static final int COL_AMOUNT = 2;
    // COL_NAME_ORIG = 3 (ignorat - nu folosim nume)
    private static final int COL_OLD_BAL_ORG = 4;
    private static final int COL_NEW_BAL_ORIG = 5;
    // COL_NAME_DEST = 6 (ignorat)
    private static final int COL_OLD_BAL_DEST = 7;
    private static final int COL_NEW_BAL_DEST = 8;
    private static final int COL_IS_FRAUD = 9;
    // COL_IS_FLAGGED_FRAUD = 10 (ignorat - generat de reguli banale)

    private PaySimCsvReader() {
    }

    public static List<PaySimRow> read(String csvPath, int maxRows) throws IOException {
        List<PaySimRow> rows = new ArrayList<>(maxRows);

        int lineNum = 1; // 1 = prima linie de date (după header)
        int errors = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine(); // consuma header-ul, nu il procesam
            if (header == null) {
                throw new IOException("PaySim CSV is empty: " + csvPath);
            }
            log.info("PaySim CSV header: {}", header);

            String line;
            while ((line = br.readLine()) != null && rows.size() < maxRows) {
                lineNum++;
                try {
                    PaySimRow row = parseLine(line);
                    rows.add(row);
                } catch (Exception e) {
                    errors++;
                    if (errors <= 5) {
                        log.warn("Line {} ignored (parsing error): {}", lineNum, e.getMessage());
                    }
                }
            }
        }

        log.info("PaySim CSV read: {} valid rows (requested: {})", rows.size(), maxRows);

        if (errors > 0) {
            log.warn("PaySim CSV: {} linii ignorate din cauza erorilor de parsing (din ~{} citite)",
                    errors, rows.size() + errors);
        }

        return rows;
    }

    private static PaySimRow parseLine(String line) {
        // PaySim nu are campuri cu virgula interna -> split direct
        String[] cols = line.split(",", -1);

        if (cols.length < 10) {
            throw new IllegalArgumentException("Too few columns: " + cols.length);
        }

        return new PaySimRow(
                Integer.parseInt(cols[COL_STEP].trim()),
                cols[COL_TYPE].trim(),
                Double.parseDouble(cols[COL_AMOUNT].trim()),
                Double.parseDouble(cols[COL_OLD_BAL_ORG].trim()),
                Double.parseDouble(cols[COL_NEW_BAL_ORIG].trim()),
                Double.parseDouble(cols[COL_OLD_BAL_DEST].trim()),
                Double.parseDouble(cols[COL_NEW_BAL_DEST].trim()),
                Integer.parseInt(cols[COL_IS_FRAUD].trim()));
    }
}
