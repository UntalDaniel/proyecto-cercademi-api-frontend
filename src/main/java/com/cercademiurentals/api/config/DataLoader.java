package com.cercademiurentals.api.config;

import com.cercademiurentals.api.entity.UserEntity;
import com.cercademiurentals.api.repository.UserRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Component; // <--- COMENTADO PARA DESHABILITAR
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

// @Component // <--- ESTA LÍNEA ESTÁ COMENTADA PARA DESHABILITAR EL DATALOADER
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLoader resourceLoader;

    private static final String CERCA_NS = "http://www.example.org/cercademiurentals#";
    private static final String TEMP_PASSWORD = "temporal123";

    @Autowired
    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder, ResourceLoader resourceLoader) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("DataLoader está DESHABILITADO (anotación @Component comentada). No se cargarán usuarios desde TTL.");
        // El resto del código está aquí pero no se ejecutará si @Component está comentado.
        if (true) return; // Salida temprana para asegurar que no se ejecute si se descomenta por error sin quererlo.

        logger.info("Iniciando DataLoader para cargar usuarios desde TTL a PostgreSQL...");

        try {
            org.springframework.core.io.Resource ttlFile = resourceLoader.getResource("classpath:cerca_individuos_v3.ttl");
            if (!ttlFile.exists()) {
                logger.warn("El archivo cerca_individuos_v3.ttl no se encontró en classpath:src/main/resources. No se cargarán usuarios de ejemplo.");
                return;
            }

            InputStream in = ttlFile.getInputStream();
            Model model = ModelFactory.createDefaultModel();
            model.read(in, null, "TTL"); 

            Resource userType = model.getResource(CERCA_NS + "Usuario");
            org.apache.jena.rdf.model.Property pUsername = model.getProperty(CERCA_NS + "tieneNombreUsuario");
            org.apache.jena.rdf.model.Property pEmail = model.getProperty(CERCA_NS + "tieneCorreo");
            org.apache.jena.rdf.model.Property pFirstName = model.getProperty(CERCA_NS + "tieneNombres");
            org.apache.jena.rdf.model.Property pLastName = model.getProperty(CERCA_NS + "tieneApellidos");
            org.apache.jena.rdf.model.Property pCreatedAt = model.getProperty(CERCA_NS + "fechaCreacion");
            org.apache.jena.rdf.model.Property pIsActive = model.getProperty(CERCA_NS + "estaActivo");

            ResIterator iter = model.listSubjectsWithProperty(RDF.type, userType);
            int usersProcessed = 0;
            int usersCreated = 0;
            int usersSkippedOnError = 0;

            while (iter.hasNext()) {
                Resource userResource = iter.nextResource();
                String userUri = userResource.getURI(); 
                usersProcessed++;

                Optional<UserEntity> existingUser = userRepository.findByUserUri(userUri);

                if (existingUser.isEmpty()) {
                    UserEntity newUser = new UserEntity();
                    newUser.setUserUri(userUri);

                    if (userResource.hasProperty(pUsername)) {
                        newUser.setUsername(userResource.getProperty(pUsername).getString());
                    } else {
                        logger.warn("Usuario en TTL con URI {} no tiene propiedad tieneNombreUsuario. Omitiendo.", userUri);
                        usersSkippedOnError++;
                        continue; 
                    }

                    if (userResource.hasProperty(pEmail)) {
                        newUser.setEmail(userResource.getProperty(pEmail).getString());
                    } else {
                         logger.warn("Usuario en TTL con URI {} no tiene propiedad tieneCorreo. Omitiendo.", userUri);
                        usersSkippedOnError++;
                        continue; 
                    }
                    
                    newUser.setPasswordHash(passwordEncoder.encode(TEMP_PASSWORD));

                    if (userResource.hasProperty(pFirstName)) {
                        newUser.setFirstName(userResource.getProperty(pFirstName).getString());
                    }
                    if (userResource.hasProperty(pLastName)) {
                        newUser.setLastName(userResource.getProperty(pLastName).getString());
                    }

                    if (userResource.hasProperty(pCreatedAt)) {
                        String createdAtStr = userResource.getProperty(pCreatedAt).getString();
                        try {
                            if (createdAtStr.endsWith("Z")) {
                                newUser.setCreatedAt(ZonedDateTime.parse(createdAtStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime());
                            } else {
                                newUser.setCreatedAt(LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME));
                            }
                        } catch (DateTimeParseException e) {
                            logger.warn("No se pudo parsear fechaCreacion '{}' para usuario {}. Usando now(). Error: {}", createdAtStr, userUri, e.getMessage());
                            newUser.setCreatedAt(LocalDateTime.now());
                        }
                    } else {
                        newUser.setCreatedAt(LocalDateTime.now());
                    }

                    if (userResource.hasProperty(pIsActive)) {
                        Statement activeStatement = userResource.getProperty(pIsActive);
                        if (activeStatement.getObject().isLiteral()) {
                            newUser.setActive(activeStatement.getBoolean());
                        } else {
                             logger.warn("Valor de estaActivo para {} no es un literal booleano. Usando 'true' por defecto.", userUri);
                            newUser.setActive(true);
                        }
                    } else {
                        newUser.setActive(true); 
                    }
                    
                    try {
                        userRepository.save(newUser);
                        usersCreated++;
                        logger.info("Usuario de TTL cargado en PostgreSQL: URI={}, Username={}", newUser.getUserUri(), newUser.getUsername());
                    } catch (Exception e) {
                        logger.error("Error al guardar usuario de TTL con URI {} en PostgreSQL: {}", userUri, e.getMessage(), e);
                        usersSkippedOnError++;
                    }
                } else {
                    logger.info("Usuario de TTL con URI {} ya existe en PostgreSQL. Omitiendo.", userUri);
                }
            }
            logger.info("DataLoader (corrida lógica) finalizado. {} usuarios de TTL procesados, {} nuevos usuarios creados en PostgreSQL, {} usuarios omitidos por errores individuales.", usersProcessed, usersCreated, usersSkippedOnError);
            if (usersCreated > 0) {
                logger.info("Contraseña temporal para usuarios cargados desde TTL: '{}'", TEMP_PASSWORD);
            }

        } catch (Exception e) {
            logger.error("Error CRÍTICO durante la ejecución de DataLoader: {}", e.getMessage(), e);
        }
    }
}
