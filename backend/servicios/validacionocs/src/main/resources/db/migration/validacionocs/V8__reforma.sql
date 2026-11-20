-- La reforma (F18-H02, D-113): el Vicerrector la inicia con el distributivo publicado, y vuelve a
-- EN_CONSOLIDACION con una versión nueva. La transición es de un tipo propio; el motivo y la versión
-- publicada que se reforma van aparte, como el rechazo en firma (V5) y la resolución (V7). El nombre
-- del check es el que le dio V4.
alter table transicion drop constraint transicion_tipo_check;
alter table transicion add constraint transicion_tipo_check
    check (tipo in ('ENVIO_PARCIAL', 'PASO_DE_ETAPA', 'DEVOLUCION', 'RECHAZO_EN_FIRMA',
                    'VERSION_NUEVA', 'REFORMA'));

-- Solo de inserción (principio VII): una fila por transición de reforma.
create table reforma (
    transicion_id     uuid primary key references transicion (id) on delete cascade,
    periodo           varchar(20)  not null,
    version_publicada integer      not null check (version_publicada >= 1),
    motivo            varchar(500) not null check (length(btrim(motivo)) between 10 and 500)
);
