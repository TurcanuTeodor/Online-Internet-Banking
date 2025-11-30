package ro.app.backend_Java_SpringBoot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class BackendJavaSpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendJavaSpringBootApplication.class, args);
	}

}
