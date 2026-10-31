# Quickstart: comprobar el puntaje de afinidad

Requisitos: el perfil consolidado levantado (`docker compose up -d --wait`), con las cargas de prueba confirmadas y 2026-I abierto.

## 1. El cálculo (US1)

- `CalculadoraAfinidadTest`: los 8 escenarios, sin dobles.
- Por la API, con `coordinacion.demo`: `GET /api/generacion/docentes/{identificacion}/afinidad?periodo=2026-I` responde las asignaturas de la oferta, con su desglose, del mayor puntaje al menor.
- Un docente con un título en el campo específico de una asignatura tiene una línea `CUARTO_NIVEL` con `CAMPO_ESPECIFICO` y el peso de la versión.

## 2. Los pesos salen de la normativa (SC-002)

- Crea una versión con otros pesos y abre con ella otro periodo: el mismo docente tiene otro puntaje en ese periodo.

## 3. La ficha (US2)

- Con `coordinacion.demo`, abre un docente en «Docentes», elige 2026-I y despliega una asignatura.
- Elige 2026-II, sin abrir: el mensaje dice que lo abras.

## 4. Errores y permisos

- `?periodo=2027-I`: 404 `GENERACION_PERIODO_NO_ENCONTRADO`.
- `?periodo=2026 I`: 400 `COMUN_SOLICITUD_INVALIDA`.
- Un docente que no existe: 404 `GENERACION_DOCENTE_NO_ENCONTRADO`.
- Un usuario sin permiso: 403.
- Una ruta `/interno/…` con un token de usuario: 403; por el API Gateway: 404.
- Con el proceso `datos` detenido: 502 o 504, con el mensaje del catálogo, en menos de 10 s.

## 5. Punta a punta

- `deploy/probar_afinidad.sh` repite estos pasos y deja la base limpia.
