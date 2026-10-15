-- Eventos de otros procesos que Usuarios ya registró (B00-H05, principio V, ADR-018). La clave
-- primaria hace la idempotencia: un id_evento se registra una sola vez, también con entregas
-- simultáneas. Nadie la modifica ni la borra.
create table evento_recibido (
    id_evento uuid primary key,
    tipo text not null,
    origen text not null,
    momento timestamptz not null,
    recibido_en timestamptz not null default now()
);
