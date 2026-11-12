# Contrato: SPA

## Tarjeta del flujo (`pantallas/comun/EstadoDelFlujo.tsx`)

En CONGELADO_EN_FIRMA, debajo de la constancia de la congelación, la sección «Documento firmable» (`DocumentoFirmable.tsx`, nuevo) consulta `consultarDocumentoFirmable` (`comun/documento.ts`):

| Estado | Muestra |
|---|---|
| `GENERADO` | «Documento firmable generado el <fecha>» y «Huella del PDF: <12 caracteres>…» con «Copiar la huella» (el mismo botón de la constancia) |
| `EN_CURSO` o `SIN_GENERAR` | «Generando el documento firmable…», con `role="status"`; vuelve a consultar cada 3 s, hasta 1 minuto |
| `FALLIDA` | Aviso de error «El PDF no se pudo generar» con el motivo |

Con la acción `GENERAR`, el botón «Generar el PDF»:
- llama a `generarDocumentoFirmable`;
- se desactiva mientras espera;
- si responde 409, muestra el mensaje del catálogo y los detalles.

Sin descarga (F14-H01).

## Accesibilidad

El estado se anuncia con `role="status"`, y el aviso de error con `role="alert"`. axe-core (WCAG 2.1 AA) sin fallas con cada estado.
