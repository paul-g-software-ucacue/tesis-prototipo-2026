package ec.mdd.normativa.aplicacion.cargas;

import ec.mdd.normativa.dominio.CambioNoPermitido;
import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.FilaInvalida;
import ec.mdd.normativa.dominio.HorasDeFila;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.RegistroDeLaVista;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.TraductorDeCarga;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * La carga 07, la política de carga horaria (F01-H04, research §7): sus filas, traducidas a las
 * versiones de normativa (FR-012). La clave es la versión, el tipo de IST y el perfil. Una versión
 * se aplica entera, con la edición de F04-H03, que es la única forma de cambiarla:
 *
 * <ul>
 *   <li>una que no existe se crea copiando la última, para conservar sus reglas, y se edita con las
 *       filas del archivo;
 *   <li>una que existe se edita, si no rige ningún periodo; si rige uno, Normativa la rechaza con
 *       {@code VersionEnUso} (principio VII).
 * </ul>
 *
 * <p>El archivo trae la tabla completa de cada versión que nombra: en una versión no se agregan ni
 * se quitan filas, así que una baja se rechaza al confirmar. La severidad, la referencia y la
 * observación no se editan (FR-002 de F04-H03) y no cuentan como cambio.
 */
final class TraductorPolitica implements TraductorDeCarga<TraductorPolitica.FilaDeVersion> {

    /** Una fila de la tabla de una versión. */
    record FilaDeVersion(VersionNormativa version, FilaCargaHoraria fila) {}

    private static final String TODOS = "TODOS";

    private final RepositorioVersiones repositorio;
    private final CrearVersion crear;
    private final EditarVersion editar;

    TraductorPolitica(RepositorioVersiones repositorio, CrearVersion crear, EditarVersion editar) {
        this.repositorio = repositorio;
        this.crear = crear;
        this.editar = editar;
    }

    @Override
    public String carga() {
        return "politica_carga_horaria";
    }

    @Override
    public boolean tieneDatos() {
        return repositorio.hayVersiones();
    }

    @Override
    public void bloquear() {
        repositorio.todas().forEach(version -> repositorio.bloquear(version.numero()));
    }

    @Override
    public Map<String, FilaDeVersion> existentes(LoteDeCarga lote) {
        List<String> codigos = codigos(lote);
        Map<String, FilaDeVersion> existentes = new LinkedHashMap<>();
        for (VersionNormativa version : repositorio.todas()) {
            if (!codigos.contains(version.codigo())) {
                continue;
            }
            for (FilaCargaHoraria fila : version.tabla().filas()) {
                existentes.put(
                        clave(version.codigo(), tipo(fila.aplicaA()), fila.perfil()),
                        new FilaDeVersion(version, fila));
            }
        }
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return clave(
                fila.valor("codigo_version_politica"),
                fila.valor("tipo_ist"),
                fila.valor("perfil"));
    }

    @Override
    public Map<String, String> campos(FilaDeVersion existente) {
        FilaCargaHoraria fila = existente.fila();
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("fecha_vigencia_desde", existente.version().vigenteDesde().toString());
        campos.put("dedicacion_total", String.valueOf(fila.dedicacionTotal()));
        campos.put("horas_clase", String.valueOf(fila.horasClase()));
        campos.put("horas_clase_min", String.valueOf(fila.horasClaseMinimo()));
        campos.put("horas_clase_max", String.valueOf(fila.horasClaseMaximo()));
        campos.put("horas_otras_docencia", texto(fila.horasOtrasDocencia()));
        campos.put("horas_gestion", texto(fila.horasGestion()));
        campos.put(
                "horas_investigacion_vinculacion_max",
                texto(fila.horasInvestigacionVinculacionMaximo()));
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("fecha_vigencia_desde", fila.valor("fecha_vigencia_desde"));
        for (String campo :
                List.of(
                        "dedicacion_total",
                        "horas_clase",
                        "horas_clase_min",
                        "horas_clase_max",
                        "horas_otras_docencia",
                        "horas_gestion",
                        "horas_investigacion_vinculacion_max")) {
            String valor = fila.valor(campo);
            campos.put(
                    campo,
                    valor.isEmpty()
                            ? ""
                            : new BigDecimal(valor).stripTrailingZeros().toPlainString());
        }
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        throw new UnsupportedOperationException("La política se aplica por versión");
    }

    @Override
    public void corregir(FilaDelLote fila, FilaDeVersion actual, String quien) {
        throw new UnsupportedOperationException("La política se aplica por versión");
    }

    /** Cada versión del archivo con una fila nueva o que cambia se crea o se edita entera. */
    @Override
    public ResultadoDelLote aplicar(LoteDeCarga lote, VistaPreviaDelLote vista, String quien) {
        Map<String, List<FilaDelLote>> porVersion =
                lote.filas().stream()
                        .collect(
                                Collectors.groupingBy(
                                        fila -> fila.valor("codigo_version_politica"),
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        Map<Integer, TipoDeRegistro> tipoDeFila = new LinkedHashMap<>();
        for (RegistroDeLaVista registro : vista.registros()) {
            if (registro.fila() != null) {
                tipoDeFila.put(registro.fila(), registro.tipo());
            }
        }
        int nuevos = 0;
        int cambiados = 0;
        for (Map.Entry<String, List<FilaDelLote>> version : porVersion.entrySet()) {
            List<FilaDelLote> filas = version.getValue();
            long deEstaVersionNuevas =
                    filas.stream()
                            .filter(f -> tipoDeFila.get(f.fila()) == TipoDeRegistro.NUEVO)
                            .count();
            long deEstaVersionCambian =
                    filas.stream()
                            .filter(f -> tipoDeFila.get(f.fila()) == TipoDeRegistro.CAMBIA)
                            .count();
            boolean conBajas =
                    vista.registros().stream()
                            .filter(r -> r.tipo() == TipoDeRegistro.BAJA)
                            .anyMatch(r -> r.clave().startsWith(version.getKey() + " · "));
            if (deEstaVersionNuevas + deEstaVersionCambian == 0 && !conBajas) {
                continue;
            }
            FilaDelLote primera = filas.getFirst();
            try {
                aplicarVersion(version.getKey(), filas, quien);
            } catch (RechazoDelLote error) {
                throw error;
            } catch (FilaInvalida error) {
                throw new RechazoDelLote(primera.fila(), "perfil", error.motivo());
            } catch (CambioNoPermitido error) {
                throw new RechazoDelLote(primera.fila(), error.campo(), error.motivo());
            } catch (RuntimeException error) {
                throw new RechazoDelLote(primera.fila(), error);
            }
            nuevos += (int) deEstaVersionNuevas;
            cambiados += (int) deEstaVersionCambian;
        }
        return new ResultadoDelLote(nuevos, cambiados, vista.cuantos(TipoDeRegistro.BAJA));
    }

    private void aplicarVersion(String codigo, List<FilaDelLote> filas, String quien) {
        FilaDelLote primera = filas.getFirst();
        LocalDate vigenteDesde = LocalDate.parse(primera.valor("fecha_vigencia_desde"));
        Optional<VersionNormativa> existente =
                repositorio.todas().stream().filter(v -> v.codigo().equals(codigo)).findFirst();
        VersionNormativa version =
                existente.orElseGet(
                        () ->
                                crear.crear(
                                        ultima().numero(),
                                        new DatosDeVersion(
                                                codigo,
                                                primera.valor("referencia_normativa"),
                                                vigenteDesde),
                                        quien));
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        for (Regla regla : version.reglas()) {
            parametros.put(regla.identificador(), regla.parametros());
        }
        editar.editar(
                version.numero(),
                new CambiosDeVersion(
                        new DatosDeVersion(version.codigo(), version.origen(), vigenteDesde),
                        filas.stream().map(TraductorPolitica::horas).toList(),
                        parametros),
                quien);
    }

    private VersionNormativa ultima() {
        return repositorio.todas().stream()
                .max(Comparator.comparingInt(VersionNormativa::numero))
                .orElseThrow(
                        () -> new IllegalStateException("No hay ninguna versión de normativa"));
    }

    private static HorasDeFila horas(FilaDelLote fila) {
        String tipo = fila.valor("tipo_ist");
        return new HorasDeFila(
                TODOS.equals(tipo) ? null : TipoIst.valueOf(tipo),
                fila.valor("perfil"),
                entero(fila.valor("dedicacion_total")),
                entero(fila.valor("horas_clase")),
                entero(fila.valor("horas_clase_min")),
                entero(fila.valor("horas_clase_max")),
                enteroONulo(fila.valor("horas_otras_docencia")),
                enteroONulo(fila.valor("horas_gestion")),
                enteroONulo(fila.valor("horas_investigacion_vinculacion_max")));
    }

    /** Un entero del archivo; un decimal que no es entero lo rechaza Normativa. */
    private static int entero(String texto) {
        BigDecimal valor = new BigDecimal(texto);
        try {
            return valor.intValueExact();
        } catch (ArithmeticException e) {
            throw new CambioNoPermitido("horas", "Las horas de la política son enteras");
        }
    }

    private static Integer enteroONulo(String texto) {
        return texto.isEmpty() ? null : entero(texto);
    }

    private static List<String> codigos(LoteDeCarga lote) {
        return lote.filas().stream()
                .map(fila -> fila.valor("codigo_version_politica"))
                .distinct()
                .toList();
    }

    private static String tipo(TipoIst tipo) {
        return tipo == null ? TODOS : tipo.name();
    }

    private static String texto(Integer valor) {
        return valor == null ? "" : valor.toString();
    }

    private static String clave(String version, String tipo, String perfil) {
        return version + " · " + tipo + " · " + perfil;
    }
}
