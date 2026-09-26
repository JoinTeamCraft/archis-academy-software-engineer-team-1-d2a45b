-- Runs once, when the Postgres container starts with an empty data volume.
-- Tables are created by Hibernate (and later by Flyway, PLS-045), not here.

ALTER DATABASE parking_lot SET timezone TO 'UTC';
