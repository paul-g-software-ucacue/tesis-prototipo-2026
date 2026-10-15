-- Eventos de prueba publicados (B00-H05, D-48): el cambio del servicio que se confirma en la misma
-- transacción que publica el evento, aunque el proceso de identidad esté caído.
create table evento_prueba (
    id_evento uuid primary key,
    momento timestamptz not null
);
