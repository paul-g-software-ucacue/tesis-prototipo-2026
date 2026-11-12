# Contrato: Validación OCS (en el proceso, sin HTTP nuevo)

## Evento `DistributivoEnFirma` (interfaz nombrada `eventos`)

```java
/**
 * El distributivo pasó a CONGELADO_EN_FIRMA con su versión congelada (F13-H02, D-101). Se publica
 * en la transacción que registra el paso; lo guarda el registro de Spring Modulith (ADR-018).
 */
public record DistributivoEnFirma(String periodo, int version, String hash, Instant momento) {}
```

Se publica en los cuatro caminos que llegan a CONGELADO_EN_FIRMA:
- aprobar el contenido;
- el envío a revisión con EN_REVISION_VRA desactivada;
- el último parcial;
- el avance genérico.

Un rechazo no publica nada.

## API publicada nueva (`puertos.entrada`)

- `ArbolDeFirmas.firmas(String periodo)` → `List<FirmaDelArbol(int orden, String etiqueta, String rol)>` del documento CONSOLIDADO, de la copia de la configuración; `SinDistributivo` si no hay.
- `EtapaParaFirma.de(String periodo, Solicitante quien)` → `{TipoDeEtapa etapa, int version, boolean tieneElRolDeFirma}`. El rol es el de CONGELADO_EN_FIRMA en la copia.
