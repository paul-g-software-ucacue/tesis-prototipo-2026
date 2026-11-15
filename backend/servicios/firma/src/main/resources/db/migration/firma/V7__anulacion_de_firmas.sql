-- La anulación de las firmas de un documento (F12-H04, D-106): Aprobación la ordena cuando la
-- versión firmada se reemplaza. Las firmas, sus archivos y sus validaciones no cambian; quedan
-- anuladas porque el documento lo está (glosario: «Se conservan en el historial»). A lo sumo una por
-- documento; con ella nadie queda en turno.
create table anulacion_de_firmas (
    periodo         varchar(20) not null,
    version         integer     not null,
    tipo            varchar(20) not null,
    motivo          varchar(20) not null check (motivo in ('RECHAZO_EN_FIRMA')),
    -- Las firmas aceptadas que tenía el documento al anularse.
    firmas_anuladas integer     not null check (firmas_anuladas >= 0),
    anulada_por     varchar(64) not null,
    anulada_en      timestamptz not null,
    primary key (periodo, version, tipo),
    -- A la generación, como rechazo_en_firma (V6) y por la misma razón.
    foreign key (periodo, version, tipo) references generacion_documento
);

-- No cambia (principio VII): el rol del proceso solo inserta y lee.
revoke update, delete, truncate on anulacion_de_firmas from current_user;
