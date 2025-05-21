package com.cercademiurentals.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean; // Comentado o eliminado
// import org.springframework.web.servlet.config.annotation.CorsRegistry; // Comentado o eliminado
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // Comentado o eliminado
// import org.springframework.lang.NonNull; // Solo si se mantiene el bean y se añade @NonNull

@SpringBootApplication
public class CercaDeMiURentalsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CercaDeMiURentalsApiApplication.class, args);
    }

    // *** CONFIGURACIÓN CORS COMENTADA/ELIMINADA DE AQUÍ ***
    // La configuración de CORS se manejará centralmente en SecurityConfig.java
    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) { // Añadido @NonNull si se mantuviera
                registry.addMapping("/api/**")
                        .allowedOrigins("*") 
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
    */
    // *** FIN CONFIGURACIÓN CORS COMENTADA/ELIMINADA ***
}