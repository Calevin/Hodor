package com.calevin.hodor.infrastructure.persistence.repositories;

import com.calevin.hodor.infrastructure.persistence.entities.UserClientRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserClientRoleRepository extends JpaRepository<UserClientRoleEntity, UUID> {

    @Query("""
        SELECT r.role 
        FROM UserClientRoleEntity r 
        WHERE r.user.username = :username AND r.clientId = :clientId
    """)
    List<String> findRolesByUsernameAndClientId(@Param("username") String username, @Param("clientId") String clientId);
}
