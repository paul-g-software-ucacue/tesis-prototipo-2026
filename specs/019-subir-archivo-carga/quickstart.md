# Quickstart: comprobar F01-H02

Guía para validar la historia de punta a punta. Los detalles están en [research.md](research.md), el modelo en [data-model.md](data-model.md) y el contrato en [contracts/cargas.md](contracts/cargas.md).

## Pruebas automáticas

```bash
cd backend && ./gradlew check integrationTest
cd frontend && npm run lint && npm run formato:verificar && npm run tipos \
  && npm test -- --run --coverage && npm run test:e2e && npm run verificar:ruptura
deploy/probar_subida.sh      # nuevo; no crea nada
```

## A mano, en la SPA

1. Entrar como `consolidacion.demo` (con D-71 tiene «Cargas: escritura») y abrir «Plantillas de carga»: cada carga tiene «Subir archivo».
2. En «Docentes contratados», subir `06_docentes.csv` de los datos de prueba: «06_docentes.csv: 47 registros leídos» (SC-001: en menos de 5 s desde que se elige).
3. Abrir el mismo archivo en LibreOffice Calc, guardarlo como XLSX y subirlo: el mismo número.
4. En «Carreras», subir un CSV sin la columna `codigo_carrera`: «Encabezados distintos de la plantilla», con `codigo_carrera: Falta en el archivo.`
5. Subir un PDF: «Formato no admitido». Guardar un CSV con tildes en Windows-1252 y subirlo: «Codificación no admitida».
6. Entrar como `coordinacion.demo`: ve las plantillas, pero no «Subir archivo».
