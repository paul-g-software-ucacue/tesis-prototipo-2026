-- La resolución del OCS (F15-H01, RF-37, D-107): el número, la fecha y el PDF con los que la
-- Secretaría general académica aprueba el distributivo. Va con su transición de EN_OCS a APROBADO,
-- como el rechazo en firma con la suya (V5). Una por versión aprobada: una reforma (F18-H02)
-- aprobará otra versión con otra resolución. El PDF va en el volumen, por su huella; sus firmas no
-- se validan (D-19). Solo de inserción por el código, como las demás tablas del servicio.
create table resolucion_ocs (
    transicion_id uuid         primary key references transicion (id) on delete cascade,
    periodo       varchar(20)  not null,
    version       integer      not null check (version >= 1),
    -- De 1 a 60 caracteres sin los espacios de los extremos; sin formato fijo.
    numero        varchar(60)  not null check (length(btrim(numero)) between 1 and 60),
    fecha         date         not null,
    sha256        char(64)     not null check (sha256 ~ '^[0-9a-f]{64}$'),
    -- El nombre recibido, sin ruta; solo se muestra.
    nombre        varchar(150) not null check (length(nombre) > 0),
    tamano        integer      not null check (tamano between 1 and 10485760),
    unique (periodo, version)
);
