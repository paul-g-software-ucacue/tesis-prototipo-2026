# Quickstart: comprobar F01-H01

Guía para validar la historia de punta a punta. Los detalles están en [research.md](research.md), el modelo en [data-model.md](data-model.md) y el contrato en [contracts/cargas.md](contracts/cargas.md).

## Pruebas automáticas

```bash
cd backend && ./gradlew check integrationTest
cd frontend && npm run lint && npm run formato:verificar && npm run tipos \
  && npm test -- --run --coverage && npm run test:e2e && npm run verificar:ruptura
deploy/probar_plantillas.sh      # nuevo; no crea nada
```

## A mano, en la SPA

1. Entrar como `consolidacion.demo` y abrir «Plantillas de carga»: las 10 cargas en su orden, con «Depende de», «Obligatoria» y el número de columnas (115 en total).
2. Pulsar «Plantilla» en «Docentes contratados»: se descarga `06_docentes.csv`. Abrirlo en LibreOffice Calc: 26 encabezados, cada uno en su columna, con las tildes bien (SC-003).
3. Desplegar «Estructura orgánica»: su diccionario, con `codigo_gestion_autorizada` «Sí si tipo_unidad = GESTION» y las 21 gestiones en sus valores. Pulsar «Diccionario»: `01_estructura_organica_diccionario.csv`, con las 7 columnas y las convenciones.
4. Entrar como `datos.demo`: ve lo mismo. Entrar como `gestion.demo` o `docente.demo`: no ve «Plantillas de carga» en el menú.

SC-001: los pasos 2 y 3, desde que se abre la pantalla, en menos de 30 segundos cada uno.
