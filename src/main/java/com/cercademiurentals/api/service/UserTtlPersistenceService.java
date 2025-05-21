package com.cercademiurentals.api.service;

import com.cercademiurentals.api.entity.UserEntity;
import com.cercademiurentals.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserTtlPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserTtlPersistenceService.class);
    private final UserRepository userRepository;

    @Autowired
    public UserTtlPersistenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Guarda un único UserEntity en una transacción nueva e independiente.
     * Si esta transacción falla (ej. por DataIntegrityViolationException),
     * solo esta operación individual hará rollback.
     *
     * @param userEntity El usuario a guardar.
     * @return true si se guardó correctamente, false si hubo un error de integridad.
     * @throws Exception para otros errores inesperados durante el guardado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveUserInNewTransaction(UserEntity userEntity) throws Exception {
        try {
            userRepository.save(userEntity);
            logger.debug("UserTtlPersistenceService: Usuario guardado exitosamente en nueva transacción: {}", userEntity.getUserUri());
            return true;
        } catch (DataIntegrityViolationException dive) {
            // Este error será manejado por el DataLoader que llama a este método.
            // Se relanza para que el DataLoader pueda registrarlo específicamente.
            logger.warn("UserTtlPersistenceService: DataIntegrityViolationException al guardar {}: {}", userEntity.getUserUri(), dive.getMessage());
            throw dive; 
        } catch (Exception e) {
            logger.error("UserTtlPersistenceService: Error inesperado al guardar {} en nueva transacción: {}", userEntity.getUserUri(), e.getMessage(), e);
            throw e; // Relanzar otras excepciones para que el DataLoader las maneje.
        }
    }
}