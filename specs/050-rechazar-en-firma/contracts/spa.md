# Contrato: SPA

## «Pendientes de firma» (`FirmasPendientes.tsx`)

- Cada fila suma el botón **«Rechazar»**, junto a «Subir el PDF firmado».
- El diálogo «Rechazar en firma»:
  - un campo «Observación» obligatorio, con la ayuda «De 10 a 500 caracteres» y un contador;
  - «Cancelar» y «Rechazar en firma». El segundo no envía si la observación, recortada, tiene menos de 10 caracteres; lo dice junto al campo;
  - con 400 del servidor, el mensaje del catálogo junto al campo;
  - con 403, el mensaje común y la lista se vuelve a pedir.
- Con 201: el aviso «Rechazo registrado» con el paso y la observación; la lista se vuelve a pedir y la fila ya no está.
- Teclado y lector de pantalla: el diálogo atrapa el foco, el campo tiene su etiqueta y su ayuda asociadas, y el foco vuelve al botón al cerrar. axe-core sin fallas (WCAG 2.1 AA).

## Estado del flujo (`EstadoDelFlujo.tsx`)

- Con `rechazoEnFirma`: un aviso «Rechazado en firma» con el paso («Paso N · rol»), quién, cuándo y la observación, y la nota «Para aprobar otra vez hace falta una versión nueva».
- Sin el botón «Aprobar el contenido»: sale de `acciones`.
