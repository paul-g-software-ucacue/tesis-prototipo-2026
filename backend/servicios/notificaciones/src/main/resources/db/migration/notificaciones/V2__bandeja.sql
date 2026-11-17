-- La bandeja interna de cada usuario (F16-H01, RF-41, data-model.md).
--
-- evento_recibido: los eventos de otros procesos ya registrados (ADR-018 §4). La clave primaria
-- hace la idempotencia: un id_evento se registra una sola vez, también con entregas simultáneas.
create table evento_recibido (
    id_evento uuid primary key,
    tipo varchar(120) not null,
    origen varchar(40) not null,
    recibido_en timestamptz not null default now()
);

-- notificacion: una por evento, destinatario y carrera. Su contenido no cambia; solo leida_en, de
-- nulo a una fecha y una sola vez (principio VII, FR-009):
--   1. ninguna operación de la API la borra ni la edita;
--   2. los disparadores rechazan cualquier otro UPDATE, el DELETE y el TRUNCATE, para cualquier rol;
--   3. el rol que migra, que es el del proceso, solo puede actualizar leida_en.
-- El dueño de la tabla podría quitar los disparadores con DDL: el mismo riesgo residual de la
-- bitácora (specs/039-bitacora-cambios/research.md §3).
create table notificacion (
    id uuid primary key,
    id_evento uuid not null references evento_recibido (id_evento),
    destinatario varchar(64) not null check (length(destinatario) >= 1),
    carrera varchar(20) check (carrera ~ '^[0-9A-Z_-]{1,20}$'),
    periodo varchar(20) not null check (periodo ~ '^[A-Za-z0-9._-]{1,20}$'),
    titulo varchar(200) not null check (length(btrim(titulo)) between 1 and 200),
    detalle varchar(1000) not null,
    enlace varchar(120) not null check (enlace ~ '^/([a-z0-9-]+(/[a-z0-9-]+)*)?$'),
    autor varchar(64) not null check (length(autor) >= 1),
    autor_nombre varchar(120) not null check (length(autor_nombre) >= 1),
    momento timestamptz not null,
    recibida_en timestamptz not null default now(),
    leida_en timestamptz
);

-- Una sola vez por evento, destinatario y carrera (FR-012): una devolución deja una por carrera.
create unique index notificacion_una_por_destinatario
    on notificacion (id_evento, destinatario, coalesce(carrera, ''));

-- La bandeja de un usuario, de la más nueva a la más antigua, y su número sin leer.
create index notificacion_bandeja on notificacion (destinatario, momento desc, id);
create index notificacion_sin_leer on notificacion (destinatario) where leida_en is null;

create function notificacion_solo_lectura() returns trigger
    language plpgsql
as $$
begin
    if tg_op = 'DELETE' then
        raise exception 'Una notificación no se borra.' using errcode = 'insufficient_privilege';
    end if;
    if old.leida_en is null
        and new.leida_en is not null
        and (new.id, new.id_evento, new.destinatario, new.carrera, new.periodo, new.titulo,
             new.detalle, new.enlace, new.autor, new.autor_nombre, new.momento, new.recibida_en)
            is not distinct from
            (old.id, old.id_evento, old.destinatario, old.carrera, old.periodo, old.titulo,
             old.detalle, old.enlace, old.autor, old.autor_nombre, old.momento, old.recibida_en)
    then
        return new;
    end if;
    raise exception 'Una notificación no se modifica ni se borra; solo se marca como leída una vez.'
        using errcode = 'insufficient_privilege';
end;
$$;

create function notificacion_rechazar_cambio() returns trigger
    language plpgsql
as $$
begin
    raise exception 'Las notificaciones y sus eventos no se modifican ni se borran.'
        using errcode = 'insufficient_privilege';
end;
$$;

create trigger notificacion_sin_cambios
    before update or delete on notificacion
    for each row execute function notificacion_solo_lectura();

create trigger notificacion_sin_truncar
    before truncate on notificacion
    for each statement execute function notificacion_rechazar_cambio();

create trigger evento_recibido_sin_cambios
    before update or delete on evento_recibido
    for each row execute function notificacion_rechazar_cambio();

create trigger evento_recibido_sin_truncar
    before truncate on evento_recibido
    for each statement execute function notificacion_rechazar_cambio();

-- El rol del proceso solo inserta, lee y marca como leída. En evento_recibido conserva UPDATE
-- porque la clave foránea de notificacion lo comprueba con FOR KEY SHARE; el disparador rechaza
-- igual cualquier cambio.
revoke update, delete, truncate on notificacion from current_user;
grant update (leida_en) on notificacion to current_user;
revoke delete, truncate on evento_recibido from current_user;
