# Contrato: SPA

## `pantallas/firmas/FirmasPendientes.tsx` (ampliada)

Cada fila suma «Subir el PDF firmado», junto a «Descargar el PDF».

**El botón:**
- abre el selector de archivos del sistema (`input type="file"`, `accept="application/pdf"`, oculto y con su etiqueta);
- al elegir el archivo, lo sube con `subirFirma` (`firmas/consultas.ts`) como `multipart/form-data`;
- se desactiva mientras sube, igual que «Descargar el PDF» de esa fila.

| Resultado | Muestra |
|---|---|
| `201` | Un aviso de éxito, «Firma registrada», con el informe; la lista se vuelve a pedir y la fila desaparece |
| `422` | Un aviso de error con el título y la descripción del catálogo, y el informe armado desde `detalles` |
| Otro error | `AvisoDeError` con el mensaje del catálogo |

**El informe** es una lista con las cuatro comprobaciones, en orden. Cada una con su nombre y su resultado:
- «Es la versión vigente»;
- «Tiene una sola firma nueva»;
- «La firma es íntegra»;
- «El certificado es de confianza».

El resultado es «Aprobada», «Rechazada» o «No evaluada», con el motivo si lo hay. Con el certificado, debajo: el nombre, el emisor y la vigencia.

El aviso queda arriba de la tabla hasta que se sube otro archivo o se cierra.

## Accesibilidad

- Cada botón nombra su fila («Subir el PDF firmado de 2026-I, versión 1»).
- El aviso tiene `role="status"` (aceptada) o `role="alert"` (rechazada).
- El resultado no depende solo del color: lleva el texto.
- axe-core (WCAG 2.1 AA) sin fallas, con el informe aceptado y con un rechazo (SC-006).
