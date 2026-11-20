package ec.mdd.validacionocs.puertos.entrada;

/**
 * La revisión de los plazos de cada etapa (F12-H03, RF-34, RN-23, D-114): avisa una vez por llegada
 * cuando vence el plazo de la etapa actual de un distributivo en curso. No bloquea nada.
 */
@FunctionalInterface
public interface RevisarPlazos {

    /**
     * Revisa todos los procesos en curso. Un fallo con uno no detiene a los demás.
     *
     * @return cuántos vencimientos nuevos registró
     */
    int revisar();
}
