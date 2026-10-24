package ec.mdd.academico.dominio;

/** Las reglas de texto que comparten las carreras y las asignaturas. */
final class Textos {

    private Textos() {}

    /**
     * El nombre sin espacios en los extremos; no puede quedar vacío ni pasar del largo.
     *
     * @throws DatoInvalido en {@code nombre}
     */
    static String nombre(String nombre, int maximo) {
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty()) {
            throw new DatoInvalido("nombre", "El nombre no puede estar en blanco");
        }
        if (limpio.length() > maximo) {
            throw new DatoInvalido(
                    "nombre", "El nombre no puede tener más de " + maximo + " caracteres");
        }
        return limpio;
    }
}
