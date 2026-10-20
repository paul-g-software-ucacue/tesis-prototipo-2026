# Quickstart: Ajustar pesos y mínimo de afinidad y la severidad de las reglas (F04-H03)

## 1. Pruebas

```bash
cd backend && ./gradlew :servicios:normativa:check :servicios:normativa:integrationTest
cd frontend && npm run lint && npm run tipos && npm test -- --run --coverage && npm run test:e2e
```

Esperado: las pruebas de research §9 en verde, con el 60 % de líneas.

## 2. En local, con el perfil

```bash
sg docker -c 'docker compose up -d --build --wait'
deploy/probar_normativa.sh
```

Esperado:
- `normativa` en V4;
- el paso de afinidad en `OK`: la versión 1 tiene cuarto nivel 3 y 2, tercer nivel 2 y 1, experiencia 2, curso 1, tope 3 y mínimo 2;
- el paso de severidad en `OK`: la versión de prueba tiene RN-05 dura en el periodo de prueba y la 1 la conserva blanda;
- al terminar, la base vuelve a tener solo lo que tenía.

## 3. Revisión manual (SC-001)

1. Entrar como `vicerrectorado.demo` y abrir «Normativa»: la sección «Afinidad» muestra los pesos y el mínimo.
2. Con una versión sin periodo, «Editar»: cambiar un peso y la severidad de RN-05 a «Dura», y «Guardar». Menos de 2 minutos.
3. Abrir la versión de origen: conserva los valores de antes.

## 4. En el equipo de demostración, después de fusionar

- Esperar «Sano en N s» con `normativa` en V4.
- En la base: cada versión con 16 reglas y su `VERSION_EDITADA` de `sistema`.
- Repetir la revisión manual con `vicerrectorado.demo`.
