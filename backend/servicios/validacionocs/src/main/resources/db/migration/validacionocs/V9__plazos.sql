-- Los plazos de cada etapa (F12-H03, D-114). Un vencimiento por llegada a la etapa: la clave única
-- es lo que impide repetir el aviso, aunque la revisión corra al arrancar y por el cron el mismo
-- día. Solo de inserción (principio VII). Sin referencia a proceso_aprobacion: un distributivo
-- recreado no borra el registro de sus vencimientos.
create table plazo_vencido (
    id           uuid         primary key,
    periodo      varchar(20)  not null,
    etapa        varchar(30)  not null
        check (etapa in ('BORRADOR_CARRERA', 'BORRADOR_UNIDADES', 'EN_CONSOLIDACION',
                         'EN_REVISION_VRA', 'CONGELADO_EN_FIRMA', 'EN_OCS', 'REMITIDO_ZONAL',
                         'APROBADO')),
    llegada      timestamptz  not null,
    plazo_dias   integer      not null check (plazo_dias >= 1),
    fecha_limite date         not null,
    detectado_en timestamptz  not null check (detectado_en >= llegada),
    unique (periodo, etapa, llegada)
);

-- La advertencia de RN-23 con la que se cargó cada configuración; nula si no hubo (FR-014).
alter table configuracion_flujo add column advertencia jsonb;
