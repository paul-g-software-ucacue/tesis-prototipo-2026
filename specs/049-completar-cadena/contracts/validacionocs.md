# Contrato: Aprobación (`validacionocs`)

Sin operaciones HTTP nuevas. `consultarFlujo` ya muestra la etapa y las transiciones: después de la cadena completa, la etapa actual es EN_OCS y la última transición es `PASO_DE_ETAPA`, de CONGELADO_EN_FIRMA a EN_OCS, por `sistema`.

**El aviso dentro del proceso** (MD-05, research §3): `ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta(periodo, version, momento)`. Lo publica Firma con la última firma aceptada y lo escucha Aprobación con `@ApplicationModuleListener`.

**Bitácora:** la transición se registra como los demás pasos de etapa, del servicio `validacionocs`.
