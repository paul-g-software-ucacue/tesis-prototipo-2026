# Contrato: Validación OCS (en el proceso, sin HTTP nuevo)

## Evento interno `DistributivoEnFirma`

```java
/**
 * El distributivo pasó a CONGELADO_EN_FIRMA con su versión congelada (F13-H02, D-101). Se publica
 * en la transacción que registra el paso; lo guarda el registro de Spring Modulith (ADR-018).
 */
record DistributivoEnFirma(
        String periodo, int version, String hash, List<PasoDeLaCadena> cadena,
        String rolDeLaEtapa, Instant momento) {}
```

Lo escucha el propio módulo (`PreparacionDelDocumento`) después de confirmar, y llama a la API publicada de Firma `PrepararDocumentoFirmable.preparar` (MD-05: Aprobación le entrega la cadena a Firma).

Se publica en los cuatro caminos que llegan a CONGELADO_EN_FIRMA:
- aprobar el contenido;
- el envío a revisión con EN_REVISION_VRA desactivada;
- el último parcial;
- el avance genérico.

Un rechazo no publica nada.

Sin API publicada nueva: Firma no le pregunta nada a Aprobación.
