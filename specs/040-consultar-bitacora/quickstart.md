# Quickstart: Consultar la bitácora

Guía para comprobar la historia de punta a punta. El detalle de cada operación está en [contracts/usuarios.md](contracts/usuarios.md), el de la pantalla en [contracts/spa.md](contracts/spa.md) y el del script en [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- El perfil consolidado levantado (`docker compose up -d --wait`, como el workflow `perfil-consolidado`), con los usuarios de la demo y sus contraseñas de la CI.
- Datos de prueba cargados: un periodo abierto con distributivo y una actividad de 4 horas, como deja el paso 1 de `probar_bitacora.sh` o, en el equipo de demostración, `deploy/equipo/poblar_demo.sh`.

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:usuarios:test :servicios:generacion:test :servicios:academico:test :servicios:roles:test
./gradlew :servicios:usuarios:integrationTest :servicios:academico:integrationTest
cd ../frontend && npm test -- bitacora && npx playwright test bitacora
```

Esperado: todo en verde; la prueba de integración de la consulta mide la primera página con 10 000 entradas por debajo de 2 s (SC-003).

## 2. El script de la historia

```bash
deploy/probar_consulta_bitacora.sh
```

Esperado: los 9 pasos en OK.

## 3. En la pantalla (comprobación del autor)

1. Entrar como `vicerrectorado.demo`. En el menú aparece «Bitácora».
2. Con un cronómetro desde que se abre la bitácora (SC-004, menos de 1 minuto), filtrar por usuario `coordinacion.demo` y por hoy, en «Desde» y «Hasta». Solo aparecen entradas de ese usuario y de hoy, en hora de Ecuador (US1-1).
3. Expandir la entrada de 4 a 6 horas de F17-H01: se ven `horas` 4 y 6. En una entrada de creación se lee «Sin valor anterior (se creó)».
4. Elegir el servicio «Generación» y la acción `ACTIVIDAD_GUARDADA`; después escribir el código de la actividad en «Identificador» (US1-3, US1-4).
5. Poner «Hasta» antes que «Desde»: el campo se marca con su motivo, sin enviar.
6. Como `coordinacion.demo`, guardar la actividad con las mismas horas. Volver como Vicerrector: no hay entrada nueva (US3-1).
7. Entrar como `coordinacion.demo`: no aparece «Bitácora», y `/bitacora` muestra el aviso de acceso no autorizado (US2).

## 4. En el equipo de demostración

Después del despliegue, repetir el apartado 3 con los datos de `poblar_demo.sh` y anotar el resultado en `evidencia.md`, con la fecha.
