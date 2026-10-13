# Contrato: validación de las firmas (PDFBox y Bouncy Castle)

Es lo que garantiza la validación del *spike* (escenarios 2 y 3; FR-005 a FR-008; D-44; research §6).

## `ValidadorPadesDePrueba`

```java
List<ResultadoFirma> validar(byte[] pdf, X509Certificate raiz)
```

Por cada firma del PDF, en el orden del documento:

| Comprobación | Válida si |
|---|---|
| ByteRange | Tiene 4 números, empieza en 0 y cubre todo el archivo hasta el final de su revisión, salvo `/Contents`. |
| Integridad | El *digest* firmado del CMS coincide con el contenido del ByteRange y la firma del firmante se verifica con su certificado. |
| Cadena | Hay un camino PKIX del certificado del firmante hasta `raiz`, en el momento de la firma, sin revocación. |
| Vigencia | El certificado del firmante es válido en el momento de la firma. |

`ResultadoFirma` informa: índice, CN del firmante, momento, y las cuatro comprobaciones con su motivo si fallan.

## `FirmasFirmaecTest` (escenarios 2 y 3)

- **Con los PDF de `ca-prueba/` (CI):**
  - `pdfa-firmado-2.pdf` empieza con los bytes exactos de `pdfa-firmado-1.pdf`;
  - tiene una firma más que este;
  - las 2 firmas son válidas en las cuatro comprobaciones contra `ca-prueba.crt`.

  Si los PDF no están (FirmaEC rechazó la CA de prueba, D-43), la prueba se salta y lo dice.
- **Modo local (D-43):** `-Dmdd.firma.pdf1=<ruta> -Dmdd.firma.pdf2=<ruta> -Dmdd.firma.raiz=<ruta>`. Las mismas comprobaciones contra la raíz indicada; imprime el resultado por firma sin el CN real.

## `CasosNegativosTest` (FR-007)

| Caso | Resultado esperado |
|---|---|
| Un byte cambiado dentro del ByteRange de la primera firma | Integridad no válida en esa firma |
| Validación contra otra raíz (una CA generada en la prueba) | Cadena no válida en todas las firmas |
| PDF firmado en la prueba con un certificado vencido antes del momento de la firma | Vigencia no válida |

Los casos negativos usan PDF firmados por la propia prueba (PDFBox con Bouncy Castle), así que corren siempre en la CI, aunque no estén las firmas de FirmaEC.

## Garantías

1. La validación de las firmas de FirmaEC se repite sola en la CI cuando los PDF firmados con la CA de prueba están en el repositorio (FR-008).
2. Los 3 casos negativos se detectan siempre (SC-002).
3. El código de producción del servicio `firma` no cambia (D-44).
