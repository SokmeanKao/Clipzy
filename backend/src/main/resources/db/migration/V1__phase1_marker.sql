-- Phase 1 placeholder so Flyway has a baseline migration.
-- Real schema arrives in Phase 2.
CREATE TABLE IF NOT EXISTS flyway_phase1_marker (
    id INT PRIMARY KEY,
    note TEXT NOT NULL
);

INSERT INTO flyway_phase1_marker (id, note)
VALUES (1, 'clipzy phase 1 scaffold')
ON CONFLICT (id) DO NOTHING;
