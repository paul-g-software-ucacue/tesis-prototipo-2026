# Contrato: procedimiento de firma con FirmaEC (manual, el autor)

Es lo que hace el autor con FirmaEC y lo que anota (escenarios 1 y 2; FR-002, FR-004; D-43; research §5).

## Antes de firmar

- FirmaEC instalado. Anotar la versión y cómo se instaló en Fedora (paquete, distribución en Java u otro camino).
- `backend/servicios/firma/src/test/resources/ca-prueba/` tiene `firmante-1.p12`, `firmante-2.p12`, `firmante-3.p12`, sus variantes `-legacy` y `ca-prueba.crt` (generados con `generar.sh`).
- `backend/servicios/firma/build/spike-firma/pdfa-prueba.pdf` generado y validado como PDF/A-2b por `PdfaDePruebaTest`. Se copia a `ca-prueba/pdfa-prueba.pdf`.

## Pasos

| # | Acción en FirmaEC | Archivo de entrada | Certificado | Archivo de salida | Qué anotar |
|---|---|---|---|---|---|
| 1 | Firmar una copia desechable con cada certificado | copia de `pdfa-prueba.pdf` | `firmante-1.p12`, `firmante-2.p12` y `firmante-3.p12`; si alguno falla, su variante `-legacy` | (se descarta) | Si FirmaEC firma con cada uno y con qué variante; si no, el mensaje exacto (escenario 1) |
| 2 | Firmar el documento | `pdfa-prueba.pdf` | `firmante-1.p12` | `pdfa-firmado-1.pdf` | Opciones ofrecidas (firma visible o invisible, página, posición) y las elegidas; algún aviso |
| 3 | Firmar el documento ya firmado | `pdfa-firmado-1.pdf` | `firmante-2.p12` | `pdfa-firmado-2.pdf` | Ídem; si FirmaEC avisa que el PDF ya tiene una firma |
| 4 | Copiar los dos PDF a `ca-prueba/` | — | — | — | — |

## Si FirmaEC no acepta la CA de prueba (D-43)

1. Anotar el mensaje y cerrar el escenario 1 como «no funcionó».
2. Repetir los pasos 2 y 3 con el certificado real del autor, **solo en su equipo**, en una carpeta fuera del repositorio. Si hay un solo certificado real, las dos firmas son con el mismo, y se anota la desviación.
3. Descargar la raíz pública (y la intermedia, si hay) de la entidad emisora del certificado real, también fuera del repositorio.
4. Ejecutar la validación en modo local ([validacion.md](validacion.md)).
5. Al repositorio solo va el resultado, en `evidencia.md`: la comparación de bytes y la salida de la validación, sin el certificado, los PDF firmados ni datos personales del certificado (el CN real no se copia).

## Garantías

1. Los pasos y las respuestas de FirmaEC quedan anotados en `evidencia.md` con la versión de FirmaEC.
2. Ningún certificado real ni PDF firmado con él entra al repositorio.
3. Los únicos `.p12` del repositorio son los de la CA de prueba, en `ca-prueba/`.
