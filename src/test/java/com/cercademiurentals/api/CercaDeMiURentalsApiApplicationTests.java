package com.cercademiurentals.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles; // Importar

@SpringBootTest
@ActiveProfiles("test") // Activar el perfil "test"
class CercaDeMiURentalsApiApplicationTests {

	@Test
	void contextLoads() {
		// Este test simplemente verifica si el contexto de la aplicación Spring se carga correctamente.
		// Con la configuración de application-test.properties, debería poder conectarse a la BD.
	}

}
