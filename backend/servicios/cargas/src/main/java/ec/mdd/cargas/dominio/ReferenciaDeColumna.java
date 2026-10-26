package ec.mdd.cargas.dominio;

import java.util.Map;
import java.util.Optional;

/**
 * A qué apunta una referencia ({@code FK}) del diccionario de datos (research §6). En una lista se
 * comprueba cada elemento o, si los elementos tienen partes, la parte que nombra el destino.
 *
 * @param tipo qué se busca
 * @param mismoArchivo si también vale una clave del propio archivo (el padre de una unidad)
 * @param campoDelArchivo la columna del propio archivo, si {@code mismoArchivo}; vacío si no
 * @param parte la parte de cada elemento que es la referencia; -1 si es el elemento entero
 */
public record ReferenciaDeColumna(
        TipoDeReferencia tipo, boolean mismoArchivo, String campoDelArchivo, int parte) {

    private static final String FK = "FK → ";
    private static final String MISMO_ARCHIVO = " (mismo archivo)";

    /** Cada destino del diccionario con lo que se busca. */
    private static final Map<String, TipoDeReferencia> DESTINOS =
            Map.of(
                    "01.codigo_unidad", TipoDeReferencia.UNIDAD,
                    "02.codigo_carrera", TipoDeReferencia.CARRERA,
                    "03.codigo_periodo", TipoDeReferencia.PERIODO,
                    "04.codigo_asignatura", TipoDeReferencia.ASIGNATURA,
                    "06.numero_identificacion", TipoDeReferencia.DOCENTE);

    /**
     * La referencia de una columna, si tiene {@code FK}.
     *
     * @param columna la columna
     * @param carga su carga
     * @param reglas las reglas de la columna, con las partes de sus elementos
     * @throws IllegalArgumentException si el destino no es uno conocido
     */
    static Optional<ReferenciaDeColumna> de(
            ColumnaDeCarga columna, Carga carga, ReglasDeColumna reglas) {
        if (!columna.clave().startsWith(FK)) {
            return Optional.empty();
        }
        String destino = columna.clave().substring(FK.length());
        if (destino.endsWith(MISMO_ARCHIVO)) {
            String campo = destino.substring(0, destino.length() - MISMO_ARCHIVO.length());
            String delArchivo = ClaveDeCarga.campoDe(campo, carga, columna);
            return Optional.of(
                    new ReferenciaDeColumna(
                            DESTINOS.get(String.format("%02d.%s", carga.numero(), delArchivo)),
                            true,
                            delArchivo,
                            -1));
        }
        TipoDeReferencia tipo = DESTINOS.get(destino);
        if (tipo == null) {
            throw new IllegalArgumentException(
                    "La columna "
                            + columna.campo()
                            + ": no sé dónde buscar la referencia «"
                            + destino
                            + "»");
        }
        int parte = -1;
        if (!reglas.partes().isEmpty()) {
            String campoDestino = destino.substring(destino.indexOf('.') + 1);
            for (int i = 0; i < reglas.partes().size(); i++) {
                if (ClaveDeCarga.campoDe(reglas.partes().get(i)).equals(campoDestino)) {
                    parte = i;
                }
            }
            if (parte < 0) {
                throw new IllegalArgumentException(
                        "La columna "
                                + columna.campo()
                                + ": ninguna parte de sus elementos es «"
                                + destino
                                + "»");
            }
        }
        return Optional.of(new ReferenciaDeColumna(tipo, false, "", parte));
    }
}
