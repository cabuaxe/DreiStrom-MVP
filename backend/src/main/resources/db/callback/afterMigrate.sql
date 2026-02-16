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
GRANT UPDATE, DELETE ON dreistrom.app_user      TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.document      TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.client        TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.income_entry  TO 'dreistrom_app'@'%';

-- Expense tables (V3)
GRANT UPDATE, DELETE ON dreistrom.allocation_rule     TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.expense_entry       TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.depreciation_asset  TO 'dreistrom_app'@'%';

-- Invoice tables (V4)
GRANT UPDATE, DELETE ON dreistrom.invoice_sequence    TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.invoice             TO 'dreistrom_app'@'%';

-- Tax & VAT tables (V6)
GRANT UPDATE, DELETE ON dreistrom.tax_period          TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.vat_return          TO 'dreistrom_app'@'%';

-- Vorauszahlung (V7)
GRANT UPDATE, DELETE ON dreistrom.vorauszahlung       TO 'dreistrom_app'@'%';

-- Compliance (V8)
GRANT UPDATE, DELETE ON dreistrom.compliance_event    TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.threshold_alert     TO 'dreistrom_app'@'%';

-- Notification (V9)
GRANT UPDATE, DELETE ON dreistrom.notification        TO 'dreistrom_app'@'%';

-- Social insurance (V10)
GRANT UPDATE, DELETE ON dreistrom.social_insurance_entry TO 'dreistrom_app'@'%';

-- Time tracking (V11)
GRANT UPDATE, DELETE ON dreistrom.time_entry          TO 'dreistrom_app'@'%';

-- App store payout (V13)
GRANT UPDATE, DELETE ON dreistrom.appstore_payout     TO 'dreistrom_app'@'%';

-- Onboarding (V14)
GRANT UPDATE, DELETE ON dreistrom.registration_step   TO 'dreistrom_app'@'%';
GRANT UPDATE, DELETE ON dreistrom.decision_point      TO 'dreistrom_app'@'%';

-- event_log: intentionally NO UPDATE/DELETE (GoBD compliance)
-- dreistrom_app only has SELECT + INSERT via database-level grant
