-- ============================================================
-- DreiStrom MVP – Post-Migration Privilege Grant
-- Runs after every Flyway migration as dreistrom_migration user.
--
-- Grants UPDATE + DELETE to dreistrom_app on ALL tables except
-- event_log (GoBD append-only audit trail: INSERT+SELECT only).
--
-- When adding new tables, add a GRANT line here unless the
-- table must be append-only.
-- ============================================================

-- Application tables: full CRUD (SELECT/INSERT from db-level grant)
GRANT UPDATE, DELETE ON dreistrom.app_user  TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.document  TO 'dreistrom_app'@'%';

-- event_log: intentionally NO UPDATE/DELETE (GoBD compliance)
-- dreistrom_app only has SELECT + INSERT via database-level grant
