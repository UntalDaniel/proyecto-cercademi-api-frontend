package com.cercademiurentals.api.controller;

import com.cercademiurentals.api.dto.LoginRequestDto;
import com.cercademiurentals.api.dto.UserProfileDto;
import com.cercademiurentals.api.dto.UserRegistrationDto;
import com.cercademiurentals.api.dto.JwtAuthenticationResponse;
import com.cercademiurentals.api.security.jwt.JwtTokenProvider;
import com.cercademiurentals.api.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public UserController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto registrationDto) {
        logger.info("Intentando registrar usuario: {}", registrationDto.getNombreUsuario());
        try {
            UserProfileDto userProfile = userService.registerNewUser(registrationDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(userProfile);
        } catch (IllegalArgumentException e) {
            logger.error("Error de validación durante el registro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) { 
            logger.error("Error durante el registro: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequestDto loginRequest) {
        logger.info("Intento de login para usuario/email: {}", loginRequest.getIdentifier());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getIdentifier(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("Usuario {} autenticado correctamente.", loginRequest.getIdentifier());

            String jwt = tokenProvider.generateToken(authentication);
            logger.debug("Token JWT generado para {}: {}", loginRequest.getIdentifier(), jwt);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String usernameFromAuthentication = userDetails.getUsername(); 

            userService.updateUserLastLogin(usernameFromAuthentication, LocalDateTime.now());

            Optional<UserProfileDto> userProfileOptional = userService.findUserProfileByUsername(usernameFromAuthentication);
            
            if (userProfileOptional.isEmpty()) {
                logger.error("No se pudo encontrar el perfil del usuario {} después del login.", usernameFromAuthentication);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo obtener el perfil del usuario después del login.");
            }
            UserProfileDto userProfile = userProfileOptional.get();
            
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, userProfile));

        } catch (AuthenticationException e) {
            logger.warn("Fallo de autenticación para {}: {}", loginRequest.getIdentifier(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error de autenticación: Credenciales inválidas.");
        } catch (Exception e) {
            logger.error("Error inesperado durante el login para {}: {}", loginRequest.getIdentifier(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor durante el login.");
        }
    }

    @GetMapping("/yo")
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Intento de acceso a /yo sin autenticación válida.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado para acceder a este recurso.");
        }

        Object principal = authentication.getPrincipal();
        String usernameFromAuthentication;

        if (principal instanceof UserDetails) {
            usernameFromAuthentication = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            usernameFromAuthentication = (String) principal;
        } else {
            logger.error("Tipo de principal desconocido en /yo: {}", principal.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la identidad del usuario.");
        }
        
        logger.info("Obteniendo perfil para el usuario autenticado: {}", usernameFromAuthentication);
        try {
            Optional<UserProfileDto> userProfileOptional = userService.findUserProfileByUsername(usernameFromAuthentication);
            
            if (userProfileOptional.isEmpty()) {
                logger.warn("No se encontró perfil para el usuario autenticado: {}", usernameFromAuthentication);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Perfil de usuario no encontrado.");
            }
            return ResponseEntity.ok(userProfileOptional.get());
        } catch (Exception e) {
            logger.error("Error al obtener el perfil del usuario {}: {}", usernameFromAuthentication, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener el perfil del usuario.");
        }
    }

    @GetMapping("/publico")
    public ResponseEntity<?> getPublicUserProfile(@RequestParam String userUri) { 
        logger.info("UserController - /publico - userUri recibido como @RequestParam: '{}'", userUri);

        if (userUri == null || userUri.trim().isEmpty()) {
            logger.warn("Solicitud a /publico sin userUri o con userUri vacío.");
            return ResponseEntity.badRequest().body("Parámetro userUri es requerido y no puede estar vacío.");
        }
        
        Optional<UserProfileDto> userProfileOpt = userService.findUserProfileByUri(userUri);

        if (userProfileOpt.isEmpty()) {
            logger.warn("Perfil de usuario NO encontrado por el servicio para URI: {}", userUri);
            // Ya no hay fallback a Fuseki, directamente 404.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Perfil de usuario no encontrado para URI: " + userUri);
        }

        logger.info("Perfil público encontrado para URI: {}. Preparando respuesta.", userUri);
        UserProfileDto publicProfile = userProfileOpt.get(); 
        // El UserProfileDto ya debería estar "limpio" de datos sensibles si es necesario,
        // o el servicio ya devuelve un DTO adecuado para vista pública.
        // Si userService.findUserProfileByUri devuelve un DTO con datos sensibles,
        // aquí se debería crear un nuevo DTO solo con campos públicos.
        // Por ahora, asumimos que userService.findUserProfileByUri ya maneja esto.
        
        return ResponseEntity.ok(publicProfile);
    }
}
