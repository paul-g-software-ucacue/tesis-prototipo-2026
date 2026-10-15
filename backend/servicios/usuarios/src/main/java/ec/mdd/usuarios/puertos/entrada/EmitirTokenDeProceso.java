package ec.mdd.usuarios.puertos.entrada;

/**
 * Emite el token de un proceso del MDD para entregar eventos a otros procesos (D-49, ADR-018).
 *
 * <p>El token lleva el rol {@code PROCESO}, que solo sirve en las rutas {@code /interno/**}.
 */
public interface EmitirTokenDeProceso {

    /**
     * Emite el token si la clave es la del proceso.
     *
     * @param proceso nombre del proceso, por ejemplo {@code distributivo}
     * @param clave la clave del proceso; nunca se registra
     * @return el token y su vencimiento
     * @throws ec.mdd.usuarios.aplicacion.CredencialesInvalidas si la clave no es la del proceso o
     *     el proceso no tiene clave configurada, con la misma respuesta en los dos casos
     */
    TokenEmitido emitir(String proceso, String clave);
}
