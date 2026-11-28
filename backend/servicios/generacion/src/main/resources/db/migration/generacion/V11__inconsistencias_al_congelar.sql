-- Las inconsistencias abiertas de la versión cuando se guarda su contenido congelado (F19-H02,
-- D-120): las duras y las blandas sin justificar del panel (MD-07). El panel se calcula al abrirlo,
-- con la oferta y las reglas del día; fijarlas aquí permite comprobar, al exportar el CSV para SIGA,
-- que la versión firmada no tenía inconsistencias abiertas (RF-53).
--
-- Vacías en un contenido guardado antes de esta migración, o tarde, cuando la versión ya no era la
-- vigente: no se adivinan. Las dos se llenan juntas o ninguna. Se escriben en el insert; el
-- disparador contenido_congelado_sin_cambios sigue rechazando cualquier update.
alter table contenido_congelado
    add column duras_abiertas   integer check (duras_abiertas >= 0),
    add column blandas_abiertas integer check (blandas_abiertas >= 0),
    add constraint inconsistencias_juntas
        check ((duras_abiertas is null) = (blandas_abiertas is null));
