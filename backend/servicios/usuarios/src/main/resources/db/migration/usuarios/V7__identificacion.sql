-- La identificación de la persona de cada cuenta (F14-H01, D-102): la cédula o el pasaporte, con el
-- formato de Docentes. Opcional y única: así Firma reconoce al docente de un cargo firmante. Es un
-- dato personal: no va en el token ni en las respuestas públicas.
alter table usuario add column identificacion varchar(13)
    check (identificacion ~ '^[0-9A-Z]{1,13}$');
create unique index usuario_identificacion on usuario (identificacion)
    where identificacion is not null;
