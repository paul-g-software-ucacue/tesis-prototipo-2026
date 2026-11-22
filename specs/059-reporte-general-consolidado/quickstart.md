# Quickstart: Reporte general consolidado en PDF y CSV

F20-H02 (#269). Cómo comprobar la historia a mano en el perfil consolidado. Los detalles están en:
- [contracts/reporting.md](contracts/reporting.md), las rutas y el formato de los archivos;
- [contracts/spa.md](contracts/spa.md), la pantalla;
- [contracts/demostracion.md](contracts/demostracion.md), los pasos automáticos.

## Requisitos

- El perfil consolidado levantado (`docker compose up`), como en F20-H01.
- Los datos de prueba cargados y el distributivo de 2026-I publicado:

```bash
MDD_CONSERVAR=PUBLICADO deploy/probar_documento_firmable.sh
```

## 1. El reporte general en pantalla (US1)

1. Entrar como `rectorado.demo` y abrir «Reportes».
2. **Pasa si:**
   - el periodo propuesto es 2026-I, con la versión 1;
   - «Tipo de reporte» propone «General»;
   - la tabla muestra a todos los docentes del distributivo, por nombre, con sus cinco componentes y su total;
   - «N docentes» coincide con el número de filas;
   - la última fila es «Total institucional».
3. Elegir «Por docente», buscar a `docente.demo` y comparar su reporte con su fila del general. **Pasa si** las horas por componente y el total son los mismos.
4. **Opcional:** comparar los totales institucionales con los del PDF/A del consolidado firmado. Deberían coincidir; si no, anotarlo en la evidencia, porque los dos salen de cálculos distintos (Riesgos del plan).

## 2. Las descargas (US2)

1. Con «General» elegido, pulsar «Descargar PDF» y «Descargar CSV».
2. **El PDF pasa si:**
   - está en horizontal;
   - el lector no ofrece campos para llenar;
   - el encabezado de la tabla se repite en cada página;
   - «Total institucional» aparece una sola vez, al final;
   - cada página tiene en el pie el periodo, la versión, el hash y el número de página.
3. **Opcional:** verificar el PDF/A con veraPDF:

   ```bash
   verapdf --flavour 2b reporte-general-2026-I.pdf
   ```

   **Pasa si** el resultado es conforme.
4. Abrir el CSV en una hoja de cálculo con la configuración de Ecuador. **Pasa si:**
   - abre en columnas, sin asistente, y las tildes se leen bien;
   - hay una fila `DOCENTE` por docente y una fila `TOTAL` al final;
   - la suma de cada columna de horas de las filas `DOCENTE` es igual a la fila `TOTAL`;
   - cada fila coincide con el PDF.
5. Como `admin.demo`, abrir «Bitácora» y filtrar por el servicio `reporting`. **Pasa si** hay dos entradas `REPORTE_GENERAL_DESCARGADO`, una con `PDF` y otra con `CSV`, sin identificación, con `rectorado.demo` como usuario.

## 3. Sin permiso

1. Entrar como `coordinacion.demo`. **Pasa si** «Reportes» no está en el menú.
2. Con su token, pedir `GET /api/reporting/periodos/2026-I/reporte-general.csv`. **Pasa si** responde 403.

## 4. La reforma

1. Como `vicerrectorado.demo`, iniciar una reforma.
2. Como `rectorado.demo`, abrir «Reportes» con «General». **Pasa si:**
   - dice «Hay una reforma en curso; el reporte sale de esta versión»;
   - los totales no cambiaron.

## 5. Las pruebas

```bash
cd backend
./gradlew :servicios:reporting:check :servicios:generacion:check
./gradlew :apps:salida:integrationTest :servicios:generacion:integrationTest
cd ../frontend
npm test -- reportes
npx playwright test e2e/reportes.spec.ts
```

**Pasan si:**
- todo está en verde;
- `GeneradorDeReportePdfaTest` reporta el tiempo del caso de 300 docentes por debajo de 10 s (SC-007).
