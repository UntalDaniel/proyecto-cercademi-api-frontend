package com.cercademiurentals.api.controller;

import java.net.URI;
import java.util.List;
import java.util.Map; 
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cercademiurentals.api.dto.AnuncioCreateDto;
import com.cercademiurentals.api.dto.AnuncioDetalleDto;
import com.cercademiurentals.api.dto.AnuncioSummaryDto;
import com.cercademiurentals.api.dto.AnuncioUpdateDto;
import com.cercademiurentals.api.dto.PuntoDeInteresDto;
import com.cercademiurentals.api.dto.UserProfileDto; // Necesario para obtener userUri
import com.cercademiurentals.api.service.SparqlQueryService;
import com.cercademiurentals.api.service.UserService; 

@RestController
@RequestMapping("/api")
public class AnuncioController {

    private static final Logger logger = LoggerFactory.getLogger(AnuncioController.class);

    private final SparqlQueryService sparqlService;
    private final UserService userService; 

    // @Autowired // Opcional si solo hay un constructor
    public AnuncioController(SparqlQueryService sparqlService, UserService userService) {
        this.sparqlService = sparqlService;
        this.userService = userService; 
        
        if (this.sparqlService == null) {
            logger.error("CONSTRUCTOR: SparqlQueryService NO fue inyectado en AnuncioController.");
        } else {
            logger.info("CONSTRUCTOR: SparqlQueryService INYECTADO correctamente en AnuncioController.");
        }
        if (this.userService == null) {
            logger.error("CONSTRUCTOR: UserService NO fue inyectado en AnuncioController.");
        } else {
            logger.info("CONSTRUCTOR: UserService INYECTADO correctamente en AnuncioController.");
        }
    }

    @GetMapping("/viviendas")
    public ResponseEntity<List<AnuncioSummaryDto>> getAvailableAnuncios(
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String tipoVivienda,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) Integer habMin, 
            @RequestParam(name = "comodidades", required = false) List<String> comodidades,
            @RequestParam(required = false) String pdiUri,
            @RequestParam(required = false) Double distMaxKm) {
        
        logger.info("GET /viviendas: searchText={}, tipoVivienda={}, precioMax={}, habMin={}, comodidades={}, pdiUri={}, distMaxKm={}",
                searchText, tipoVivienda, precioMax, habMin, comodidades, pdiUri, distMaxKm);
        
        if (this.sparqlService == null) {
            logger.error("ERROR EN getAvailableAnuncios: this.sparqlService ES NULL.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        try {
            List<AnuncioSummaryDto> anuncios = sparqlService.findAvailableAnuncios(searchText, tipoVivienda, precioMax, habMin, comodidades, pdiUri, distMaxKm);
            return ResponseEntity.ok(anuncios);
        } catch (Exception e) {
            logger.error("Error en AnuncioController.getAvailableAnuncios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/puntos-de-interes")
    public ResponseEntity<List<PuntoDeInteresDto>> getPuntosDeInteres() {
        logger.info("GET /puntos-de-interes: Solicitud recibida.");
        if (this.sparqlService == null) {
            logger.error("ERROR EN getPuntosDeInteres: this.sparqlService ES NULL.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        try {
            List<PuntoDeInteresDto> pdis = sparqlService.findPuntosDeInteres();
            return ResponseEntity.ok(pdis);
        } catch (Exception e) {
            logger.error("Error crítico al obtener los puntos de interés: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/anuncios/{idAnuncio}")
    public ResponseEntity<?> getAnuncioDetalle(@PathVariable String idAnuncio) { // El tipo de retorno es ResponseEntity<?>
        logger.info("GET /anuncios/{}: Solicitud recibida.", idAnuncio);
        String anuncioUri = "http://www.example.org/cercademiurentals#anuncio_" + idAnuncio;
        if (this.sparqlService == null) {
             logger.error("ERROR EN getAnuncioDetalle: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            Optional<AnuncioDetalleDto> anuncioOpt = sparqlService.findAnuncioDetailsByUri(anuncioUri);
            if (anuncioOpt.isPresent()) {
                return ResponseEntity.ok(anuncioOpt.get()); // Devuelve ResponseEntity<AnuncioDetalleDto>
            } else {
                logger.warn("Anuncio no encontrado con URI: {}", anuncioUri);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Anuncio no encontrado."); // Devuelve ResponseEntity<String>
            }
        } catch (Exception e) {
            logger.error("Error al obtener detalles del anuncio {}: {}", anuncioUri, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la solicitud del anuncio.");
        }
    }

    @PostMapping("/anuncios")
    public ResponseEntity<?> createAnuncio(@RequestBody AnuncioCreateDto anuncioDto, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        String usernameFromAuth = ((UserDetails) authentication.getPrincipal()).getUsername();
        Optional<UserProfileDto> userProfileOpt = userService.findUserProfileByUsername(usernameFromAuth);

        if (userProfileOpt.isEmpty() || userProfileOpt.get().getUri() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo obtener la URI del proveedor autenticado.");
        }
        anuncioDto.setProviderUri(userProfileOpt.get().getUri());
        
        logger.info("POST /anuncios: Creando anuncio '{}' por usuario {}", anuncioDto.getTitulo(), userProfileOpt.get().getUri());
        if (this.sparqlService == null) {
             logger.error("ERROR EN createAnuncio: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            String nuevoAnuncioUri = sparqlService.createAnuncio(anuncioDto); 
            return ResponseEntity.created(URI.create("/api/anuncios/" + nuevoAnuncioUri.substring(nuevoAnuncioUri.lastIndexOf('_') + 1)))
                                 .body(Map.of("message", "Anuncio creado exitosamente.", "uri", nuevoAnuncioUri));
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al crear anuncio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error al crear anuncio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al crear el anuncio.");
        }
    }

    @PutMapping("/anuncios/{idAnuncio}")
    public ResponseEntity<?> updateAnuncio(@PathVariable String idAnuncio, @RequestBody AnuncioUpdateDto anuncioUpdateDto, Authentication authentication) {
        String anuncioUri = "http://www.example.org/cercademiurentals#anuncio_" + idAnuncio;
        logger.info("PUT /anuncios/{}: Actualizando anuncio URI: {}", idAnuncio, anuncioUri);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN updateAnuncio: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            // Considerar pasar el userUri del principal para validación de propiedad en el servicio
            // String authenticatedUserUri = userService.findUserProfileByUsername(((UserDetails) authentication.getPrincipal()).getUsername()).map(UserProfileDto::getUri).orElse(null);
            // boolean actualizado = sparqlService.updateAnuncio(anuncioUri, anuncioUpdateDto, authenticatedUserUri);
            boolean actualizado = sparqlService.updateAnuncio(anuncioUri, anuncioUpdateDto); 
            if (actualizado) {
                return ResponseEntity.ok(Map.of("message", "Anuncio actualizado exitosamente.", "uri", anuncioUri));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Anuncio no encontrado o no se pudo actualizar (posiblemente no es el propietario).");
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al actualizar anuncio {}: {}", anuncioUri, e.getMessage());
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (SecurityException e) { 
            logger.warn("Intento no autorizado de actualizar anuncio {}: {}", anuncioUri, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error al actualizar anuncio {}: {}", anuncioUri, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al actualizar el anuncio.");
        }
    }

    @DeleteMapping("/anuncios/{idAnuncio}")
    public ResponseEntity<?> deleteAnuncio(@PathVariable String idAnuncio, Authentication authentication) {
        String anuncioUri = "http://www.example.org/cercademiurentals#anuncio_" + idAnuncio;
        logger.info("DELETE /anuncios/{}: Eliminando anuncio URI: {}", idAnuncio, anuncioUri);
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN deleteAnuncio: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            // Considerar pasar el userUri del principal a deleteAnuncioByUri para que verifique la propiedad.
            sparqlService.deleteAnuncioByUri(anuncioUri); 
            return ResponseEntity.ok(Map.of("message", "Solicitud de eliminación procesada para el anuncio.", "uri", anuncioUri));
        } catch (SecurityException e) {
            logger.warn("Intento no autorizado de eliminar anuncio {}: {}", anuncioUri, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) { 
            logger.error("Error al eliminar anuncio {}: {}", anuncioUri, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al eliminar el anuncio.");
        }
    }

    @GetMapping("/mis-anuncios")
    public ResponseEntity<?> getMyAnuncios(Authentication authentication) { 
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        String usernameFromAuth = ((UserDetails) authentication.getPrincipal()).getUsername();
        Optional<UserProfileDto> userProfileOpt = userService.findUserProfileByUsername(usernameFromAuth);

        if (userProfileOpt.isEmpty() || userProfileOpt.get().getUri() == null) {
            logger.warn("No se pudo obtener la URI para el usuario autenticado: {}", usernameFromAuth);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo identificar al usuario para obtener sus anuncios.");
        }
        String userUriFromToken = userProfileOpt.get().getUri();
        logger.info("GET /mis-anuncios: Solicitud para usuario con URI: {}", userUriFromToken);
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN getMyAnuncios: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            List<AnuncioSummaryDto> anuncios = sparqlService.findAnunciosByProviderUri(userUriFromToken);
            return ResponseEntity.ok(anuncios);
        } catch (Exception e) {
            logger.error("Error al obtener los anuncios del usuario {}: {}", userUriFromToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener los anuncios del usuario.");
        }
    }
    
    @PostMapping("/anuncios/{idAnuncio}/interes")
    public ResponseEntity<?> markAnuncioAsInterested(@PathVariable String idAnuncio, Authentication authentication) { 
        String anuncioUriCompleta = "http://www.example.org/cercademiurentals#anuncio_" + idAnuncio;
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        String usernameFromAuth = ((UserDetails) authentication.getPrincipal()).getUsername();
        Optional<UserProfileDto> userProfileOpt = userService.findUserProfileByUsername(usernameFromAuth);

        if (userProfileOpt.isEmpty() || userProfileOpt.get().getUri() == null) {
            logger.warn("No se pudo obtener la URI para el usuario autenticado: {}", usernameFromAuth);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo identificar al usuario para marcar interés.");
        }
        String userUriFromToken = userProfileOpt.get().getUri();
        logger.info("POST /anuncios/{}/interes: Usuario {} marcando interés.", idAnuncio, userUriFromToken);
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN markAnuncioAsInterested: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            sparqlService.markAnuncioAsInterested(userUriFromToken, anuncioUriCompleta);
            return ResponseEntity.ok().body("Anuncio " + anuncioUriCompleta + " marcado como interesante para el usuario " + userUriFromToken + ".");
        } catch (Exception e) {
            logger.error("Error en AnuncioController.markAnuncioAsInterested para ID {}, user {}: {}", idAnuncio, userUriFromToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno al intentar marcar el anuncio como interesante.");
        }
    }

    @DeleteMapping("/anuncios/{idAnuncio}/interes") 
    public ResponseEntity<?> removeAnuncioInterest(@PathVariable String idAnuncio, Authentication authentication) {
        String anuncioUriCompleta = "http://www.example.org/cercademiurentals#anuncio_" + idAnuncio;
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        String usernameFromAuth = ((UserDetails) authentication.getPrincipal()).getUsername();
        Optional<UserProfileDto> userProfileOpt = userService.findUserProfileByUsername(usernameFromAuth);

        if (userProfileOpt.isEmpty() || userProfileOpt.get().getUri() == null) {
            logger.warn("No se pudo obtener la URI para el usuario autenticado: {}", usernameFromAuth);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo identificar al usuario para quitar interés.");
        }
        String userUriFromToken = userProfileOpt.get().getUri();
        logger.info("DELETE /anuncios/{}/interes: Usuario {} quitando interés.", idAnuncio, userUriFromToken);
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN removeAnuncioInterest: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            sparqlService.removeAnuncioInterest(userUriFromToken, anuncioUriCompleta); 
            return ResponseEntity.ok().body("Interés en anuncio " + anuncioUriCompleta + " eliminado para el usuario " + userUriFromToken + ".");
        } catch (Exception e) {
            logger.error("Error en AnuncioController.removeAnuncioInterest para ID {}, user {}: {}", idAnuncio, userUriFromToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno al intentar quitar el interés del anuncio.");
        }
    }


    @GetMapping("/mis-intereses")
    public ResponseEntity<?> getMyInterestedAnuncios(Authentication authentication) { 
         if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        String usernameFromAuth = ((UserDetails) authentication.getPrincipal()).getUsername();
        Optional<UserProfileDto> userProfileOpt = userService.findUserProfileByUsername(usernameFromAuth);

        if (userProfileOpt.isEmpty() || userProfileOpt.get().getUri() == null) {
            logger.warn("No se pudo obtener la URI para el usuario autenticado: {}", usernameFromAuth);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo identificar al usuario para obtener sus intereses.");
        }
        String userUriFromToken = userProfileOpt.get().getUri();
        logger.info("GET /mis-intereses: Solicitud para usuario con URI: {}", userUriFromToken);
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN getMyInterestedAnuncios: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            List<AnuncioSummaryDto> anunciosInteresantes = sparqlService.findInterestedAnunciosByUri(userUriFromToken);
            return ResponseEntity.ok(anunciosInteresantes);
        } catch (Exception e) {
            logger.error("Error en AnuncioController.getMyInterestedAnuncios para user {}: {}", userUriFromToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno al obtener los anuncios de interés.");
        }
    }

     @PutMapping("/anuncios/{idAnuncio}/estado")
    public ResponseEntity<?> updateAnuncioState(@PathVariable String idAnuncio, 
                                                @RequestBody Map<String, String> payload, 
                                                Authentication authentication) {
        String anuncioUri = "http://www.example.org/cercademiurentals#anuncio_" + idAnuncio;
        String nuevoEstado = payload.get("estado");

        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nuevo estado no puede estar vacío.");
        }
        logger.info("PUT /anuncios/{}/estado: Actualizando estado a '{}' para URI: {}", idAnuncio, nuevoEstado, anuncioUri);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado.");
        }
        // Aquí también deberías verificar si el usuario autenticado es el propietario del anuncio.
        
        if (this.sparqlService == null) {
             logger.error("ERROR EN updateAnuncioState: this.sparqlService ES NULL.");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor (servicio nulo).");
        }
        try {
            sparqlService.updateAnuncioState(anuncioUri, nuevoEstado.trim());
            return ResponseEntity.ok(Map.of("message", "Estado del anuncio actualizado exitosamente.", "uri", anuncioUri, "nuevoEstado", nuevoEstado.trim()));
        } catch (SecurityException e) {
            logger.warn("Intento no autorizado de actualizar estado del anuncio {}: {}", anuncioUri, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error al actualizar estado del anuncio {}: {}", anuncioUri, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al actualizar el estado del anuncio.");
        }
    }
}