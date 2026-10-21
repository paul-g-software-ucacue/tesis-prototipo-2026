-- Los periodos académicos con sus estados (F02-H01, RF-13) y el registro de sus cambios (FR-008,
-- principio VII). Son las primeras tablas de negocio del esquema academico (D-35).
-- La versión de normativa del periodo no está aquí: la guarda Normativa (asignación normativa), y
-- Académico se la pide por su interfaz publicada (research §2 y §4). Sin datos precargados: los
-- periodos se registran a mano hasta que llegue la carga 3 (F01).

create table periodo_academico (
    codigo varchar(20) primary key check (codigo ~ '^[A-Za-z0-9._-]{1,20}$'),
    nombre varchar(120) not null check (length(trim(nombre)) > 0),
    inicio date not null,
    fin date not null,
    estado varchar(16) not null
        check (estado in ('PLANIFICACION', 'EN_APROBACION', 'VIGENTE', 'CERRADO')),
    registrado_en timestamptz not null,
    registrado_por varchar(64) not null,
    check (fin > inicio)
);

create table registro_cambio (
    id uuid primary key,
    momento timestamptz not null,
    usuario varchar(64) not null,
    accion varchar(30) not null
        check (accion in ('PERIODO_REGISTRADO', 'PERIODO_CORREGIDO', 'PERIODO_ABIERTO',
                          'PERIODO_ESTADO_CAMBIADO')),
    detalle jsonb not null
);
