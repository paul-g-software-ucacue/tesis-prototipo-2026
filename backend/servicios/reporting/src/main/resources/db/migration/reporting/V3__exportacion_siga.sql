-- Las exportaciones a SIGA (F19-H02, RF-53, D-120): cada CSV para SIGA entregado, con quién,
-- cuándo, la versión publicada y su hash, la versión del layout de exportación, el archivo, sus
-- filas y su suma de verificación (el SHA-256 de los bytes entregados). El archivo no se guarda:
-- con la versión y el layout se vuelve a generar idéntico.
--
-- Se inserta en la misma transacción que la entrada CSV_SIGA_GENERADO de la bitácora, antes de
-- entregar el archivo. No se modifica ni se borra (principio VII).
create table exportacion_siga (
    id            uuid        primary key,
    periodo       varchar(20) not null check (periodo ~ '^[A-Za-z0-9._-]{1,20}$'),
    version       integer     not null check (version >= 1),
    hash_version  char(64)    not null check (hash_version ~ '^[0-9a-f]{64}$'),
    layout        integer     not null check (layout >= 0),
    nombre        varchar(80) not null check (length(nombre) > 0),
    filas         integer     not null check (filas >= 0),
    sha256        char(64)    not null check (sha256 ~ '^[0-9a-f]{64}$'),
    exportada_por varchar(64) not null check (length(exportada_por) > 0),
    exportada_en  timestamptz not null
);

create index exportacion_siga_del_periodo on exportacion_siga (periodo, exportada_en desc);

create function exportacion_siga_sin_cambios() returns trigger
    language plpgsql
as $$
begin
    raise exception 'Una exportación a SIGA no se puede modificar ni borrar.'
        using errcode = 'insufficient_privilege';
end;
$$;

create trigger exportacion_siga_sin_cambios
    before update or delete on exportacion_siga
    for each row execute function exportacion_siga_sin_cambios();
