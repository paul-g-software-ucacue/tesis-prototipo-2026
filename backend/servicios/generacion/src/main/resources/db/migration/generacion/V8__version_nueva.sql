-- F12-H04 (D-106): la versión nueva que crea un rechazo en firma. Cada versión registra su motivo,
-- quién y cuándo (diseño de firmas, tabla distributivo_version); la versión 1 es la inicial, sin
-- motivo. Las versiones que ya existían no tienen autor ni momento.

alter table version_distributivo
    add column motivo varchar(20) check (motivo in ('RECHAZO_EN_FIRMA')),
    add column creada_por varchar(64),
    add column creada_en timestamptz,
    add constraint version_distributivo_motivo_solo_desde_la_2 check ((numero = 1) = (motivo is null));

-- La versión nueva comparte el archivo del informe con la anulada: el almacén nunca lo reescribe ni
-- lo borra. La clave sigue siendo única dentro de cada versión.
alter table informe_sustentado drop constraint informe_sustentado_clave_key;
alter table informe_sustentado
    add constraint informe_sustentado_version_clave_key unique (version_id, clave);
