-- Devolver desde EN_CONSOLIDACION (F12-H05, D-98, data-model.md): el parcial devuelto, la
-- transición de devolución con la observación de cada carrera, y si cada etapa admite devolución en
-- la copia de la configuración del proceso. Los nombres de los check son los que PostgreSQL les dio
-- en V3.

alter table parcial_del_proceso drop constraint parcial_del_proceso_estado_check;
alter table parcial_del_proceso add constraint parcial_del_proceso_estado_check
    check (estado in ('PENDIENTE', 'ENVIADO', 'DEVUELTO'));

alter table transicion drop constraint transicion_tipo_check;
alter table transicion add constraint transicion_tipo_check
    check (tipo in ('ENVIO_PARCIAL', 'PASO_DE_ETAPA', 'DEVOLUCION'));

-- Solo de inserción (principio VII): la observación de cada carrera de una devolución.
create table observacion_de_devolucion (
    transicion_id uuid not null references transicion (id) on delete cascade,
    carrera varchar(20) not null,
    texto varchar(500) not null check (length(btrim(texto)) between 10 and 500),
    primary key (transicion_id, carrera)
);

-- Los procesos creados antes guardaron la copia sin admiteDevolucion. Se completa con el valor de
-- flujo-por-defecto.yaml para cada etapa, que es la configuración que usaban (research §4).
update proceso_aprobacion
set etapas = (
    select jsonb_agg(
               etapa || jsonb_build_object(
                   'admiteDevolucion',
                   etapa ->> 'tipo' in ('EN_CONSOLIDACION', 'EN_REVISION_VRA',
                                        'CONGELADO_EN_FIRMA', 'EN_OCS'))
               order by orden)
    from jsonb_array_elements(etapas) with ordinality as copia (etapa, orden)
)
where exists (
    select 1 from jsonb_array_elements(etapas) as copia (etapa)
    where not etapa ? 'admiteDevolucion'
);
