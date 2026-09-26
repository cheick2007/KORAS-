-- Initialisation de la base de données Koras

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS utilisateurs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    roles TEXT[] NOT NULL DEFAULT '{USER}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des préférences
CREATE TABLE IF NOT EXISTS preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    langue VARCHAR(10) NOT NULL DEFAULT 'FRANCAIS',
    vitesse_parole DECIMAL(3,1) NOT NULL DEFAULT 1.0,
    voix VARCHAR(50),
    volume_parole INT NOT NULL DEFAULT 80,
    vibration_activee BOOLEAN NOT NULL DEFAULT true,
    mode_verbeux BOOLEAN NOT NULL DEFAULT false,
    periode_retention VARCHAR(20) NOT NULL DEFAULT 'TRENTE_JOURS',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Table d'audit
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    horodatage TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id UUID REFERENCES utilisateurs(id),
    action VARCHAR(50) NOT NULL,
    parametres JSONB,
    resultat VARCHAR(20) NOT NULL,
    duree_ms BIGINT NOT NULL,
    token_idempotence UUID NOT NULL,
    hash_precedent VARCHAR(64) NOT NULL,
    hash VARCHAR(64) NOT NULL,
    signature VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index pour recherche rapide
CREATE INDEX idx_audit_horodatage ON audit_log(horodatage DESC);
CREATE INDEX idx_audit_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_token ON audit_log(token_idempotence);

-- Table des tokens révoqués (blacklist JWT)
CREATE TABLE IF NOT EXISTS revoked_tokens (
    jti VARCHAR(255) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES utilisateurs(id),
    revoked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

-- Index pour nettoyage automatique
CREATE INDEX idx_revoked_expires ON revoked_tokens(expires_at);

-- Fonction de nettoyage automatique des tokens expirés
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM revoked_tokens WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Utilisateur de test (mot de passe: "password" - à changer en production)
INSERT INTO utilisateurs (id, email, password_hash, roles)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'test@koras.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- "password"
    '{USER, ADMIN}'
) ON CONFLICT (email) DO NOTHING;

-- Message de confirmation
DO $$
BEGIN
    RAISE NOTICE 'Base de données Koras initialisée avec succès';
END $$;
