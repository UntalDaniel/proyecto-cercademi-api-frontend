package com.cercademiurentals.api.service;

import java.time.LocalDateTime; // Importar LocalDateTime
import java.util.Optional;

import com.cercademiurentals.api.dto.LoginRequestDto;
import com.cercademiurentals.api.dto.UserProfileDto;
import com.cercademiurentals.api.dto.UserRegistrationDto;
// Quitar la importación de UserEntity si registerNewUser devuelve UserProfileDto
// import com.cercademiurentals.api.entity.UserEntity; 

public interface UserService {

    /**
     * Registra un nuevo usuario en el sistema.
     * @param registrationData DTO con los datos de registro.
     * @return El perfil del usuario creado. // Cambiado tipo de retorno
     * @throws Exception si el nombre de usuario o correo ya existen, o si ocurre un error.
     */
    // Cambiado el tipo de retorno a UserProfileDto
    UserProfileDto registerNewUser(UserRegistrationDto registrationData) throws Exception;

    /**
     * Autentica a un usuario.
     * (La autenticación real la manejará Spring Security con UserDetailsService).
     * Este método podría usarse para obtener la URI del usuario después de una autenticación exitosa.
     * @param loginData DTO con los datos de login.
     * @return Optional con la UserProfileDto si las credenciales son válidas y el usuario está activo. // Cambiado tipo de retorno
     * @throws Exception si ocurre un error.
     */
    // Opcional: Podrías cambiar el retorno a Optional<UserProfileDto> si es más conveniente para el login
    Optional<UserProfileDto> loginUser(LoginRequestDto loginData) throws Exception;


    /**
     * Encuentra un perfil de usuario por su URI.
     * @param userUri La URI del usuario.
     * @return Un Optional que contiene el UserProfileDto si se encuentra.
     */
    Optional<UserProfileDto> findUserProfileByUri(String userUri);

    /**
     * Encuentra un perfil de usuario por su nombre de usuario.
     * @param username El nombre de usuario.
     * @return Un Optional que contiene el UserProfileDto si se encuentra.
     */
    Optional<UserProfileDto> findUserProfileByUsername(String username);
    
    /**
     * Actualiza el campo lastLogin del usuario.
     * @param username El nombre de usuario.
     * @param loginTime La hora del login. // Nuevo parámetro
     */
    // Corregido: Añadido parámetro LocalDateTime
    void updateUserLastLogin(String username, LocalDateTime loginTime);

}
