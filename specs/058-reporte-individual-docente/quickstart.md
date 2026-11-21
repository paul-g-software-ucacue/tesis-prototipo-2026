# Quickstart: Reporte individual del docente en PDF y CSV

F20-H01 (#268). Cómo comprobar la historia a mano en el perfil consolidado. Los detalles de las rutas están en [contracts/reporting.md](contracts/reporting.md), y los pasos automáticos, en [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- El perfil consolidado levantado, con `MDD_CLAVE_PROCESO_SALIDA` definida (`docker compose up`).
- Los datos de prueba cargados y el distributivo de 2026-I publicado:

```bash
MDD_CONSERVAR=PUBLICADO deploy/probar_documento_firmable.sh
```

## 1. El reporte en pantalla (US1)

1. Entrar como `rectorado.demo` y abrir «Reportes».
2. **Pasa si:**
   - el periodo propuesto es 2026-I, con «Versión 1, publicada el … por secretaria.demo»;
   - al buscar a `docente.demo` por su nombre aparece en la lista;
   - su reporte muestra sus asignaturas, sus actividades y las horas por componente con el total;
   - el hash del encabezado es el de la constancia de la publicación en «Resolución del OCS».
3. Comparar el total con su fila en «Consolidado del periodo». **Pasa si** es el mismo.

## 2. Las descargas (US2)

1. Pulsar «Descargar PDF» y «Descargar CSV».
2. **El PDF pasa si:**
   - el lector no ofrece campos para llenar;
   - cada página tiene en el pie el periodo, la versión y el hash.
3. **Opcional:** verificar el PDF/A con veraPDF:

   ```bash
   verapdf --flavour 2b reporte-individual-2026-I-*.pdf
   ```

   **Pasa si** el resultado es conforme.
4. Abrir el CSV en una hoja de cálculo con la configuración de Ecuador. **Pasa si:**
   - abre en columnas, sin asistente;
   - las tildes se leen bien;
   - importando «Identificación» como texto, como dice el modal que aparece al descargarlo, la cédula conserva el cero inicial;
   - cada fila y cada total coinciden con el PDF.
5. Como `admin.demo`, abrir «Bitácora» y filtrar por la identificación del docente. **Pasa si** hay dos entradas `REPORTE_INDIVIDUAL_DESCARGADO`, una con `PDF` y otra con `CSV`, con `rectorado.demo` como usuario.

## 3. Sin permiso

1. Entrar como `coordinacion.demo`. **Pasa si** «Reportes» no está en el menú.
2. Abrir `/reportes` a mano. **Pasa si** se ve la pantalla de acceso no autorizado.

## 4. La reforma

1. Como `vicerrectorado.demo`, iniciar una reforma.
2. Como `coordinacion.demo`, corregir las horas de un paralelo de `docente.demo` en «Oferta y propuesta de docentes».
3. Como `rectorado.demo`, abrir «Reportes» otra vez. **Pasa si:**
   - dice «Hay una reforma en curso; el reporte sale de esta versión»;
   - el reporte de `docente.demo` no cambió.

## 5. Las pruebas

```bash
cd backend
./gradlew :servicios:reporting:check :servicios:generacion:check :servicios:validacionocs:check :servicios:usuarios:check
cd ../frontend
npm test -- reportes
npx playwright test e2e/reportes.spec.ts
```
