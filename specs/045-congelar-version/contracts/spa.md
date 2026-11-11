# Contrato: SPA

## Tarjeta del flujo (`pantallas/comun/EstadoDelFlujo.tsx`)

- **El botón.** Con la acción `APROBAR_CONTENIDO`, muestra «Aprobar el contenido».
- **El diálogo** (`DialogoDeAprobacion.tsx`, nuevo):
  - título «Aprobar el contenido»;
  - texto «La versión N queda congelada: es la que se firmará. Todo cambio posterior exigirá una versión nueva.»;
  - botones «Aprobar y congelar» y «Cancelar».
  - Llama a `aprobarContenido` en `comun/flujo.ts`. Si hay error, muestra el mensaje del catálogo en el diálogo.
- **La constancia.** Con `congelacion`, muestra «Versión N congelada por <quien> el <fecha>» y la huella abreviada (12 primeros caracteres y «…»). Al lado, un botón accesible «Copiar la huella», que copia los 64 caracteres y confirma «Huella copiada».
- **A mitad de camino.** Si la versión está congelada pero la etapa sigue en EN_REVISION_VRA (la segunda transacción falló), no hay `congelacion` y sí `APROBAR_CONTENIDO`: la tarjeta sigue ofreciendo «Aprobar el contenido», sin constancia, y el consolidado y el panel ya muestran el aviso de versión congelada. Volver a aprobar lo completa.

## Consolidado (`pantallas/consolidado/Consolidado.tsx`) y panel (`pantallas/inconsistencias/Inconsistencias.tsx`)

- `puedeEscribir` y `puedeJustificar` también exigen `!versionCongelada`.
- Con `versionCongelada`, el aviso es «La versión N está congelada para la firma: ya no cambia.». Reemplaza al de revisión (#215). Sin congelar y con `enRevision`, sigue el de #215.
- El informe sigue con «Descargar el informe».

## Propuestas y actividades

Sin cambios. Con la versión congelada, los parciales están enviados y ya no ofrecen editar.

## Accesibilidad

El diálogo y el botón de copiar pasan axe-core (WCAG 2.1 AA). El diálogo enfoca «Cancelar» al abrirse y se cierra con Escape.
