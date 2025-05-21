package com.cercademiurentals.api.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cercademiurentals.api.dto.LoginRequestDto;
import com.cercademiurentals.api.dto.UserProfileDto;
import com.cercademiurentals.api.dto.UserRegistrationDto;
import com.cercademiurentals.api.entity.UserEntity;
import com.cercademiurentals.api.repository.UserRepository;
import com.cercademiurentals.api.service.SparqlQueryService;
import com.cercademiurentals.api.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String BASE_USER_NAMESPACE = "http://www.example.org/cercademiurentals#usuario_";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SparqlQueryService sparqlQueryService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SparqlQueryService sparqlQueryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sparqlQueryService = sparqlQueryService;
    }

    @Override
    @Transactional
    public UserProfileDto registerNewUser(UserRegistrationDto registrationData) throws Exception {
        if (userRepository.existsByUsername(registrationData.getNombreUsuario())) {
            throw new Exception("El nombre de usuario '" + registrationData.getNombreUsuario() + "' ya está en uso.");
        }
        if (userRepository.existsByEmail(registrationData.getCorreo())) {
            throw new Exception("El correo electrónico '" + registrationData.getCorreo() + "' ya está registrado.");
        }

        UserEntity newUser = new UserEntity();
        String userIdPart = UUID.randomUUID().toString().substring(0, 10).replace("-","");
        newUser.setUserUri(BASE_USER_NAMESPACE + userIdPart);
        newUser.setUsername(registrationData.getNombreUsuario().trim());
        newUser.setEmail(registrationData.getCorreo().trim().toLowerCase());
        newUser.setPasswordHash(passwordEncoder.encode(registrationData.getPassword())); 
        newUser.setFirstName(registrationData.getNombres().trim());
        newUser.setLastName(registrationData.getApellidos().trim());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setActive(true);

        UserEntity savedUser = userRepository.save(newUser);
        logger.info("Usuario {} guardado en BD relacional. URI: {}", savedUser.getUsername(), savedUser.getUserUri());

        try {
            sparqlQueryService.createSemanticUserStub(savedUser.getUserUri(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
            logger.info("Stub semántico creado en Fuseki para usuario: {}", savedUser.getUserUri());
        } catch (Exception e) {
            logger.error("Error al crear el stub semántico para el usuario {} en Fuseki.", savedUser.getUserUri(), e);
            throw new Exception("Error al crear la representación semántica del usuario: " + e.getMessage(), e);
        }
        
        return convertToUserProfileDto(savedUser);
    }

    @Override
    public Optional<UserProfileDto> loginUser(LoginRequestDto loginData) throws Exception {
        Optional<UserEntity> userOptional = userRepository.findByUsername(loginData.getIdentifier());
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(loginData.getIdentifier());
        }

        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            if (!user.isActive()) {
                logger.warn("Intento de login para usuario inactivo: {}", loginData.getIdentifier());
                return Optional.empty(); 
            }
            
            updateUserLastLogin(user.getUsername(), LocalDateTime.now());
            return Optional.of(convertToUserProfileDto(user));
            
        }
        logger.warn("Usuario no encontrado para login (después de autenticación exitosa aparente): {}", loginData.getIdentifier());
        return Optional.empty();
    }
    
    @Override
    @Transactional 
    public void updateUserLastLogin(String username, LocalDateTime loginTime) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(loginTime);
            userRepository.save(user); 
            logger.info("Actualizado lastLogin para usuario: {}", username);
        });
    }

    @Override
    @Transactional(readOnly = true) // Es buena práctica marcar como readOnly si solo se lee
    public Optional<UserProfileDto> findUserProfileByUri(String userUri) {
        logger.info("UserServiceImpl - findUserProfileByUri - Buscando usuario con URI: '{}'", userUri);
        Optional<UserEntity> userEntityOptional = userRepository.findByUserUri(userUri);
        if (userEntityOptional.isEmpty()) {
            logger.warn("UserServiceImpl - findUserProfileByUri - Usuario NO ENCONTRADO en PostgreSQL para URI: '{}'", userUri);
        } else {
            logger.info("UserServiceImpl - findUserProfileByUri - Usuario ENCONTRADO en PostgreSQL para URI: '{}'. Convirtiendo a DTO.", userUri);
        }
        return userEntityOptional.map(this::convertToUserProfileDto);
    }
    
    @Override
    @Transactional(readOnly = true) // Es buena práctica marcar como readOnly si solo se lee
    public Optional<UserProfileDto> findUserProfileByUsername(String username) {
        logger.info("UserServiceImpl - findUserProfileByUsername - Buscando usuario con username: '{}'", username);
        Optional<UserEntity> userEntityOptional = userRepository.findByUsername(username);
         if (userEntityOptional.isEmpty()) {
            logger.warn("UserServiceImpl - findUserProfileByUsername - Usuario NO ENCONTRADO en PostgreSQL para username: '{}'", username);
        } else {
            logger.info("UserServiceImpl - findUserProfileByUsername - Usuario ENCONTRADO en PostgreSQL para username: '{}'. Convirtiendo a DTO.", username);
        }
        return userEntityOptional.map(this::convertToUserProfileDto);
    }

    private UserProfileDto convertToUserProfileDto(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        UserProfileDto dto = new UserProfileDto();
        dto.setUri(userEntity.getUserUri());                 
        dto.setNombreUsuario(userEntity.getUsername());      
        dto.setCorreo(userEntity.getEmail());                
        dto.setNombres(userEntity.getFirstName());           
        dto.setApellidos(userEntity.getLastName());          
        dto.setFechaCreacion(userEntity.getCreatedAt() != null ? userEntity.getCreatedAt().toString() : null);
        dto.setUltimoAcceso(userEntity.getLastLogin() != null ? userEntity.getLastLogin().toString() : null);
        dto.setEstaActivo(userEntity.isActive());

        if (userEntity.getUserUri() != null && !userEntity.getUserUri().isEmpty()) {
            try {
                logger.debug("Convirtiendo a DTO, obteniendo datos de Fuseki para URI: {}", userEntity.getUserUri());
                Optional<Float> ratingOpt = sparqlQueryService.findUserAverageRating(userEntity.getUserUri());
                ratingOpt.ifPresent(dto::setCalificacionPromedio);
                logger.debug("Calificación para {}: {}", userEntity.getUserUri(), ratingOpt.orElse(null));

                int anunciosCount = sparqlQueryService.countAnunciosByProviderUri(userEntity.getUserUri());
                dto.setCantidadAnunciosPublicados(anunciosCount);
                logger.debug("Cantidad anuncios para {}: {}", userEntity.getUserUri(), anunciosCount);

                int interesesCount = sparqlQueryService.countInteresesByUserUri(userEntity.getUserUri());
                dto.setCantidadInteresesMarcados(interesesCount);
                logger.debug("Cantidad intereses para {}: {}", userEntity.getUserUri(), interesesCount);

            } catch (Exception e) {
                logger.error("Error al obtener datos adicionales de Fuseki para el perfil de {}: {}", userEntity.getUserUri(), e.getMessage());
                 dto.setCalificacionPromedio(null); 
                 dto.setCantidadAnunciosPublicados(0);
                 dto.setCantidadInteresesMarcados(0);
            }
        } else {
            logger.warn("User URI es nulo o vacío para UserEntity con ID: {}, no se pueden obtener datos de Fuseki.", userEntity.getId());
             dto.setCalificacionPromedio(null);
             dto.setCantidadAnunciosPublicados(0);
             dto.setCantidadInteresesMarcados(0);
        }
        return dto;
    }
}