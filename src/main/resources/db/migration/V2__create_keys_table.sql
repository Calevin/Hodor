CREATE TABLE oauth2_keys (
    id UUID PRIMARY KEY,
    kid VARCHAR(255) NOT NULL,
    key_data TEXT NOT NULL, -- Almacenamos el JWK completo en formato JSON
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);