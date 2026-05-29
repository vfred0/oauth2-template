-- DEV / DEMO SEED — DO NOT RUN IN PRODUCTION.
-- The raw keys below are KNOWN (their hashes are seeded). Use only for local testing.
--   demo-admin   (role ADMIN)         raw key: sk_live_demo_admin_key_AAAA
--   demo-analyst (role DATA_ANALYST)  raw key: sk_live_demo_analyst_key_BBBB
-- Apply against the application DB, e.g.:
--   docker compose exec -T postgres-app psql -U app -d appdb < src/main/resources/db/seed/rbac_apikey_demo.sql

INSERT INTO permissions (resource, action) VALUES ('apikeys', 'manage')
ON CONFLICT (resource, action) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'apikeys' AND p.action = 'manage'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (keycloak_sub, role_id)
SELECT 'demo-admin', id FROM roles WHERE name = 'ADMIN'
ON CONFLICT (keycloak_sub) DO NOTHING;

INSERT INTO user_roles (keycloak_sub, role_id)
SELECT 'demo-analyst', id FROM roles WHERE name = 'DATA_ANALYST'
ON CONFLICT (keycloak_sub) DO NOTHING;

INSERT INTO api_keys (key_hash, prefix_hint, subject, label, created_at)
VALUES
    ('ea41c12893ddefbf93770df12baa08d1f0397ae6fa3e041cb248ea29b9384d7d',
     'sk_live_...AAAA', 'demo-admin', 'demo admin key', now()),
    ('e3a1d7784f6c64d7f43ca5580308cce5f8351044e039c09318f3ce4111c8896b',
     'sk_live_...BBBB', 'demo-analyst', 'demo analyst key', now())
ON CONFLICT (key_hash) DO NOTHING;
