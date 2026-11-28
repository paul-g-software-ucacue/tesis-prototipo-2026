# Quickstart: Validar antes de exportar y registrar la exportación

F19-H02 (#267). Cómo comprobar la historia de punta a punta en el perfil local. Los detalles de la API están en [contracts/reporting.md](contracts/reporting.md), los de la pantalla en [contracts/spa.md](contracts/spa.md) y los del script en [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- **El perfil:** construido desde la rama, con `docker compose build datos distributivo salida frontend` y `docker compose up -d --wait`. `datos` no cambia, pero sale de la misma versión.
- **El distributivo:** 2026-I congelado **con esta versión**, para que su contenido tenga las inconsistencias registradas (research §2). El script lo recrea; una base anterior responde «no se registraron sus inconsistencias».
- **El script:** `deploy/probar_documento_firmable.sh` con `MDD_CONSERVAR=PUBLICADO`, que deja la versión 1 de 2026-I publicada.
- **Las herramientas:** `sha256sum` (o `certutil` en Windows).

## 1. Exportar y verificar (US2)

1. Entrar como `secretaria.demo` y abrir «Exportación a SIGA». **Pasa si:**
   - la sección «CSV para SIGA» muestra, antes del botón, el aviso de la hoja de cálculo y de la suma de verificación (FR-019a);
   - «Exportaciones del periodo» dice que todavía no se exportó, o lista las de corridas anteriores.
2. Con 2026-I, pulsar «Generar CSV para SIGA». **Pasa si:**
   - se descarga `siga-2026-I-v1.csv`;
   - aparecen «Suma de verificación (SHA-256):», el valor y la línea con `certutil` y `sha256sum`;
   - la exportación nueva está arriba de la lista, con `secretaria.demo`, la hora, la versión 1, el layout, el archivo y la misma suma.
3. En una terminal, `sha256sum siga-2026-I-v1.csv`. **Pasa si** es igual a la suma de la pantalla (SC-002).
4. Pulsar «Copiar la suma de verificación» y pegarla. **Pasa si** es la misma.
5. Generar otra vez. **Pasa si** hay dos exportaciones con la misma suma (SC-005).
6. Como `vicerrectorado.demo`, abrir «Bitácora». **Pasa si:**
   - hay una entrada `CSV_SIGA_GENERADO` por exportación, con el periodo, la versión, el layout, la suma y el `id`;
   - ninguna tiene nombres ni horas (SC-003).
7. Por la API, sin la pantalla:

   ```bash
   curl -sk -D - -o siga.csv -H "Authorization: Bearer $TOKEN" \
     https://mdd.localhost/api/reporting/periodos/2026-I/csv-siga | grep -i x-suma-verificacion
   sha256sum siga.csv
   ```

   **Pasa si** el encabezado y `sha256sum` coinciden.

## 2. La validación (US1)

1. **Sin publicación:** con un periodo en EN_OCS (R1 del script), pedir el CSV por la API. **Pasa si:**
   - responde 409 `REPORTING_SIN_PUBLICACION`;
   - no hay exportación nueva;
   - no hay `EXPORTACION_SIGA_RECHAZADA`.
2. **Firmas o inconsistencias:** el flujo normal no deja publicar una versión así, así que esta parte se comprueba con las pruebas automáticas (§4), no en el perfil:
   - `CondicionesDeExportacionTest` cubre cada motivo y varios a la vez;
   - `GenerarCsvSigaServicioTest` comprueba que no hay archivo ni registro, que queda la entrada del rechazo y que sigue el 409 aunque la entrada no se guarde;
   - `SigaContratoTest` comprueba el 409 `REPORTING_VERSION_NO_EXPORTABLE`.
3. **Durante una reforma** (R5 del script): el CSV tiene la misma suma que antes de la reforma y queda una exportación más de la versión 1.

## 3. Acceso e inmutabilidad

- **Acceso:** `rectorado.demo`, `coordinacion.demo` y `admin.demo` no ven «Exportación a SIGA». Por la API, reciben 403 en `csv-siga` y en `exportaciones-siga` (SC-008).
- **Inmutabilidad:** en la base, `update reporting.exportacion_siga set filas = 0` y `delete from reporting.exportacion_siga` fallan con `insufficient_privilege` (SC-006, R11).
- **Accesibilidad:** la pantalla pasa axe-core con la suma y la lista (SC-009). El recorrido con lector de pantalla queda para el autor.

## 4. Las pruebas automáticas

```bash
cd backend
./gradlew :servicios:reporting:check :servicios:firma:check :servicios:generacion:check \
          :apps:salida:integrationTest :apps:distributivo:integrationTest
cd ../frontend && npx vitest run src/pantallas/siga && npx playwright test e2e/siga.spec.ts
deploy/probar_documento_firmable.sh
```

**Pasa si** todo está en verde, y el script pasa R1, R4, R5, R9, R10 y R11.

**SC-007:** `GenerarCsvSigaServicioTest` mide los 300 docentes (menos de 10 s); la evidencia anota el tiempo real por el API Gateway.
