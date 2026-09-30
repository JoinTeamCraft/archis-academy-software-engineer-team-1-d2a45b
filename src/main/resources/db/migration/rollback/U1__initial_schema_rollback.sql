-- ============================================================================
-- U1__initial_schema_rollback.sql
-- Down-migration / Rollback script for V1__initial_schema.sql
-- Reverses all schema changes applied in V1 in reverse dependency order.
-- ============================================================================

-- Drop tables in reverse order of foreign key dependencies
DROP TABLE IF EXISTS report_metadata CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS vehicles CASCADE;
DROP TABLE IF EXISTS parking_spots CASCADE;
DROP TABLE IF EXISTS parking_lots CASCADE;
DROP TABLE IF EXISTS users CASCADE;
