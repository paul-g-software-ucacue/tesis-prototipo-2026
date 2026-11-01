-- Las actividades no lectivas de cada parcial (F06-H04, RF-26, MD-01). La actividad y el proyecto son
-- de otros servicios: sin llaves foráneas (principio V). Una por docente, actividad y proyecto en cada
-- parcial: el proyecto es opcional, y un valor nulo no choca en un índice común.

create table actividad_no_lectiva (
    id uuid primary key,
    version_id uuid not null,
    carrera varchar(20) not null,
    docente varchar(13) not null,
    nombre_docente varchar(200) not null,
    actividad varchar(6) not null,
    nombre_actividad varchar(500) not null,
    componente varchar(14) not null
        check (componente in ('OTRAS_DOCENCIA', 'INVESTIGACION', 'VINCULACION', 'GESTION')),
    proyecto varchar(30),
    nombre_proyecto varchar(300),
    horas numeric(5, 2) not null check (horas > 0 and horas <= 40),
    asignada_en timestamptz not null,
    asignada_por varchar(64) not null,
    foreign key (version_id, carrera) references parcial_carrera (version_id, carrera),
    check ((proyecto is null) = (nombre_proyecto is null))
);

create unique index actividad_una_por_parcial
    on actividad_no_lectiva (version_id, carrera, docente, actividad, coalesce(proyecto, ''));

create index actividad_por_docente on actividad_no_lectiva (version_id, docente);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check check (accion in (
    'DISTRIBUTIVO_CREADO', 'PROPUESTA_GUARDADA', 'PROPUESTA_QUITADA',
    'ACTIVIDAD_GUARDADA', 'ACTIVIDAD_QUITADA'));
