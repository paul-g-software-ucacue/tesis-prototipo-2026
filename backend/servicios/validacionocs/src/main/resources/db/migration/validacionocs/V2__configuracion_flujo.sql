-- La configuración del flujo y del árbol de firmas de cada periodo (F12-H01, RF-34, RF-58, D-89). Se
-- guarda el archivo tal como se subió, con sus comentarios. Es solo de inserción: una carga nueva
-- agrega una fila y la vigente es la última (principio VII). Sin referencia al periodo: Académico es
-- otro esquema (principio V).

create table configuracion_flujo (
    id uuid primary key,
    periodo varchar(20) not null,
    contenido text not null check (length(contenido) > 0),
    cargada_por varchar(64) not null,
    cargada_en timestamptz not null
);

create index configuracion_vigente on configuracion_flujo (periodo, cargada_en desc);
