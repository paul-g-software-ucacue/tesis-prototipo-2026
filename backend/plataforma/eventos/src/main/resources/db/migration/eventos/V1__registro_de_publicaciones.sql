-- Registro persistente de publicaciones del proceso (principio V, ADR-018), en eventos_<proceso>.
-- Es el DDL v2 de PostgreSQL de Spring Modulith 2.1.1
-- (spring-modulith-events-jdbc: schemas/v2/schema-postgresql.sql), sin cambios. Lo migra
-- flywayEventos, porque la creación automática de Spring Modulith está apagada.
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
