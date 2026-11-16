# Contrato: SPA

## La pantalla «Resolución del OCS» (`/resolucion-ocs`)

En el menú y con su ruta para quien tiene `RESOLUCION_OCS:LECTURA` o `RESOLUCION_OCS:ESCRITURA`: el selector de periodo y el estado del flujo del periodo (`EstadoDelFlujoDelPeriodo`). La Secretaría general no abre «Consolidado del periodo»: no lee el consolidado (D-16). Lee los periodos y el flujo con `DISTRIBUTIVO:LECTURA`.

## El estado del flujo (`EstadoDelFlujo.tsx`), en «Resolución del OCS» y en «Consolidado del periodo»

**Con `REGISTRAR_RESOLUCION` en `acciones`**, un formulario «Registrar la resolución del OCS»:
- los campos «Número de la resolución», «Fecha de la resolución» (fecha, sin futura) y «PDF de la resolución»;
- «Registrar la resolución» abre una confirmación: «El distributivo quedará aprobado con esta resolución. Después no se corrige.»;
- un 400 o un 413 se muestran en el formulario, con su descripción del catálogo; otro fallo, el aviso común;
- al terminar, recarga el flujo.

**Con `resolucion`**, la constancia «Resolución del OCS n.º {número}»:
- «del {fecha}, registrada por {quién} el {cuándo}»;
- la huella copiable, como la de la congelación;
- el botón «Descargar la resolución», que baja el PDF con su nombre.

## Accesibilidad

Etiquetas en los tres campos, errores asociados a su campo, la confirmación como diálogo con el foco adentro y el teclado completo. Playwright con axe-core (WCAG 2.1 AA).
