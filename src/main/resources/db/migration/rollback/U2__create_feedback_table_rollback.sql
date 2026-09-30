-- ============================================================================
-- U2__create_feedback_table_rollback.sql
-- Down-migration / Rollback script for V2__create_feedback_table.sql
-- ============================================================================

DROP TABLE IF EXISTS feedback CASCADE;
