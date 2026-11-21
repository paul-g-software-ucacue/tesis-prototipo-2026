-- El contenido congelado de cada versión (F20-H01, D-115): por docente, sus asignaturas con las
-- horas de clase de la oferta al congelar, sus actividades, su carga por componente y su
-- dedicación. Se guarda una sola vez y no se modifica (principio VII): la oferta se puede corregir
-- después de publicar, y lo publicado no debe cambiar.
--
-- Cuelga de la versión y se borra con ella: la limpieza de los scripts de demostración recrea el
-- distributivo de un periodo, y un contenido guardado por (periodo, número) quedaría de otra
-- versión con el mismo número.
create table contenido_congelado (
    version_id  uuid        primary key
        references version_distributivo (id) on delete cascade,
    periodo     varchar(20) not null,
    numero      integer     not null check (numero >= 1),
    hash        char(64)    not null check (hash ~ '^[0-9a-f]{64}$'),
    guardado_en timestamptz not null,
    -- true si se calculó al pedirlo y no al congelar (versiones congeladas antes de D-115).
    tardio      boolean     not null,
    unique (periodo, numero)
);

create table carga_congelada (
    version_id uuid         not null references contenido_congelado (version_id) on delete cascade,
    docente    varchar(13)  not null,
    nombre     varchar(200) not null,
    contenido  jsonb        not null,
    primary key (version_id, docente)
);

create function contenido_congelado_sin_cambios() returns trigger
    language plpgsql
as $$
begin
    raise exception 'El contenido congelado no se puede modificar.'
        using errcode = 'insufficient_privilege';
end;
$$;

create trigger contenido_congelado_sin_cambios
    before update on contenido_congelado
    for each row execute function contenido_congelado_sin_cambios();

create trigger carga_congelada_sin_cambios
    before update on carga_congelada
    for each row execute function contenido_congelado_sin_cambios();
