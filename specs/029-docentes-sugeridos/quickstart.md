# Quickstart: comprobar la sugerencia de docentes

Requisitos: el perfil consolidado levantado (`docker compose up -d --wait`), con las cargas de prueba confirmadas y 2026-I abierto.

## 1. El orden (US1)

- `SugerenciaTest`: el orden, el empate por horas y por nombre, y «Sin horas suficientes», sin dobles.
- Por la API, con `coordinacion.demo`: `GET /api/generacion/periodos/2026-I/oferta/{asignatura}/{paralelo}/sugerencia` responde los candidatos con su desglose.
- El docente en licencia de los datos no aparece. Uno activo que pasa a licencia desaparece; al volver a Activo, aparece otra vez.
- Aparece un candidato cuya carrera base no es la de la asignatura.

## 2. El mínimo (US2)

- En una asignatura sin tronco común, nadie tiene menos que el mínimo, y `fueraPorElMinimo` cuenta a los que quedaron fuera.
- En una asignatura de tronco común aparecen todos los activos, también con 0 puntos.
- Con otra versión y otro mínimo, abierta en otro periodo, cambia quién aparece.

## 3. Las horas (US3)

- `SugerenciaTest`, con horas simuladas: las horas disponibles, nunca negativas, y el lugar en la lista.
- Por la API: todos con 0 h en otras carreras y sus horas disponibles iguales a las contratadas.
- Con datos reales, al cerrar F06-H03.

## 4. La pantalla

- Con `coordinacion.demo`, en «Oferta», pulsa «Sugerencia» en una fila ofertada y despliega un candidato.
- Una fila retirada no ofrece la acción.

## 5. Errores y permisos

- Un paralelo que no existe: 404 `GENERACION_PARALELO_NO_ENCONTRADO`.
- Un paralelo retirado: 409 `GENERACION_PARALELO_RETIRADO`.
- Un periodo sin abrir: 409 `GENERACION_PERIODO_SIN_ABRIR`.
- Un código mal escrito: 400 `COMUN_SOLICITUD_INVALIDA`.
- Un usuario sin permiso: 403.
- `/interno/docentes/activos` con un token de usuario: 403, también por el API Gateway, que no la enruta.
- Con el proceso `datos` detenido: 502 o 504, con el mensaje del catálogo, en menos de 10 s.

## 6. Punta a punta

- `deploy/probar_sugerencia.sh` repite estos pasos y deja la base limpia.
