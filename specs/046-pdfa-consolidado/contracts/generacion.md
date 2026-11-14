# Contrato: Generación (en el proceso, sin HTTP nuevo)

## API publicada nueva: `ContenidoCongelado`

```java
/**
 * El contenido de la versión congelada, para el documento firmable (F13-H02, D-101). Solo lee y
 * puede llamar a Datos (D-83): no se usa dentro de una transacción.
 * @throws DistributivoNoEncontrado sin distributivo
 * @throws VersionNoCongelada si la versión vigente no está CONGELADA
 */
ContenidoDeLaVersion contenido(String periodo);
```

`ContenidoDeLaVersion`:
- `version` y `hash` (`hashDelDistributivo`);
- `carreras` (`nombre`, `estado`, `paralelos`, `propuestos`, `horas` por componente);
- `totales` por componente;
- `docentes`, por apellidos y nombres:
  - `identificacion` y `nombre`;
  - `asignaturas` (`codigo`, `nombre`, `paralelo`, `carrera` y `horas`);
  - `actividades` (`nombre`, `componente` y `horas`);
  - `horas` por componente.

Sin datos de contacto ni de contrato (principio IX).
