# Quickstart: El consolidado no cambia durante la revisión

El detalle está en [contracts/generacion.md](contracts/generacion.md), [contracts/validacionocs.md](contracts/validacionocs.md), [contracts/spa.md](contracts/spa.md) y [contracts/demostracion.md](contracts/demostracion.md).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:generacion:test :servicios:validacionocs:test :apps:distributivo:test
./gradlew :servicios:validacionocs:integrationTest
./gradlew :servicios:usuarios:test --rerun     # BitacoraSinBorradoContratoTest
cd ../frontend && npm test -- Consolidado Inconsistencias && npx playwright test consolidado
```

## 2. El script

```bash
deploy/probar_consolidado_en_revision.sh
```

Esperado: los 6 pasos en OK.

## 3. En la pantalla (comprobación del autor, perfil local)

Después del script no queda nada: para ver la pantalla, repetir a mano el camino de F12-H05 (T020) o correr el script con la limpieza desactivada. Con el distributivo en «Revisión del Vicerrectorado», como `consolidacion.demo`:
1. **Consolidado del periodo:**
   - no hay «Volver a consolidar» ni «Adjuntar el informe»;
   - está el aviso «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.»;
   - el informe se descarga.
2. **Panel de inconsistencias:** no hay «Justificar» y está el mismo aviso.
3. **Antes de enviar a revisión** (en Consolidación), las dos pantallas están como siempre.

## 4. En el equipo de demostración

2026-I está en Consolidación: comprobar que «Volver a consolidar», «Adjuntar el informe» y «Justificar» siguen, y que no aparece el aviso.
