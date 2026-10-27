package ec.mdd.cargas.dominio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Revisa cada fila del archivo de una carga con las reglas del diccionario de datos y las
 * referencias del sistema, y devuelve el informe por fila (RF-08, RF-09, F01-H03). Se arma una vez
 * por carga, al leer el diccionario. No guarda nada (FR-008); las reglas propias de cada servicio
 * dueño las aplica el dueño al confirmar la carga (aclaración 1, F01-H04).
 */
public final class ValidadorDeFilas {

    /** El campo que dice si la identificación es una cédula o un pasaporte. */
    private static final String TIPO_DE_IDENTIFICACION = "tipo_identificacion";

    private static final String ESPACIOS = "Tiene espacios en los extremos: se quitan.";
    private static final String FILA_VACIA = "Fila vacía: se ignora.";

    private final Carga carga;
    private final Map<String, ReglasDeColumna> reglas = new LinkedHashMap<>();
    private final Map<String, CondicionDeObligatoriedad> condiciones = new HashMap<>();
    private final Map<String, ReferenciaDeColumna> referencias = new HashMap<>();
    private final Optional<ClaveDeCarga> clave;

    /**
     * Lee las reglas de las columnas de una carga.
     *
     * @param carga la carga
     * @param catalogoDe los valores del catálogo de una columna, o ninguno
     * @param otraColumna los valores permitidos de una columna de otra carga, por número y campo
     * @throws IllegalArgumentException si una regla del diccionario no se puede leer
     */
    ValidadorDeFilas(
            Carga carga,
            Function<ColumnaDeCarga, List<String>> catalogoDe,
            BiFunction<Integer, String, List<String>> otraColumna) {
        this.carga = carga;
        for (ColumnaDeCarga columna : carga.columnas()) {
            reglas.put(
                    columna.campo(),
                    new ReglasDeColumna(columna, catalogoDe.apply(columna), otraColumna));
        }
        for (ColumnaDeCarga columna : carga.columnas()) {
            condiciones.put(
                    columna.campo(),
                    CondicionDeObligatoriedad.de(
                            columna, carga, campo -> reglas.get(campo).valoresPermitidos()));
            ReferenciaDeColumna.de(columna, carga, reglas.get(columna.campo()))
                    .ifPresent(referencia -> referencias.put(columna.campo(), referencia));
        }
        this.clave = ClaveDeCarga.de(carga);
    }

    /** Las reglas de una columna. */
    public ReglasDeColumna reglas(String campo) {
        return existente(reglas, campo);
    }

    /** La obligatoriedad de una columna. */
    public CondicionDeObligatoriedad condicion(String campo) {
        return existente(condiciones, campo);
    }

    /** La referencia de una columna, si tiene {@code FK}. */
    public Optional<ReferenciaDeColumna> referencia(String campo) {
        existente(reglas, campo);
        return Optional.ofNullable(referencias.get(campo));
    }

    /**
     * Revisa las filas de una tabla que ya tiene la estructura de la carga (F01-H02).
     *
     * @param tabla los encabezados de la plantilla y las filas
     * @param existentes lo que hay en el sistema
     * @return el informe, con los problemas en orden de fila y de columna
     */
    public InformeValidacionCarga validar(TablaDeDatos tabla, ReferenciasExistentes existentes) {
        List<String> campos = tabla.encabezados().stream().map(String::strip).toList();
        List<List<String>> filas = tabla.filas();
        int ultima = ultimaConDatos(filas);
        Set<String> delArchivo = clavesDelArchivo(campos, filas);
        Map<List<String>, Integer> vistas = new HashMap<>();
        List<ProblemaDeFila> problemas = new ArrayList<>();
        for (int i = 0; i <= ultima; i++) {
            int fila = i + 2;
            List<String> celdas = filas.get(i);
            if (celdas.stream().allMatch(String::isBlank)) {
                problemas.add(ProblemaDeFila.advertencia(fila, "", "", FILA_VACIA));
                continue;
            }
            Map<String, String> valores = new HashMap<>();
            for (int c = 0; c < campos.size(); c++) {
                valores.put(campos.get(c), celdas.get(c).strip());
            }
            for (int c = 0; c < campos.size(); c++) {
                revisarCelda(fila, campos.get(c), celdas.get(c), valores, existentes, delArchivo)
                        .forEach(problemas::add);
            }
            clave.flatMap(k -> repetida(k, fila, valores, vistas)).ifPresent(problemas::add);
        }
        return new InformeValidacionCarga(tabla.registros(), problemas);
    }

    private List<ProblemaDeFila> revisarCelda(
            int fila,
            String campo,
            String crudo,
            Map<String, String> valores,
            ReferenciasExistentes existentes,
            Set<String> delArchivo) {
        String valor = valores.get(campo);
        List<ProblemaDeFila> problemas = new ArrayList<>();
        if (!crudo.equals(valor) && !valor.isEmpty()) {
            problemas.add(ProblemaDeFila.advertencia(fila, campo, valor, ESPACIOS));
        }
        if (valor.isEmpty()) {
            CondicionDeObligatoriedad condicion = condiciones.get(campo);
            if (condicion.exige(valores)) {
                problemas.add(ProblemaDeFila.error(fila, campo, valor, condicion.motivo()));
            }
            return problemas;
        }
        ReglasDeColumna regla = reglas.get(campo);
        Optional<String> motivo = regla.revisar(valor);
        if (motivo.isPresent()) {
            problemas.add(ProblemaDeFila.error(fila, campo, valor, motivo.get()));
            return problemas;
        }
        if (regla.esIdentificacion()) {
            Optional<ProblemaDeFila> identificacion =
                    identificacion(fila, campo, valor, valores.get(TIPO_DE_IDENTIFICACION));
            if (identificacion.isPresent()) {
                problemas.add(identificacion.get());
                return problemas;
            }
        }
        ReferenciaDeColumna referencia = referencias.get(campo);
        if (referencia != null) {
            referencia(fila, campo, valor, referencia, existentes, delArchivo)
                    .ifPresent(problemas::add);
        }
        return problemas;
    }

    private static Optional<ProblemaDeFila> identificacion(
            int fila, String campo, String valor, String tipo) {
        if ("CEDULA".equals(tipo) && !Cedula.valida(valor)) {
            Optional<String> sugerencia = Cedula.conCeroInicial(valor);
            String motivo = sugerencia.isPresent() ? Cedula.SIN_CERO : Cedula.INVALIDA;
            return Optional.of(
                    new ProblemaDeFila(
                            fila, campo, valor, motivo, TipoDeProblema.ERROR, sugerencia));
        }
        if ("PASAPORTE".equals(tipo) && !Cedula.pasaporte(valor)) {
            return Optional.of(ProblemaDeFila.error(fila, campo, valor, Cedula.PASAPORTE_INVALIDO));
        }
        return Optional.empty();
    }

    private Optional<ProblemaDeFila> referencia(
            int fila,
            String campo,
            String valor,
            ReferenciaDeColumna referencia,
            ReferenciasExistentes existentes,
            Set<String> delArchivo) {
        ReglasDeColumna regla = reglas.get(campo);
        List<String> codigos = regla.esLista() ? ReglasDeColumna.elementos(valor) : List.of(valor);
        for (int i = 0; i < codigos.size(); i++) {
            String codigo = codigos.get(i);
            if (referencia.parte() >= 0) {
                codigo = codigo.split("\\|", -1)[referencia.parte()].strip();
            }
            boolean existe =
                    existentes.existe(referencia.tipo(), codigo)
                            || (referencia.mismoArchivo() && delArchivo.contains(codigo));
            if (!existe) {
                String nombre = referencia.tipo().nombre();
                String motivo =
                        regla.esLista()
                                ? "El elemento "
                                        + (i + 1)
                                        + " no existe en el sistema como "
                                        + sinArticulo(nombre)
                                        + ": regístralo o confirma antes su carga."
                                : "No existe "
                                        + nombre
                                        + " en el sistema: regístral"
                                        + terminacion(nombre)
                                        + " o confirma antes su carga.";
                if (referencia.mismoArchivo()) {
                    motivo =
                            "No existe "
                                    + nombre
                                    + " ni en este archivo ni en el sistema: agrégala.";
                }
                return Optional.of(ProblemaDeFila.error(fila, campo, valor, motivo));
            }
        }
        return Optional.empty();
    }

    private static Optional<ProblemaDeFila> repetida(
            ClaveDeCarga clave,
            int fila,
            Map<String, String> valores,
            Map<List<String>, Integer> vistas) {
        List<String> valorDeLaClave = clave.campos().stream().map(valores::get).toList();
        if (valorDeLaClave.stream().anyMatch(String::isEmpty)) {
            return Optional.empty();
        }
        Integer anterior = vistas.putIfAbsent(valorDeLaClave, fila);
        if (anterior == null) {
            return Optional.empty();
        }
        return Optional.of(
                ProblemaDeFila.error(
                        fila, clave.campo(), valores.get(clave.campo()), clave.motivo(anterior)));
    }

    /** Las claves del propio archivo que puede citar una referencia del mismo archivo. */
    private Set<String> clavesDelArchivo(List<String> campos, List<List<String>> filas) {
        Set<String> claves = new HashSet<>();
        for (ReferenciaDeColumna referencia : referencias.values()) {
            if (referencia.mismoArchivo()) {
                int indice = campos.indexOf(referencia.campoDelArchivo());
                for (List<String> fila : filas) {
                    claves.add(fila.get(indice).strip());
                }
            }
        }
        return claves;
    }

    private static int ultimaConDatos(List<List<String>> filas) {
        int ultima = filas.size() - 1;
        while (ultima >= 0 && filas.get(ultima).stream().allMatch(String::isBlank)) {
            ultima--;
        }
        return ultima;
    }

    /**
     * La terminación del pronombre: «regístrala» para «la carrera», «regístralo» para «el periodo».
     */
    private static String terminacion(String nombre) {
        return nombre.startsWith("la ") ? "a" : "o";
    }

    private static String sinArticulo(String nombre) {
        return nombre.substring(nombre.indexOf(' ') + 1);
    }

    private <T> T existente(Map<String, T> mapa, String campo) {
        T valor = mapa.get(campo);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "La carga " + carga.codigo() + " no tiene la columna " + campo);
        }
        return valor;
    }
}
