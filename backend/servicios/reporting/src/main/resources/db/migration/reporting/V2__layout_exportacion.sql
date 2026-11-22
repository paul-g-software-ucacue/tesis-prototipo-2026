-- El layout de exportación del CSV para SIGA (F19-H01, RF-52, ADR-002, D-119): cada archivo de
-- mapeo válido que carga la Secretaría general académica, tal cual, con su SHA-256, quién y cuándo.
-- El archivo es la fuente: conserva sus comentarios y se valida de nuevo al leerlo.
--
-- Cada fila es una versión del layout de exportación; rige la de número más alto. La versión 0 es
-- el layout por defecto, que sale de la plantilla del servicio y no se guarda aquí.
--
-- No se modifica ni se borra (principio VII): una carga nueva es una fila nueva, y las anteriores
-- quedan como historial.
create table layout_exportacion (
    numero      integer      primary key check (numero >= 1),
    texto       text         not null check (octet_length(texto) <= 262144),
    sha256      char(64)     not null check (sha256 ~ '^[0-9a-f]{64}$'),
    cargado_por varchar(100) not null,
    cargado_en  timestamptz  not null
);

create function layout_exportacion_sin_cambios() returns trigger
    language plpgsql
as $$
begin
    raise exception 'Una versión del layout de exportación no se puede modificar ni borrar.'
        using errcode = 'insufficient_privilege';
end;
$$;

create trigger layout_exportacion_sin_cambios
    before update or delete on layout_exportacion
    for each row execute function layout_exportacion_sin_cambios();
