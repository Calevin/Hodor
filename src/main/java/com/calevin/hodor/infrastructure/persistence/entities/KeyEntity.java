package com.calevin.hodor.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth2_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyEntity {
    @Id
    private UUID id;
    private String kid;
    @Column(columnDefinition = "TEXT")
    private String keyData;
    private Instant createdAt;
}