-- 6. Tabla de Roles por Cliente (Multi-tenancy)
-- Almacena los roles específicos que un usuario tiene en una aplicación concreta.
CREATE TABLE user_client_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    client_id VARCHAR(100) NOT NULL, -- Logical client_id from oauth2_registered_client
    role VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_client_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_client_role UNIQUE (user_id, client_id, role)
);

-- Índices para optimizar la búsqueda de roles por usuario y cliente
CREATE INDEX idx_user_client_roles_user_client ON user_client_roles(user_id, client_id);
