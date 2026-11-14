-- Cada PDF subido por el firmante en turno, aceptado o rechazado, con el resultado de su validación
-- básica (F14-H02, D-23, D-103). La firma de un paso es su validación aceptada; su archivo es
-- documentos/<sha256_subido>.pdf. Las subidas de quien no está en turno no dejan fila.
create table validacion_firma (
    id             uuid        primary key,
    periodo        varchar(20) not null,
    version        integer     not null,
    tipo           varchar(20) not null,
    -- El paso en turno al subir.
    orden          integer     not null check (orden >= 1),
    subido_por     varchar(64) not null,
    subido_en      timestamptz not null,
    sha256_subido  char(64)    not null check (sha256_subido ~ '^[0-9a-f]{64}$'),
    tamano         integer     not null check (tamano > 0),
    aceptada       boolean     not null,
    -- Las cuatro comprobaciones, en orden, con su resultado y su motivo.
    comprobaciones jsonb       not null check (jsonb_typeof(comprobaciones) = 'array'),
    -- El certificado de la firma nueva, si había una sola legible.
    certificado    jsonb,
    -- A la generación, como firmante_esperado: verificar la clave bloquea la fila apuntada con
    -- FOR KEY SHARE como su dueño, y eso exige UPDATE, que V2 le quitó en documento_firmable.
    foreign key (periodo, version, tipo) references generacion_documento
);

-- Una sola firma por paso: la garantía en la base de que dos subidas a la vez no registran dos
-- (FR-014), además del bloqueo de la generación al registrar.
create unique index validacion_firma_aceptada
    on validacion_firma (periodo, version, tipo, orden) where aceptada;

-- Como el documento, no cambia (principio VII): el rol del proceso solo inserta y lee.
revoke update, delete, truncate on validacion_firma from current_user;
