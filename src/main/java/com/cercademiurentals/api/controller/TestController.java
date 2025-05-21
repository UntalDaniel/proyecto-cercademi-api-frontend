package com.cercademiurentals.api.controller; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired; // Eliminado
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cercademiurentals.api.service.SparqlQueryService;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    private final SparqlQueryService sparqlService; 

    // @Autowired // Eliminado ya que es opcional con un solo constructor
    public TestController(SparqlQueryService sparqlService) {
        this.sparqlService = sparqlService;
        if (this.sparqlService == null) {
            logger.error("¡¡¡ERROR CRÍTICO!!! SparqlQueryService NO fue inyectado en TestController.");
        } else {
            logger.info("TestController instanciado: SparqlQueryService INYECTADO correctamente.");
        }
    }

    @GetMapping("/count-users")
    public String testUserCount() {
        if (sparqlService == null) {
            logger.error("SparqlQueryService es null en testUserCount. La inyección falló.");
            return "Error: SparqlQueryService no está disponible.";
        }
        try {
            int count = sparqlService.countUsers();
            return "Número de usuarios encontrados en Fuseki: " + count;
        } catch (Exception e) {
            logger.error("Error al ejecutar countUsers en TestController", e);
            return "Error al contar usuarios: " + e.getMessage();
        }
    }
}
