-- Consolidar y el informe sustentado (F10-H02, RF-32, D-88). El consolidado se calcula al abrirlo:
-- solo se guarda quién y cuándo. Las dos tablas son solo de inserción: vale la última fila de cada
-- versión, y las anteriores se conservan (principio VII). El archivo del informe está en el volumen
-- archivos: aquí van su clave y su huella.

create table consolidacion (
    id uuid primary key,
    version_id uuid not null references version_distributivo (id) on delete cascade,
    consolidada_por varchar(64) not null,
    consolidada_en timestamptz not null
);

create index consolidacion_vigente on consolidacion (version_id, consolidada_en desc);

create table informe_sustentado (
    id uuid primary key,
    version_id uuid not null references version_distributivo (id) on delete cascade,
    nombre varchar(150) not null check (length(nombre) > 0),
    tamano integer not null check (tamano between 1 and 10485760),
    huella char(64) not null,
    clave varchar(36) not null unique,
    adjuntado_por varchar(64) not null,
    adjuntado_en timestamptz not null
);

create index informe_vigente on informe_sustentado (version_id, adjuntado_en desc);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check check (accion in (
    'DISTRIBUTIVO_CREADO', 'PROPUESTA_GUARDADA', 'PROPUESTA_QUITADA',
    'ACTIVIDAD_GUARDADA', 'ACTIVIDAD_QUITADA', 'INCONSISTENCIA_JUSTIFICADA',
    'DISTRIBUTIVO_CONSOLIDADO', 'INFORME_ADJUNTADO'));
