package com.cercademiurentals.api.service.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cercademiurentals.api.entity.UserEntity;
import com.cercademiurentals.api.repository.UserRepository;


@Service("userDetailsService") // Es importante darle un nombre al bean si tienes múltiples UserDetailsService
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carga un usuario por su nombre de usuario o correo electrónico.
     * Este método es llamado por Spring Security durante la autenticación.
     *
     * @param usernameOrEmail El nombre de usuario o correo electrónico proporcionado en el login.
     * @return Un objeto UserDetails que contiene la información del usuario.
     * @throws UsernameNotFoundException si el usuario no se encuentra o no está activo.
     */
    @Override
    @Transactional(readOnly = true) // readOnly = true porque solo estamos leyendo datos
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Intentar buscar por nombre de usuario primero
        Optional<UserEntity> userOptional = userRepository.findByUsername(usernameOrEmail);
        
        // Si no se encuentra por nombre de usuario, intentar por correo electrónico
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(usernameOrEmail);
        }

        UserEntity userEntity = userOptional.orElseThrow(() ->
                new UsernameNotFoundException("Usuario no encontrado con identificador: " + usernameOrEmail));

        if (!userEntity.isActive()) {
            throw new UsernameNotFoundException("La cuenta de usuario está desactivada: " + usernameOrEmail);
        }

        // Aquí defines los roles/autoridades del usuario.
        // Por ahora, asignaremos un rol genérico "ROLE_USER".
        // Si tienes un sistema de roles más complejo almacenado en UserEntity o en Fuseki,
        // deberías cargar esos roles aquí.
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Rol básico
        
        // Ejemplo si tuvieras un campo 'roles' en UserEntity como "ROLE_USER,ROLE_PROVIDER":
        // if (userEntity.getRoles() != null && !userEntity.getRoles().isEmpty()) {
        //     authorities = java.util.Arrays.stream(userEntity.getRoles().split(","))
        //                        .map(SimpleGrantedAuthority::new)
        //                        .collect(Collectors.toSet());
        // }


        return new User(
                userEntity.getUsername(), // El "username" que Spring Security usará internamente
                userEntity.getPasswordHash(), // La contraseña HASHEADA de la base de datos
                userEntity.isActive(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities // Las autoridades (roles) del usuario
        );
    }
}
