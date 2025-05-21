package com.cercademiurentals.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cercademiurentals.api.entity.UserEntity;

/**
 * Repositorio JPA para la entidad UserEntity.
 * Proporciona métodos CRUD (Crear, Leer, Actualizar, Eliminar) y
 * la capacidad de definir consultas personalizadas.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Busca un usuario por su nombre de usuario.
     * @param username El nombre de usuario a buscar.
     * @return Un Optional que contiene el UserEntity si se encuentra, o vacío si no.
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Busca un usuario por su dirección de correo electrónico.
     * @param email El correo electrónico a buscar.
     * @return Un Optional que contiene el UserEntity si se encuentra, o vacío si no.
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Busca un usuario por su URI (la que se usa para enlazar con Fuseki).
     * @param userUri La URI del usuario a buscar.
     * @return Un Optional que contiene el UserEntity si se encuentra, o vacío si no.
     */
    Optional<UserEntity> findByUserUri(String userUri); // Este es el método clave

    /**
     * Comprueba si existe un usuario con el nombre de usuario dado.
     * @param username El nombre de usuario a comprobar.
     * @return true si existe un usuario, false en caso contrario.
     */
    boolean existsByUsername(String username);

    /**
     * Comprueba si existe un usuario con el correo electrónico dado.
     * @param email El correo electrónico a comprobar.
     * @return true si existe un usuario, false en caso contrario.
     */
    boolean existsByEmail(String email);

}