-- Versiones nuevas y su asociación a periodos (F04-H02, RF-19). Una versión nueva se copia de otra
-- (copiada_de); una versión que rige un periodo ya no cambia (principio VII). Un periodo, nombrado
-- por el código que usa Académico, tiene una sola versión (asignación normativa, glosario §7.3).
-- La versión anterior del servicio, que solo conoce V1 y V2, sigue arrancando sobre este esquema
-- (D-55): solo se agregan una columna nula, una tabla y dos acciones.

alter table version_normativa
    add column copiada_de int references version_normativa (numero);

create table asignacion_normativa (
    version_id uuid not null references version_normativa (id),
    periodo varchar(20) not null unique check (periodo ~ '^[A-Za-z0-9._-]{1,20}$'),
    asociada_en timestamptz not null,
    asociada_por varchar(64) not null,
    primary key (version_id, periodo)
);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check
    check (accion in ('VERSION_CREADA', 'VERSION_EDITADA', 'VERSION_ASOCIADA'));
