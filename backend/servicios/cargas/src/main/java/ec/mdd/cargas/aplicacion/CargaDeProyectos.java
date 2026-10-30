package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.dominio.Obligatoriedad;

/** La carga que admite la confirmación «sin proyectos», según el diccionario de datos (F01-H05). */
final class CargaDeProyectos {

    private CargaDeProyectos() {}

    /** Su código, el de la carga con obligatoriedad «obligatoria salvo sin proyectos». */
    static String codigo(DiccionarioDeDatos diccionario) {
        return diccionario.cargas().stream()
                .filter(
                        carga ->
                                carga.obligatoriedad()
                                        == Obligatoriedad.OBLIGATORIA_SALVO_SIN_PROYECTOS)
                .map(Carga::codigo)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Ninguna carga del diccionario admite «sin proyectos»"));
    }
}
