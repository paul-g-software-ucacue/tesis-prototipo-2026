# Quickstart: Ver el panel de inconsistencias globales

## Automático

```bash
deploy/probar_panel.sh
```

Con el perfil levantado y la base vacía, sobre los datos de prueba:

1. Las 7 comprobaciones aparecen y coinciden con un conteo aparte (SC-001, SC-003).
2. Cada una desaparece al corregirla.
3. Una blanda se justifica, y una dura no.
4. Una excepción de RN-19 sale como justificada.
5. Otra meta u otro margen cambian el resultado.
6. Vicerrectorado lee, Consolidación justifica y un coordinador no entra.
7. El panel abre en menos de 5 s, y justificar tarda menos de 3 s.
8. Con `datos` detenido, responde el error del catálogo en menos de 10 s.

## A mano, en la demo

Entra con `consolidacion.demo`, abre **Panel de inconsistencias** y elige el periodo 003.

- **Paralelos sin propuesta:** `A_NOCTURNA` y `B` de `SOC`.
- **Gestores:** fuera de rango, porque la demo no tiene los 8 que exige Tipo 2.
- **La excepción de `POO-23`:** como justificada.
- **Justificar:** una blanda, con un texto de 10 a 500 caracteres.

Después entra con `vicerrectorado.demo`: ve lo mismo, sin **Justificar**.
