-- La versión nueva que crea un rechazo en firma (F12-H04, D-106): una transición de la etapa a la
-- que volvió el distributivo a la misma, por el sistema, con la versión nueva y su hash.
alter table transicion drop constraint transicion_tipo_check;
alter table transicion add constraint transicion_tipo_check
    check (tipo in ('ENVIO_PARCIAL', 'PASO_DE_ETAPA', 'DEVOLUCION', 'RECHAZO_EN_FIRMA',
                    'VERSION_NUEVA'));
