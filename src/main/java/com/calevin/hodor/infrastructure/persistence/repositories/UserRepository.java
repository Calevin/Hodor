package com.calevin.hodor.infrastructure.persistence.repositories;

import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByAuthorities_Authority(String authority);

    @Modifying
    @Query(value = "INSERT INTO user_client_access (user_id, client_internal_id) VALUES (:userId, :clientId)", nativeQuery = true)
    void linkUserToClient(@Param("userId") UUID userId, @Param("clientId") String clientId);

    @Query(value = """
                SELECT c.client_id
                FROM oauth2_registered_client c
                JOIN user_client_access uca ON c.id = uca.client_internal_id
                JOIN users u ON u.id = uca.user_id
                WHERE u.username = :username
            """, nativeQuery = true)
    List<String> findAuthorizedClientsByUsername(@Param("username") String username);
}