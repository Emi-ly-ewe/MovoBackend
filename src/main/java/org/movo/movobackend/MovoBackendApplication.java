package org.movo.movobackend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableJpaRepositories("org.movo.movobackend.repository")
@EntityScan("org.movo.movobackend.model.entity")
public class MovoBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovoBackendApplication.class, args);
    }
}
