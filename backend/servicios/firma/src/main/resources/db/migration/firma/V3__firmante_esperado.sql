-- El firmante esperado de cada paso del documento firmable (F14-H01, D-102): la persona cuyo nombre
-- imprime el PDF, fijada al generarlo. El Responsable designado, por su usuario; el Vicerrector y
-- el Rector, por la identificación del docente del cargo vigente.
create table firmante_esperado (
    periodo        varchar(20) not null,
    version        integer     not null,
    tipo           varchar(20) not null,
    orden          integer     not null check (orden >= 1),
    rol            varchar(40) not null,
    usuario        varchar(64),
    identificacion varchar(13) check (identificacion ~ '^[0-9A-Z]{1,13}$'),
    primary key (periodo, version, tipo, orden),
    -- A la generación y no a documento_firmable: verificar la clave bloquea la fila apuntada con
    -- FOR KEY SHARE como su dueño, y eso exige UPDATE, que V2 le quitó en documento_firmable.
    foreign key (periodo, version, tipo) references generacion_documento,
    check ((usuario is null) <> (identificacion is null))
);

-- Como el documento, no cambia (principio VII): el rol del proceso solo inserta y lee.
revoke update, delete, truncate on firmante_esperado from current_user;
