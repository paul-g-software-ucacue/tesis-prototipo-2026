-- La versión nueva de una reforma (F18-H02, D-113): copia la publicada, que sigue CONGELADA, y
-- registra el motivo REFORMA. El nombre del check es el que PostgreSQL le dio en V8.
alter table version_distributivo drop constraint version_distributivo_motivo_check;
alter table version_distributivo add constraint version_distributivo_motivo_check
    check (motivo in ('RECHAZO_EN_FIRMA', 'REFORMA'));
