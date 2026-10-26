package ec.mdd.cargas.dominio;

import ec.mdd.cargas.adaptadores.salida.DatosDePrueba;
import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.adaptadores.salida.LectorPorFormato;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El diccionario de datos real y los 10 archivos de prueba ficticios, para probar las reglas de las
 * filas sin dobles (principio VIII).
 */
final class DiccionarioReal {

    /** El diccionario de {@code diccionario-de-datos.yaml}. */
    static final DiccionarioDeDatos DICCIONARIO = new DiccionarioYaml().leer();

    private DiccionarioReal() {}

    /** La carga con ese código. */
    static Carga carga(String codigo) {
        return DICCIONARIO.carga(codigo).orElseThrow();
    }

    /** Las reglas de una columna de una carga. */
    static ReglasDeColumna reglas(String carga, String campo) {
        return DICCIONARIO.validador(carga(carga)).reglas(campo);
    }

    /** La tabla de un archivo de prueba, como la entrega la fuente de datos. */
    static TablaDeDatos tabla(String archivo) {
        return new LectorPorFormato().leer(archivo, DatosDePrueba.contenido(archivo)).tabla();
    }

    /**
     * Lo que existiría en el sistema con las cargas 01 a 06 de prueba confirmadas: las unidades,
     * las carreras, los periodos, las asignaturas y los docentes.
     */
    static ReferenciasExistentes referenciasDePrueba() {
        return new ReferenciasExistentes(
                columna("01_estructura_organica.csv", "codigo_unidad"),
                columna("02_carreras.csv", "codigo_carrera"),
                columna("03_ciclos_academicos.csv", "codigo_periodo"),
                columna("04_asignaturas_malla.csv", "codigo_asignatura"),
                columna("06_docentes.csv", "numero_identificacion"));
    }

    private static Set<String> columna(String archivo, String campo) {
        TablaDeDatos tabla = tabla(archivo);
        int indice = tabla.encabezados().indexOf(campo);
        Set<String> valores = new HashSet<>();
        for (List<String> fila : tabla.filas()) {
            valores.add(fila.get(indice).strip());
        }
        return valores;
    }
}
