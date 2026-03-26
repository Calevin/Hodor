-- 1. Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Tabla estándar de Spring Authorization Server (Registered Client)
-- Esta tabla permite que Hodor sepa qué sistemas (Blog, API Go) tienen permiso de pedir tokens.
CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);

-- 3. Tabla de Usuarios (Esquema Hodor)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Almacenará el hash de BCrypt
    email VARCHAR(100) UNIQUE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE
);

-- 4. Tabla de Autoridades (Roles de Spring Security)
CREATE TABLE authorities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_authority UNIQUE (user_id, authority)
);

-- 5. Tabla de Vínculo Usuario-Sistema
-- Define a qué aplicaciones tiene permitido loguearse un usuario.
CREATE TABLE user_client_access (
    user_id UUID NOT NULL,
    client_internal_id VARCHAR(100) NOT NULL, -- Referencia al ID de oauth2_registered_client
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, client_internal_id),
    CONSTRAINT fk_access_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_client FOREIGN KEY (client_internal_id) REFERENCES oauth2_registered_client(id) ON DELETE CASCADE
);

-- Índices para optimizar la búsqueda de usuarios y validación de tokens
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_auth_user_id ON authorities(user_id);