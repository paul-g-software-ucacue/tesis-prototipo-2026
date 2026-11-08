-- El proceso de aprobación de cada distributivo (F12-H02, RF-34, data-model.md): su etapa actual,
-- la copia de la configuración con la que empezó (FR-003), el envío de cada parcial y sus
-- transiciones. Sin referencia a Generación ni a Académico: son otros esquemas (principio V). Un
-- distributivo por periodo; distributivo_creado_en dice de cuál es, por si se recrea (research §5).

create table proceso_aprobacion (
    periodo varchar(20) primary key,
    estado varchar(30) not null
        check (estado in ('BORRADOR_CARRERA', 'BORRADOR_UNIDADES', 'EN_CONSOLIDACION',
                          'EN_REVISION_VRA', 'CONGELADO_EN_FIRMA', 'EN_OCS', 'REMITIDO_ZONAL',
                          'APROBADO', 'PUBLICADO')),
    etapas jsonb not null,
    distributivo_creado_en timestamptz not null,
    creado_en timestamptz not null
);

create table parcial_del_proceso (
    periodo varchar(20) not null references proceso_aprobacion (periodo) on delete cascade,
    carrera varchar(20) not null,
    estado varchar(10) not null check (estado in ('PENDIENTE', 'ENVIADO')),
    primary key (periodo, carrera)
);

-- Solo de inserción (principio VII): cada transición también va a la bitácora (D-95).
create table transicion (
    id uuid primary key,
    periodo varchar(20) not null references proceso_aprobacion (periodo) on delete cascade,
    tipo varchar(15) not null check (tipo in ('ENVIO_PARCIAL', 'PASO_DE_ETAPA')),
    desde varchar(30) not null,
    hacia varchar(30) not null,
    carrera varchar(20),
    version integer not null check (version >= 1),
    hash char(64) not null check (hash ~ '^[0-9a-f]{64}$'),
    quien varchar(64) not null,
    momento timestamptz not null,
    check ((tipo = 'ENVIO_PARCIAL') = (carrera is not null))
);

create index transicion_por_periodo on transicion (periodo, momento);
