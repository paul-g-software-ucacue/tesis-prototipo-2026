# Quickstart: comprobar la carrera del coordinador

Requisitos: el perfil consolidado levantado (`docker compose up -d --wait`), con las cargas de prueba confirmadas y 2026-I abierto.

## 1. Indicar la carrera (US1)

- Con `admin.demo`, en «Roles» → «Usuarios», indica una carrera a `coordinacion.demo`.
- La tabla la muestra junto a sus roles.
- A un usuario sin el rol: 409 `ROLES_CARRERA_SIN_ROL`.
- Con `coordinacion.demo`: 403 por la matriz.
- Al quitarle el rol, su carrera se borra.

## 2. Solo su carrera (US2)

- Con `coordinacion.demo`, la oferta de 2026-I trae solo su carrera y sus horas a cubrir.
- La sugerencia de un paralelo de su carrera responde 200; la de otra, 403.
- La afinidad de un docente trae solo asignaturas de su carrera.

## 3. Sin carrera (US3)

- Antes de indicarla: la oferta, la sugerencia y la afinidad responden 403 `COMUN_CARRERA_SIN_INDICAR`, y la pantalla muestra el aviso.

## 4. Alcance total (US4)

- Con `vicerrectorado.demo`: las 7 carreras en la oferta y sugerencias de todas.
- Con `datos.demo`: la afinidad como siempre.

## 5. Punta a punta

- `deploy/probar_carrera_coordinador.sh` repite estos pasos y deja la base limpia.
