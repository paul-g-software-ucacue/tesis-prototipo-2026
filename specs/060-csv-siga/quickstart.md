# Quickstart: Generar el CSV para SIGA con el layout configurable

F19-H01 (#266). Cómo comprobar la historia a mano en el perfil consolidado. Los detalles están en:
- [contracts/reporting.md](contracts/reporting.md): las rutas, el catálogo y el formato;
- [contracts/spa.md](contracts/spa.md): la pantalla;
- [contracts/demostracion.md](contracts/demostracion.md): los pasos automáticos R9 y R10;
- [data-model.md](data-model.md) y [research.md](research.md) §4 a §6: el layout y sus reglas.

## Requisitos

- El perfil consolidado levantado (`docker compose up`), con una base que congele **después** de esta historia, para que el contenido tenga el tipo de identificación (research §2).
- Los datos de prueba cargados y el distributivo de 2026-I publicado:

```bash
MDD_CONSERVAR=PUBLICADO deploy/probar_documento_firmable.sh
```

## 1. El CSV con el layout por defecto (US1 y US2)

1. Entrar como `secretaria.demo` y abrir «Exportación a SIGA».
2. **Pasa si:**
   - el periodo propuesto es 2026-I, con la versión 1;
   - dice «Se generará con el layout por defecto.» (o el layout que dejó el script);
   - el menú de `rectorado.demo` no tiene la entrada.
3. Pulsar «Generar CSV para SIGA» y abrir `siga-2026-I-v1.csv` en un editor de texto, **no** en una hoja de cálculo. **Pasa si:**
   - la primera fila es el encabezado, con las columnas de FR-004 en su orden;
   - el archivo no tiene BOM (`head -c3 siga-2026-I-v1.csv | xxd` no da `efbbbf`) y usa CRLF;
   - cada asignatura y paralelo y cada actividad de un docente está en una fila;
   - en la fila de una actividad, la asignatura y el paralelo dicen `NA`;
   - los nombres están en mayúsculas y conservan las tildes y las eñes;
   - las cédulas tienen 10 dígitos, incluso las que empiezan con 0;
   - las horas tienen punto y dos decimales, y la fecha es `aaaa-mm-dd`.
4. Comparar los totales con el reporte general:

   ```bash
   python3 -I - siga-2026-I-v1.csv <<'EOF'
   import csv, sys
   from collections import defaultdict
   from decimal import Decimal
   suma = defaultdict(Decimal)
   with open(sys.argv[1], newline='', encoding='utf-8') as f:
       for fila in csv.DictReader(f):
           suma[fila['COMPONENTE']] += Decimal(fila['HORAS_SEMANALES'])
   print(dict(suma))
   EOF
   ```

   **Pasa si** cada total es igual a «Total institucional» del reporte general de 2026-I, en «Reportes».
5. Generar otra vez y comparar con `cmp`. **Pasa si** los dos archivos son idénticos.
6. Como `vicerrectorado.demo`, abrir «Bitácora». **Pasa si** hay dos entradas `CSV_SIGA_GENERADO` de `secretaria.demo` con el periodo y la versión, sin nombres ni horas.

## 2. Otro layout (US3)

1. En «Layout de exportación», pulsar «Descargar plantilla». **Pasa si** está comentada en español y explica cada campo.
2. Editarla:
   - `NUMERO_IDENTIFICACION` → `CEDULA_DOCENTE`;
   - invertir `CODIGO_ASIGNATURA` y `PARALELO`;
   - quitar `FECHA_PUBLICACION`;
   - agregar a `componente` la traducción `CLASE: "01"` … `GESTION: "05"`.
3. Cargarla. **Pasa si:**
   - dice «Se cargó la versión N del layout de exportación…»;
   - la tabla muestra el layout nuevo;
   - el anterior está en «Versiones anteriores».
4. Generar el CSV. **Pasa si:**
   - el encabezado tiene las columnas nuevas, en su orden, sin la fecha;
   - el componente sale `01`…`05`;
   - el script del paso 1.4, con `CEDULA_DOCENTE`, da los mismos totales.
5. Cargar un archivo con `campo: horas_semanales`, dos columnas `PERIODO` y una traducción de `componente` sin `GESTION`. **Pasa si:**
   - dice «El archivo tiene 3 problemas. El layout vigente no cambió.»;
   - cada problema nombra su columna y su regla;
   - el layout vigente es el del paso 3.
6. Cargar un archivo con un ancla (`&a`) y otro de más de 256 KB. **Pasa si** responden «Archivo no válido» y «Archivo demasiado grande», sin cambiar el vigente.
7. Volver a cargar la plantilla sin cambios, para dejar el layout por defecto.

## 3. Acceso y casos límite

- **Permisos:** como `admin.demo` y como `rectorado.demo`, entrar a `/exportacion-siga`. **Pasa si** ven «sin acceso»; por la API, los dos reciben 403 en las cuatro operaciones.
- **Durante la reforma:** con `MDD_CONSERVAR=REFORMA`, generar el CSV. **Pasa si** dice la versión 1 y es idéntico al del paso 1.
- **Sin publicación:** con un periodo sin publicar, `curl` de `/api/reporting/periodos/{periodo}/csv-siga`. **Pasa si** responde 409 `REPORTING_SIN_PUBLICACION`.
- **Fórmulas:** con un docente de prueba cuyo nombre empiece con `=`, como en las pruebas del reporte individual, la celda sale con `'` al inicio.

## 4. Las pruebas automáticas

```bash
cd backend && ./gradlew :servicios:reporting:check :servicios:generacion:check :servicios:docentes:check :apps:salida:integrationTest
cd frontend && npm run test -- siga && npx playwright test siga
deploy/probar_documento_firmable.sh   # R1, R4, R5, R9 y R10
```

La evidencia (`evidencia.md`) anota el tiempo de generación con 300 docentes (SC-008) y el resultado de axe-core (SC-010).
