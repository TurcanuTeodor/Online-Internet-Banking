package ro.app.fraud;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test de integrare: verifică că contextul Spring Boot pornește complet.
 * Dezactivat implicit deoarece necesită PostgreSQL activ.
 *
 * Pentru a rula local, setează variabila de mediu INTEGRATION_TESTS=true
 * și asigură-te că baza de date este disponibilă.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FraudServiceApplicationTests {

    @Before
    public void checkEnvironment() {
        Assume.assumeTrue("INTEGRATION_TESTS must be 'true'",
                "true".equals(System.getenv("INTEGRATION_TESTS")));
    }

    @Test
    public void contextLoads() {
        // Verifică că toate bean-urile Spring se injectează corect
    }
}
