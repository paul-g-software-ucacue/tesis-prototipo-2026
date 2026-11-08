# Contrato: devolver y ver la devolución en la SPA

**`EstadoDelFlujo`** (`frontend/src/pantallas/comun/EstadoDelFlujo.tsx`):
- **Un parcial con `devolucion`:**
  - dice «<carrera>: devuelto por <por> el <en>»;
  - debajo, la observación, en un bloque con su etiqueta «Observación»;
  - se ve en «Oferta del periodo» y en «Consolidado del periodo» (SC-006).
- **El coordinador con el parcial devuelto** ve el botón «Reenviar parcial» (la acción `ENVIAR_PARCIAL`).

**`Consolidado.tsx`** (`frontend/src/pantallas/consolidado/`):
- **El botón:** con `DEVOLVER_PARCIALES` en `acciones`, «Devolver parciales», junto a «Enviar a revisión».
- **El diálogo** «Devolver parciales»:
  - las carreras con el parcial enviado, cada una con su casilla;
  - al marcar una, aparece su campo «Observación para <carrera>», de varias líneas, con el contador «N de 500» y el error en el campo si tiene menos de 10 caracteres sin espacios;
  - «Devolver» queda deshabilitado mientras no haya al menos una carrera marcada o falte una observación válida;
  - el texto de la confirmación: «Las carreras marcadas vuelven a editar su parcial y el distributivo vuelve a Propuesta de carrera. Cuando reenvíen, vuelve a Consolidación.» (con el nombre de la etapa de la configuración).
- **Al devolver,** se invalidan el flujo y el consolidado, y el diálogo se cierra.
- **Si falla,** el error se muestra con `AvisoDeError`, dentro del diálogo, con sus `detalles`.

**`flujo.ts`** suma `devolverParciales(periodo, devoluciones)`.

**Pruebas:**
- **Vitest de `EstadoDelFlujo`:**
  - un parcial devuelto con quién, cuándo y la observación;
  - «Reenviar parcial» para el coordinador.
- **Vitest de `Consolidado`:**
  - el botón solo con la acción;
  - el diálogo: marcar, escribir, el contador, el error de menos de 10, «Devolver» deshabilitado;
  - la petición con las carreras y sus observaciones;
  - el 409 en el diálogo.
- **Playwright `e2e/consolidado.spec.ts`:**
  - el Responsable devuelve dos carreras con la API simulada y ve el flujo en «Propuesta de carrera» con las observaciones;
  - axe-core sin fallas, con el diálogo abierto.
- **Playwright `e2e/oferta.spec.ts`:** el coordinador ve la observación de su parcial devuelto y «Reenviar parcial».
