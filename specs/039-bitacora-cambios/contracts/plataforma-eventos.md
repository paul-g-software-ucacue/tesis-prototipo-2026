# Contrato: `:plataforma:eventos` y los procesos (ADR-024)

No es una API HTTP: es lo que la plataforma garantiza a los servicios que publican.

## `ReceptorLocal`

```java
/** Un receptor de eventos del mismo proceso, que se llama sin HTTP (ADR-024). */
public interface ReceptorLocal {
    /** El servicio destino que atiende, como en {@code mdd.eventos.destinos}. */
    String destino();

    /** Registra el evento; una excepción deja la publicación pendiente. */
    void recibir(EventoEntreProcesos evento);
}
```

## `ClienteDeEventos.entregar`

1. Si hay un `ReceptorLocal` para `evento.destino()`, lo llama. Cualquier excepción se convierte en `EntregaFallida`, sin el contenido en el mensaje.
2. Si no, entrega por HTTP como hoy (ADR-018), con su URL de `mdd.eventos.destinos`.
3. Si no hay ninguno de los dos: `EntregaFallida` («sin destino configurado»), como hoy.

El oyente, el reenvío y el monitor de publicaciones atascadas no cambian: una entrega local fallida se reintenta igual que una HTTP.

## Configuración por proceso

| Proceso | `mdd.eventos.proceso` | Destinos HTTP | Receptores locales | Clave |
|---|---|---|---|---|
| `identidad` | `identidad` (nuevo) | ninguno | `usuarios` | No necesita: no entrega por HTTP. |
| `datos` | `datos` | `roles` y **`usuarios` (nuevo)** | ninguno | `MDD_CLAVE_PROCESO_DATOS` (ya existe) |
| `distributivo` | `distributivo` | `usuarios` | ninguno | `MDD_CLAVE_PROCESO_DISTRIBUTIVO` (ya existe) |

`identidad` arranca sin `mdd.eventos.clave` ni `mdd.eventos.token-proceso`: `ProveedorDeTokenDeProceso` ya pide el token solo cuando se usa, y una entrega local no lo usa.
