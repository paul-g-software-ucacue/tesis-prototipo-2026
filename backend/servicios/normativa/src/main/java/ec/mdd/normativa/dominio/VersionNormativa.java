package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Versión de normativa: el conjunto numerado de parámetros de RN-01 a RN-26 que rige un periodo
 * (glosario §7.3, RF-19, modelo de dominio §5.1). No cambia una vez asociada a un periodo:
 * cualquier ajuste crea una versión nueva.
 *
 * <p>Es inmutable. La versión 1 llega por migración con la Política SENESCYT 2024 (F04-H01). Desde
 * F04-H02, una versión nueva se crea copiando otra ({@link #copiaDe}), se edita mientras no rija
 * ningún periodo ({@link #editada}) y se asocia a periodos ({@link #asociadaA}); cada operación
 * devuelve una versión nueva.
 *
 * @param numero número de la versión, desde 1
 * @param codigo código de su origen, por ejemplo {@code SENESCYT-2024-03}
 * @param origen de dónde sale, por ejemplo «Política SENESCYT 2024»
 * @param vigenteDesde desde cuándo rige la norma
 * @param tipoIst el tipo de IST de la institución (Tipo 2 en el prototipo)
 * @param tabla la tabla de carga horaria
 * @param reglas las reglas con sus parámetros, sin repetir, ordenadas por identificador
 * @param copiadaDe el número de la versión copiada; {@code null} en la versión 1
 * @param asignaciones los periodos que rige, sin repetir, en el orden en que se asociaron
 */
public record VersionNormativa(
        int numero,
        String codigo,
        String origen,
        LocalDate vigenteDesde,
        TipoIst tipoIst,
        TablaCargaHoraria tabla,
        List<Regla> reglas,
        Integer copiadaDe,
        List<AsignacionNormativa> asignaciones) {

    private static final BigDecimal MAXIMO = new BigDecimal("100000000");

    /** Comprueba las invariantes, ordena las reglas y copia las asignaciones. */
    public VersionNormativa {
        if (numero < 1) {
            throw new IllegalArgumentException("El número de versión empieza en 1: " + numero);
        }
        if (codigo == null || codigo.isBlank() || origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("La versión necesita código y origen");
        }
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(tipoIst, "tipoIst");
        Objects.requireNonNull(tabla, "tabla");
        Set<String> identificadores = new HashSet<>();
        for (Regla regla : Objects.requireNonNull(reglas, "reglas")) {
            if (!identificadores.add(regla.identificador())) {
                throw new IllegalArgumentException(
                        "La regla "
                                + regla.identificador()
                                + " está repetida en la versión "
                                + numero);
            }
        }
        // Por identificador, con las RF-xx (RF-24) después de las RN-xx.
        reglas =
                reglas.stream()
                        .sorted(
                                Comparator.comparing(
                                                (Regla regla) ->
                                                        regla.identificador().startsWith("RF-"))
                                        .thenComparing(Regla::identificador))
                        .toList();
        if (copiadaDe != null && copiadaDe == numero) {
            throw new IllegalArgumentException("La versión " + numero + " no se copia de sí misma");
        }
        Set<String> periodos = new HashSet<>();
        for (AsignacionNormativa asignacion :
                Objects.requireNonNull(asignaciones, "asignaciones")) {
            if (!periodos.add(asignacion.periodo())) {
                throw new IllegalArgumentException(
                        "El periodo " + asignacion.periodo() + " está repetido");
            }
        }
        asignaciones = List.copyOf(asignaciones);
    }

    /** Una versión que no se copió de otra ni rige periodos, como la versión 1. */
    public VersionNormativa(
            int numero,
            String codigo,
            String origen,
            LocalDate vigenteDesde,
            TipoIst tipoIst,
            TablaCargaHoraria tabla,
            List<Regla> reglas) {
        this(numero, codigo, origen, vigenteDesde, tipoIst, tabla, reglas, null, List.of());
    }

    /**
     * Una versión nueva con la tabla, las reglas y el tipo de IST de otra, y su propia cabecera. No
     * rige ningún periodo, así que se puede editar.
     *
     * @param origen la versión que se copia
     * @param numero el número de la nueva, distinto del de la copiada
     * @param datos su cabecera
     * @param momento cuándo se crea, para el evento
     * @return la versión nueva y su evento {@link VersionNormativaCreada}
     */
    public static Copia copiaDe(
            VersionNormativa origen, int numero, DatosDeVersion datos, Instant momento) {
        VersionNormativa nueva =
                new VersionNormativa(
                        numero,
                        datos.codigo(),
                        datos.origen(),
                        datos.vigenteDesde(),
                        origen.tipoIst(),
                        origen.tabla(),
                        origen.reglas(),
                        origen.numero(),
                        List.of());
        return new Copia(
                nueva,
                new VersionNormativaCreada(numero, datos.codigo(), origen.numero(), momento));
    }

    /**
     * La versión con otra cabecera, otras horas en sus filas y otros valores en sus parámetros. Las
     * filas, las reglas y los parámetros tienen que ser exactamente los de la versión (FR-002), y
     * cada fila tiene que cumplir sus invariantes.
     *
     * @param cambios la edición completa
     * @return la versión editada y lo que cambió, valor por valor
     * @throws VersionEnUso si la versión rige un periodo
     * @throws CambioNoPermitido si se agrega o se quita una fila, una regla o un parámetro
     * @throws CambioNoPermitido si un parámetro es negativo, tiene más de dos decimales o no cabe
     * @throws FilaInvalida si una fila no cumple sus invariantes
     */
    public Edicion editada(CambiosDeVersion cambios) {
        if (enUso()) {
            throw new VersionEnUso(numero);
        }
        List<CambioDeValor> lista = new ArrayList<>();
        DatosDeVersion datos = cambios.datos();
        anotar(lista, "codigo", codigo, datos.codigo());
        anotar(lista, "origen", origen, datos.origen());
        anotar(lista, "vigenteDesde", vigenteDesde.toString(), datos.vigenteDesde().toString());
        TablaCargaHoraria nuevaTabla = new TablaCargaHoraria(filasEditadas(cambios.filas(), lista));
        List<Regla> nuevasReglas = reglasEditadas(cambios.parametros(), lista);
        if (lista.isEmpty()) {
            return new Edicion(this, List.of());
        }
        return new Edicion(
                new VersionNormativa(
                        numero,
                        datos.codigo(),
                        datos.origen(),
                        datos.vigenteDesde(),
                        tipoIst,
                        nuevaTabla,
                        nuevasReglas,
                        copiadaDe,
                        asignaciones),
                List.copyOf(lista));
    }

    /**
     * La versión asociada también a ese periodo. Desde entonces no cambia. Asociarla otra vez al
     * mismo periodo no cambia nada.
     *
     * @param periodo el código del periodo
     * @param momento cuándo se asocia
     */
    public VersionNormativa asociadaA(String periodo, Instant momento) {
        AsignacionNormativa asignacion = new AsignacionNormativa(periodo, momento);
        if (periodos().contains(periodo)) {
            return this;
        }
        List<AsignacionNormativa> todas = new ArrayList<>(asignaciones);
        todas.add(asignacion);
        return new VersionNormativa(
                numero, codigo, origen, vigenteDesde, tipoIst, tabla, reglas, copiadaDe, todas);
    }

    /** Si rige al menos un periodo: entonces ya no cambia. */
    public boolean enUso() {
        return !asignaciones.isEmpty();
    }

    /** Los códigos de los periodos que rige, en el orden en que se asociaron. */
    public List<String> periodos() {
        return asignaciones.stream().map(AsignacionNormativa::periodo).toList();
    }

    /** La regla con ese identificador, si la versión la tiene. */
    public Optional<Regla> regla(String identificador) {
        return reglas.stream()
                .filter(regla -> regla.identificador().equals(identificador))
                .findFirst();
    }

    private List<FilaCargaHoraria> filasEditadas(
            List<HorasDeFila> horas, List<CambioDeValor> lista) {
        Map<String, HorasDeFila> porCampo = new LinkedHashMap<>();
        for (HorasDeFila fila : horas) {
            if (porCampo.put(fila.campo(), fila) != null) {
                throw new CambioNoPermitido(fila.campo(), "La fila está repetida");
            }
        }
        List<FilaCargaHoraria> filas = new ArrayList<>();
        for (FilaCargaHoraria actual : tabla.filas()) {
            String campo = HorasDeFila.campo(actual.perfil(), actual.aplicaA());
            HorasDeFila nueva = porCampo.remove(campo);
            if (nueva == null) {
                throw new CambioNoPermitido(
                        campo, "Falta la fila: en una versión no se quitan filas");
            }
            anotar(
                    lista,
                    campo + ".dedicacionTotal",
                    actual.dedicacionTotal(),
                    nueva.dedicacionTotal());
            anotar(lista, campo + ".horasClase", actual.horasClase(), nueva.horasClase());
            anotar(
                    lista,
                    campo + ".horasClaseMinimo",
                    actual.horasClaseMinimo(),
                    nueva.horasClaseMinimo());
            anotar(
                    lista,
                    campo + ".horasClaseMaximo",
                    actual.horasClaseMaximo(),
                    nueva.horasClaseMaximo());
            anotar(
                    lista,
                    campo + ".horasOtrasDocencia",
                    actual.horasOtrasDocencia(),
                    nueva.horasOtrasDocencia());
            anotar(lista, campo + ".horasGestion", actual.horasGestion(), nueva.horasGestion());
            anotar(
                    lista,
                    campo + ".horasInvestigacionVinculacionMaximo",
                    actual.horasInvestigacionVinculacionMaximo(),
                    nueva.horasInvestigacionVinculacionMaximo());
            filas.add(
                    new FilaCargaHoraria(
                            actual.aplicaA(),
                            actual.perfil(),
                            nueva.dedicacionTotal(),
                            nueva.horasClase(),
                            nueva.horasClaseMinimo(),
                            nueva.horasClaseMaximo(),
                            nueva.horasOtrasDocencia(),
                            nueva.horasGestion(),
                            nueva.horasInvestigacionVinculacionMaximo(),
                            actual.severidad(),
                            actual.referencia(),
                            actual.observacion()));
        }
        if (!porCampo.isEmpty()) {
            throw new CambioNoPermitido(
                    porCampo.keySet().iterator().next(),
                    "La fila no está en la versión: en una versión no se agregan filas");
        }
        return filas;
    }

    private List<Regla> reglasEditadas(
            Map<String, Map<String, BigDecimal>> parametros, List<CambioDeValor> lista) {
        Map<String, Map<String, BigDecimal>> pendientes = new LinkedHashMap<>(parametros);
        List<Regla> nuevas = new ArrayList<>();
        for (Regla actual : reglas) {
            String campo = "reglas[" + actual.identificador() + "]";
            Map<String, BigDecimal> valores = pendientes.remove(actual.identificador());
            if (valores == null) {
                throw new CambioNoPermitido(
                        campo, "Falta la regla: en una versión no se quitan reglas");
            }
            if (!valores.keySet().equals(actual.parametros().keySet())) {
                throw new CambioNoPermitido(
                        campo,
                        "Los parámetros de "
                                + actual.identificador()
                                + " no son los de la versión: solo cambian sus valores");
            }
            Map<String, BigDecimal> nuevos = new LinkedHashMap<>();
            actual.parametros()
                    .forEach(
                            (nombre, anterior) -> {
                                BigDecimal nuevo =
                                        Objects.requireNonNull(valores.get(nombre), nombre);
                                valorValido(campo + "." + nombre, nuevo);
                                if (anterior.compareTo(nuevo) != 0) {
                                    lista.add(
                                            new CambioDeValor(
                                                    campo + "." + nombre,
                                                    texto(anterior),
                                                    texto(nuevo)));
                                    nuevos.put(nombre, nuevo);
                                } else {
                                    nuevos.put(nombre, anterior);
                                }
                            });
            nuevas.add(new Regla(actual.identificador(), actual.severidad(), nuevos));
        }
        if (!pendientes.isEmpty()) {
            throw new CambioNoPermitido(
                    "reglas[" + pendientes.keySet().iterator().next() + "]",
                    "La regla no está en la versión: en una versión no se agregan reglas");
        }
        return nuevas;
    }

    private static void anotar(
            List<CambioDeValor> lista, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            lista.add(
                    new CambioDeValor(
                            campo,
                            anterior == null ? null : anterior.toString(),
                            nuevo == null ? null : nuevo.toString()));
        }
    }

    // Los parámetros son cifras no negativas con hasta dos decimales, como los guarda la base.
    private static void valorValido(String campo, BigDecimal valor) {
        if (valor.signum() < 0) {
            throw new CambioNoPermitido(campo, "El valor no puede ser negativo");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new CambioNoPermitido(campo, "El valor admite hasta dos decimales");
        }
        if (valor.compareTo(MAXIMO) >= 0) {
            throw new CambioNoPermitido(campo, "El valor es demasiado grande");
        }
    }

    private static String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    /**
     * Una versión recién creada y su evento.
     *
     * @param version la versión nueva
     * @param evento su evento de dominio
     */
    public record Copia(VersionNormativa version, VersionNormativaCreada evento) {}

    /**
     * Una versión editada y lo que cambió.
     *
     * @param version la versión editada; la misma si nada cambió
     * @param cambios los valores que cambiaron, en el orden de la versión; vacía si ninguno
     */
    public record Edicion(VersionNormativa version, List<CambioDeValor> cambios) {}
}
