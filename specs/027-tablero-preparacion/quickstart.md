# Quickstart: comprobar el tablero de preparación

Requisitos: el perfil consolidado levantado (`docker compose up -d --wait`) con una base sin datos de prueba, y los usuarios de la demo.

## 1. Falta una carga obligatoria (escenario 1)

- Con `datos.demo`, confirma las cargas 01 a 05 y 07 de prueba, sin la 06 (docentes). La 08 y la 09 dependen de la 06, así que tampoco entran; confirma 2026-I «sin proyectos».
- Con `vicerrectorado.demo`, abre «Preparación del periodo» y elige 2026-I:
  - «Docentes contratados» y «Cargos y autoridades» dicen «Pendiente»; los proyectos, «Sin proyectos»;
  - lo que falta las nombra;
  - «Crear distributivo» está deshabilitado y su descripción dice qué falta.

## 2. Cargas completas (escenario 2)

- Confirma la 06 y la 08, y abre 2026-I con su versión de normativa (pantalla «Periodos», F02-H01).
- En el tablero de 2026-I:
  - las 9 obligatorias están «Cargada» (o la 9, «Sin proyectos»);
  - no falta nada;
  - «Crear distributivo» está habilitado, aunque falten los matriculados.
- Al pulsarlo, el aviso dice que la construcción llega con F06. No se crea nada.

## 3. Por periodo, con errores y permisos

- 2026-II, sin oferta propia: la oferta dice «Pendiente».
- Una carga rechazada al confirmar, sin datos: «Con errores» y su motivo.
- `coordinacion.demo` ve el tablero sin el botón; `docente.demo` recibe 403.
- `?periodo=2027-I` da 400 `CARGAS_PERIODO_NO_ENCONTRADO`, y `?periodo=2026-III` da 400 `COMUN_SOLICITUD_INVALIDA`.

## 4. Punta a punta

- `deploy/probar_preparacion.sh` repite los pasos 1 a 3 por la API y deja la base limpia.
