package com.calevin.hodor.infrastructure.persistence.repositories;

import com.calevin.hodor.infrastructure.persistence.entities.KeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface KeyRepository extends JpaRepository<KeyEntity, UUID> {
    // Obtenemos la llave más reciente
    Optional<KeyEntity> findFirstByOrderByCreatedAtDesc();
}