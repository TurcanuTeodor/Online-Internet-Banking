package ro.app.fraud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de integrare: verifică că contextul Spring Boot pornește complet.
 * Dezactivat implicit deoarece necesită PostgreSQL activ.
 *
 * Pentru a rula local, setează variabila de mediu INTEGRATION_TESTS=true
 * și asigură-te că baza de date este disponibilă.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TESTS", matches = "true")
class FraudServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifică că toate bean-urile Spring se injectează corect
    }
}
