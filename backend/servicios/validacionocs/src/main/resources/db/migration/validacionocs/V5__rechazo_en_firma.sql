-- El rechazo en firma (F14-H04, D-105, data-model.md): Firma avisa que el firmante en turno no
-- firmó, y el distributivo vuelve de CONGELADO_EN_FIRMA a la etapa activa anterior por el sistema.
-- La transición es de un tipo propio; quién rechazó, en qué paso y su observación van aparte, porque
-- quien registra la transición es el sistema. El nombre del check es el que le dio V4.

alter table transicion alter column tipo type varchar(20);

alter table transicion drop constraint transicion_tipo_check;
alter table transicion add constraint transicion_tipo_check
    check (tipo in ('ENVIO_PARCIAL', 'PASO_DE_ETAPA', 'DEVOLUCION', 'RECHAZO_EN_FIRMA'));

-- Solo de inserción (principio VII), como observacion_de_devolucion: una fila por transición.
create table rechazo_en_firma (
    transicion_id uuid primary key references transicion (id) on delete cascade,
    orden integer not null check (orden >= 1),
    rol varchar(40) not null,
    rechazado_por varchar(64) not null,
    rechazado_en timestamptz not null,
    observacion varchar(500) not null check (length(btrim(observacion)) between 10 and 500)
);
