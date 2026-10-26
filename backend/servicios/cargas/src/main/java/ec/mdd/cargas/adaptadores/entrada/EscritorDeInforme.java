package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.dominio.ProblemaDeFila;
import java.util.ArrayList;
import java.util.List;

/**
 * Escribe el informe de validación de la carga en CSV (RF-09, FR-007, research §9): las columnas
 * {@code fila,campo,valor,motivo,tipo,sugerencia}, con todos los problemas, sobre {@link
 * EscritorCsv}, que pone el BOM, sigue RFC 4180 y antepone un apóstrofo a toda celda que empiece
 * con {@code =}, {@code +}, {@code -}, {@code @}, tabulación o retorno de carro (RNF-SEG-04). Aquí
 * esa neutralización importa: los valores vienen del archivo subido.
 */
final class EscritorDeInforme {

    /** Las columnas del informe, en su orden. */
    static final List<String> ENCABEZADOS =
            List.of("fila", "campo", "valor", "motivo", "tipo", "sugerencia");

    private EscritorDeInforme() {}

    /**
     * El informe en CSV.
     *
     * @param informe el informe, con todos sus problemas
     * @return el texto, que se envía en UTF-8
     */
    static String escribir(InformeValidacionCarga informe) {
        List<List<String>> filas = new ArrayList<>();
        filas.add(ENCABEZADOS);
        for (ProblemaDeFila problema : informe.problemas()) {
            filas.add(
                    List.of(
                            String.valueOf(problema.fila()),
                            problema.campo(),
                            problema.valor(),
                            problema.motivo(),
                            problema.tipo().name(),
                            problema.sugerencia().orElse("")));
        }
        return EscritorCsv.escribir(filas);
    }
}
