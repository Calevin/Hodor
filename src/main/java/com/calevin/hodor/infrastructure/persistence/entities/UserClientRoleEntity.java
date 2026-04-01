package com.calevin.hodor.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que representa la relación entre Usuarios, Clientes y Roles específicos del cliente.
 * Implementa el patrón Multi-tenancy para gestionar permisos diferenciados por aplicación.
 */
@Entity
@Table(name = "user_client_roles", uniqueConstraints = 
    @UniqueConstraint(columnNames = {"user_id", "client_id", "role"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserClientRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(nullable = false, length = 50)
    private String role;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private OffsetDateTime assignedAt;
}
