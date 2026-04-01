-- =================================================================
-- V7__db_hardening.sql
-- Endurecimiento del modelo de datos: integridad referencial,
-- documentacion de proposito de tablas, indices y columnas de auditoria.
-- =================================================================

-- -----------------------------------------------------------------
-- 1. Documentacion de proposito de tablas (Identidad vs Contexto)
--
-- Separacion de responsabilidades:
--   authorities       -> Gestion interna del IdP (Hodor).
--   user_client_access -> Control de acceso por aplicacion (puerta de entrada).
--   user_client_roles  -> Permisos especificos dentro de cada aplicacion.
-- -----------------------------------------------------------------

COMMENT ON TABLE authorities IS
    'Roles globales del IdP (Hodor). Define la identidad administrativa del usuario '
    'dentro del sistema de autenticacion (ej. ROLE_ADMIN, ROLE_USER). '
    'NO se usa para logica de negocio de clientes.';

COMMENT ON TABLE user_client_access IS
    'Control de acceso por aplicacion. Determina si un usuario puede iniciar sesion '
    'en un cliente especifico (Blog, Juego). Se usa para bloqueos globales de '
    'aplicacion sin eliminar los roles del usuario en user_client_roles.';

COMMENT ON TABLE user_client_roles IS
    'Permisos especificos por cliente (multi-tenancy). Define que puede hacer un '
    'usuario dentro de una aplicacion concreta (ej. EDITOR en Blog, MODERATOR en Juego). '
    'La existencia de registros aqui NO implica acceso; user_client_access es la puerta de entrada.';

-- -----------------------------------------------------------------
-- 2. FK en user_client_roles.client_id -> oauth2_registered_client.client_id
--    Previene la asignacion de roles a clientes inexistentes.
-- -----------------------------------------------------------------

-- Prerequisito: UNIQUE constraint en oauth2_registered_client.client_id
-- para que pueda ser objetivo de una Foreign Key.
ALTER TABLE oauth2_registered_client
    ADD CONSTRAINT uk_oauth2_client_id UNIQUE (client_id);

ALTER TABLE user_client_roles
    ADD CONSTRAINT fk_client_roles_client
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE;

-- -----------------------------------------------------------------
-- 3. UNIQUE constraint en oauth2_keys.kid
--    Previene colisiones durante la rotacion de llaves.
-- -----------------------------------------------------------------

ALTER TABLE oauth2_keys
    ADD CONSTRAINT uk_oauth2_keys_kid UNIQUE (kid);

-- -----------------------------------------------------------------
-- 4. Columna updated_at en users (auditoria de modificaciones)
-- -----------------------------------------------------------------

ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- -----------------------------------------------------------------
-- 5. Indices en oauth2_authorization
--    Optimiza las consultas frecuentes: busqueda por principal,
--    por cliente, y limpieza de tokens expirados.
-- -----------------------------------------------------------------

CREATE INDEX idx_oauth2_auth_principal ON oauth2_authorization(principal_name);
CREATE INDEX idx_oauth2_auth_client ON oauth2_authorization(registered_client_id);
CREATE INDEX idx_oauth2_auth_token_expires ON oauth2_authorization(access_token_expires_at);

-- -----------------------------------------------------------------
-- 6. FK con ON DELETE CASCADE en oauth2_authorization_consent
--    Elimina consentimientos huerfanos al borrar un cliente.
-- -----------------------------------------------------------------

ALTER TABLE oauth2_authorization_consent
    ADD CONSTRAINT fk_consent_client
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id) ON DELETE CASCADE;
