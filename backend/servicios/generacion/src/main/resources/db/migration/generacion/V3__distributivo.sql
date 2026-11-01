-- El distributivo del periodo, sus versiones, sus parciales y las propuestas (F06-H03, MD-01). El
-- periodo, la carrera, la asignatura y el docente son de otros servicios: sin llaves foráneas
-- (principio V). Una propuesta por asignatura-paralelo, por su llave primaria (D-19).

create table distributivo (
    id uuid primary key,
    periodo varchar(20) not null unique,
    creado_en timestamptz not null,
    creado_por varchar(64) not null
);

create table version_distributivo (
    id uuid primary key,
    distributivo_id uuid not null references distributivo (id),
    numero integer not null check (numero > 0),
    estado varchar(10) not null check (estado in ('EDITABLE', 'CONGELADA', 'ANULADA')),
    unique (distributivo_id, numero)
);

create table parcial_carrera (
    version_id uuid not null references version_distributivo (id),
    carrera varchar(20) not null,
    estado varchar(14) not null check (estado in ('EN_ELABORACION', 'ENVIADO', 'DEVUELTO')),
    primary key (version_id, carrera)
);

create table propuesta (
    version_id uuid not null,
    asignatura varchar(30) not null,
    paralelo varchar(20) not null,
    carrera varchar(20) not null,
    docente varchar(13) not null,
    nombre_docente varchar(200) not null,
    justificacion varchar(14) not null
        check (justificacion in ('AFINIDAD', 'DISPONIBILIDAD', 'EXCEPCION')),
    texto varchar(500),
    propuesta_en timestamptz not null,
    propuesta_por varchar(64) not null,
    primary key (version_id, asignatura, paralelo),
    foreign key (version_id, carrera) references parcial_carrera (version_id, carrera),
    -- Solo una excepción lleva texto, y siempre lo lleva.
    check ((justificacion = 'EXCEPCION') = (texto is not null))
);

create index propuesta_por_docente on propuesta (version_id, docente);

-- Cada cambio, con quién y cuándo (principio VII). El detalle nunca lleva el texto de la justificación.
create table registro_cambio (
    id uuid primary key,
    momento timestamptz not null,
    usuario varchar(64) not null,
    accion varchar(20) not null check (accion in (
        'DISTRIBUTIVO_CREADO', 'PROPUESTA_GUARDADA', 'PROPUESTA_QUITADA')),
    detalle jsonb not null
);
