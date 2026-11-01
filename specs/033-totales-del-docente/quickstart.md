# Quickstart: comprobar los totales del docente

Requisitos: el perfil consolidado levantado, con las cargas de prueba confirmadas, 2026-I abierto, el distributivo creado y dos coordinadores con carreras distintas.

## 1. Al guardar (US1)

- Con el coordinador de una carrera, propón a un docente y asígnale una actividad: el panel se actualiza en cada pantalla.
- Con el coordinador de otra carrera, asigna al mismo docente: los totales suman las dos.
- Quita una asignación: deja de contar.

## 2. La proporción (US2)

- Con 8 h de clase y 4 h de otras actividades de docencia: «Clase 67 % · Otras 33 % (meta 60/40)».
- Agrega horas de gestión: la proporción no cambia.
- Sin clase ni otras actividades: dice que todavía no hay proporción.

## 3. Antes de guardar (US3)

- En el formulario, elige a un docente: ves sus horas contratadas, asignadas y disponibles.
- En la sugerencia, despliega a un candidato: ves lo mismo.
- Con más horas de las contratadas: dice por cuántas se pasó.

## 4. La lista (US4)

- Un docente con actividades en dos carreras: la columna muestra los totales de las dos, y la lista, solo sus actividades de esta carrera.
- El Vicerrector ve los mismos totales, sin acciones.

## 5. Lo que no se puede mostrar (US5)

- Con `datos` detenido, ves un mensaje claro en menos de 10 s.

## 6. Punta a punta

- `deploy/probar_totales.sh` repite estos pasos y deja la base limpia.
