-- El rechazo en firma (F14-H04, D-105): el firmante en turno no firma y escribe una observación.
-- A lo sumo uno por documento; con él nadie queda en turno y el documento se cierra. Las firmas
-- aceptadas antes se conservan (FR-007).
create table rechazo_en_firma (
    periodo       varchar(20)  not null,
    version       integer      not null,
    tipo          varchar(20)  not null,
    -- El paso en turno al rechazar.
    orden         integer      not null check (orden >= 1),
    rechazado_por varchar(64)  not null,
    rechazado_en  timestamptz  not null,
    -- La observación, de 10 a 500 caracteres sin los espacios de los extremos (RF-36, D-98).
    observacion   varchar(500) not null check (length(btrim(observacion)) between 10 and 500),
    primary key (periodo, version, tipo),
    -- A la generación, como validacion_firma (D-103): verificar la clave bloquea la fila apuntada
    -- con FOR KEY SHARE, y eso exige UPDATE, que V2 le quitó en documento_firmable.
    foreign key (periodo, version, tipo) references generacion_documento
);

-- Como el documento, no cambia (principio VII): el rol del proceso solo inserta y lee.
revoke update, delete, truncate on rechazo_en_firma from current_user;
