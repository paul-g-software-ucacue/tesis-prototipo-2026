-- El documento firmable de la versión congelada (F13-H02, D-101, data-model.md).
--
-- generacion_documento: la solicitud que entregó Aprobación (MD-05), con su cadena de firmas y el
-- rol de la etapa, y el último resultado de generar el documento de esa versión. Una por periodo,
-- versión y tipo: la fila se bloquea para que dos generaciones no registren dos documentos.
create table generacion_documento (
    periodo        varchar(20) not null check (periodo ~ '^[A-Za-z0-9._-]{1,20}$'),
    version        integer     not null check (version >= 1),
    tipo           varchar(20) not null check (tipo in ('CONSOLIDADO')),
    hash_contenido char(64)    not null check (hash_contenido ~ '^[0-9a-f]{64}$'),
    cadena         jsonb       not null check (jsonb_typeof(cadena) = 'array'),
    rol_de_etapa   varchar(40) not null,
    estado         varchar(20) not null check (estado in ('EN_CURSO', 'FALLIDA', 'GENERADA')),
    motivo         varchar(200),
    pedida_por     varchar(64) not null,
    actualizada_en timestamptz not null,
    primary key (periodo, version, tipo),
    check ((estado = 'FALLIDA') = (motivo is not null))
);

-- documento_firmable: el PDF/A registrado, uno por versión y tipo. El archivo está fuera de la base
-- (RNF-POR-01), nombrado por su SHA-256.
create table documento_firmable (
    id              uuid primary key,
    periodo         varchar(20) not null,
    version         integer     not null,
    tipo            varchar(20) not null,
    hash_contenido  char(64)    not null check (hash_contenido ~ '^[0-9a-f]{64}$'),
    sha256_original char(64)    not null check (sha256_original ~ '^[0-9a-f]{64}$'),
    archivo         varchar(80) not null,
    tamano          integer     not null check (tamano > 0),
    generado_por    varchar(64) not null,
    generado_en     timestamptz not null,
    unique (periodo, version, tipo),
    foreign key (periodo, version, tipo) references generacion_documento
);

-- El documento registrado no cambia (principio VII): el rol del proceso, que migra y es el dueño,
-- solo inserta y lee. F14 agrega el archivo vigente y las firmas en tablas propias.
revoke update, delete, truncate on documento_firmable from current_user;
