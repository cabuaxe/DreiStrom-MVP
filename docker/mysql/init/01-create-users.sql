-- Create application user (limited privileges)
CREATE USER IF NOT EXISTS 'dreistrom_app'@'%' IDENTIFIED BY 'dreistrom_dev';

-- Create migration user (full privileges for Flyway)
CREATE USER IF NOT EXISTS 'dreistrom_migration'@'%' IDENTIFIED BY 'dreistrom_dev';

-- Grant migration user full access
GRANT ALL PRIVILEGES ON dreistrom.* TO 'dreistrom_migration'@'%';

-- Grant app user standard CRUD on all tables
GRANT SELECT, INSERT, UPDATE, DELETE ON dreistrom.* TO 'dreistrom_app'@'%';

FLUSH PRIVILEGES;
