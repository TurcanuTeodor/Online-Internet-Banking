package ro.app.backend_Java_SpringBoot;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class BackendJavaSpringBootApplicationTests {

	  @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context, "ApplicationContext should be started");
    }

}
