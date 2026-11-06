-- La bitácora de auditoría (F17-H01, RF-44, data-model.md): una entrada por cambio guardado en
-- otro servicio, con quién, qué, cuándo, objeto, valor anterior y valor nuevo. Solo crece:
--   1. ninguna operación de la API la modifica (FR-009);
--   2. los disparadores rechazan UPDATE, DELETE y TRUNCATE para cualquier rol (FR-010);
--   3. el rol que migra, que es el del proceso, pierde esos permisos.
-- El dueño de la tabla podría quitar los disparadores con DDL: riesgo residual documentado en
-- specs/039-bitacora-cambios/research.md §3.
create table bitacora (
    id_evento uuid primary key,
    servicio varchar(20) not null
        check (servicio in ('generacion', 'academico', 'docentes', 'normativa', 'roles',
                            'validacionocs')),
    accion varchar(40) not null check (accion ~ '^[A-Z][A-Z_]{2,39}$'),
    usuario varchar(64) not null check (length(usuario) >= 1),
    momento timestamptz not null,
    periodo varchar(20) check (periodo ~ '^[A-Za-z0-9._-]{1,20}$'),
    objeto jsonb not null,
    anterior jsonb,
    nuevo jsonb,
    recibida_en timestamptz not null default now(),
    check (anterior is not null or nuevo is not null)
);

-- Para consultarla por periodo y por fecha (F17-H02).
create index bitacora_periodo_momento on bitacora (periodo, momento);
create index bitacora_momento on bitacora (momento);

create function bitacora_rechazar_cambio() returns trigger
    language plpgsql
as $$
begin
    raise exception 'La bitácora no se puede modificar ni borrar.'
        using errcode = 'insufficient_privilege';
end;
$$;

create trigger bitacora_sin_cambios
    before update or delete on bitacora
    for each row execute function bitacora_rechazar_cambio();

create trigger bitacora_sin_truncar
    before truncate on bitacora
    for each statement execute function bitacora_rechazar_cambio();

-- El rol del proceso solo inserta y lee.
revoke update, delete, truncate on bitacora from current_user;
