# Contrato: la ruta interna de Generación, ampliada

F20-H02 (#269). Se amplía `contratos/generacion/openapi.yaml`. Es una ruta entre procesos, con el token de proceso (ADR-023). Solo la usa Reporting.

## `consultarCargasCongeladas` (cambia)

`GET /interno/generacion/periodos/{periodo}/versiones/{numero}/cargas`

Lo que no cambia:
- la ruta, los parámetros y las respuestas de error: 400, 401, 403, 404 si la versión no existe o no está congelada, y 500;
- el caso tardío de D-115: si el contenido falta, se calcula, se guarda y se devuelve.

**200:** `CargasCongeladas`, `{hash, docentes}`. Lo que cambia es cada elemento de `docentes`.

### `DocenteCongelado` (amplía)

```yaml
DocenteCongelado:
  type: object
  additionalProperties: false
  required: [identificacion, nombre, horas]     # horas es nueva y obligatoria
  properties:
    identificacion:
      type: string
    nombre:
      type: string
    dedicacion:                                 # nueva, opcional
      description: Ausente si el docente ya no estaba activo al congelar.
      allOf:
        - $ref: '#/components/schemas/Dedicacion'
    horas:                                      # nueva
      $ref: '#/components/schemas/HorasPorComponente'
```

- **De dónde salen:** `dedicacion` y `horas` son las mismas que devuelve `consultarCargaCongelada` para ese docente (`docente.dedicacion` y `horas`), leídas de la misma fila de `carga_congelada`.
- **El orden:** por nombre y, a igual nombre, por identificación, como hoy.

## Compatibilidad

- **El cliente.** El único cliente es `ContenidoDesdeGeneracion` de Reporting, que se actualiza en el mismo cambio y lee la respuesta con su esquema (principio XIV).
- **El despliegue.** `distributivo` y `salida` salen de la misma versión del repositorio, en el perfil consolidado y en el equipo de demostración. Una `salida` nueva con un `distributivo` viejo fallaría por la falta de `horas`, y respondería 502 sin entregar nada. Ese orden no se da en los despliegues del proyecto (Riesgos del plan).

## Lo que comprueban las pruebas

- **`InternoGeneracionContratoTest`:** la respuesta con `dedicacion` y `horas`, y un docente sin dedicación.
- **`ContenidoCongeladoIntegrationTest`:** los docentes de una versión guardada traen las horas y la dedicación de su carga, en PostgreSQL real.
- **`ClientesDeReportingTest`:** Reporting lee los campos nuevos, y una respuesta sin `horas` es un 502.
