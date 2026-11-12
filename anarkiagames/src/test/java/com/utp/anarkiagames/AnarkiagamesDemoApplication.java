package com.utp.anarkiagames;

import org.springframework.boot.SpringApplication;

import java.util.Map;

/**
 * Backend de demostración con datos de ejemplo en H2 en memoria, para probar el frontend.
 * Se ejecuta con {@code ./mvnw spring-boot:test-run}; al detenerlo, los datos se pierden.
 */
public class AnarkiagamesDemoApplication {

    // Las propiedades de sistema tienen prioridad sobre el .env, los perfiles y application.properties,
    // así la demo nunca puede conectarse a la BD real.
    private static final Map<String, String> BD_EN_MEMORIA = Map.of(
            "spring.datasource.url", "jdbc:h2:mem:anarkiagames-demo;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name", "org.h2.Driver",
            "spring.datasource.username", "sa",
            "spring.datasource.password", "",
            "spring.flyway.enabled", "false",
            "spring.jpa.hibernate.ddl-auto", "create-drop");

    public static void main(String[] args) {
        BD_EN_MEMORIA.forEach(System::setProperty);
        SpringApplication.from(AnarkiagamesApplication::main)
                .withAdditionalProfiles("demo")
                .run(args);
    }
}
