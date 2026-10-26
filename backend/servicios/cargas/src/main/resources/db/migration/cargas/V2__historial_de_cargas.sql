-- El historial de cargas (F01-H04, RF-10, glosario «Historial de cargas»): cada intento de confirmar
-- una carga, con el usuario, el momento, el nombre del archivo, su hash y el resultado. No guarda el
-- contenido del archivo (principio IX). Un hash con el resultado APLICADA en la misma carga hace que
-- otra confirmación responda REPETIDA (MD-13).
create table cargas.historial_carga (
    id bigint generated always as identity primary key,
    carga varchar(40) not null check (carga ~ '^[a-z_]{1,40}$'),
    usuario varchar(100) not null check (length(usuario) > 0),
    momento timestamptz not null,
    archivo varchar(255) not null check (length(archivo) > 0),
    hash char(64) not null check (hash ~ '^[0-9a-f]{64}$'),
    resultado varchar(10) not null check (resultado in ('APLICADA', 'RECHAZADA', 'REPETIDA')),
    nuevos integer not null check (nuevos >= 0),
    cambiados integer not null check (cambiados >= 0),
    bajas_sin_aplicar integer not null check (bajas_sin_aplicar >= 0),
    motivo varchar(30)
        check (motivo in ('CON_ERRORES', 'VISTA_PREVIA_DESACTUALIZADA', 'RECHAZADA_POR_EL_DUENO')),
    -- Solo un rechazo tiene motivo, y todo rechazo lo tiene.
    check ((resultado = 'RECHAZADA') = (motivo is not null))
);

create index historial_carga_por_carga on cargas.historial_carga (carga, momento desc, id desc);

create index historial_carga_aplicada on cargas.historial_carga (carga, hash)
    where resultado = 'APLICADA';
