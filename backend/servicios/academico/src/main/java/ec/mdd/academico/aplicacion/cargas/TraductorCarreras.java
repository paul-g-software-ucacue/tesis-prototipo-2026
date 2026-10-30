package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.puertos.entrada.DatosDeCarrera;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * La carga 02, las carreras (F01-H04): sus columnas, traducidas a las carreras de Académico
 * (FR-012). Registra y corrige con {@link MantenerCarreras}, así que se aplican sus reglas, como
 * las siglas únicas o la sede que existe.
 */
final class TraductorCarreras extends TraductorAcademico<Carrera> {

    private final RepositorioCarreras repositorio;
    private final MantenerCarreras mantener;

    TraductorCarreras(RepositorioCarreras repositorio, MantenerCarreras mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "carreras";
    }

    @Override
    public boolean tieneDatos() {
        return repositorio.hayCarrerasVigentes();
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, Carrera> existentes(LoteDeCarga lote) {
        Map<String, Carrera> existentes = new LinkedHashMap<>();
        repositorio.carreras().forEach(carrera -> existentes.put(carrera.codigo(), carrera));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("codigo_carrera");
    }

    @Override
    public Map<String, String> campos(Carrera carrera) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", carrera.nombre());
        campos.put("siglas", carrera.siglas());
        campos.put("codigo_unidad_sede", carrera.sede());
        campos.put("nivel_formacion", carrera.nivelFormacion().name());
        campos.put("modalidad", carrera.modalidad().name());
        campos.put(
                "jornadas",
                carrera.jornadas().stream().map(Enum::name).collect(Collectors.joining("; ")));
        campos.put("campo_amplio", carrera.campo().amplio());
        campos.put("campo_especifico", carrera.campo().especifico());
        campos.put("campo_detallado", texto(carrera.campoDetallado()));
        campos.put("numero_ciclos", String.valueOf(carrera.numeroCiclos()));
        campos.put("estado", carrera.estado().name());
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", fila.valor("nombre"));
        campos.put("siglas", fila.valor("siglas"));
        campos.put("codigo_unidad_sede", fila.valor("codigo_unidad_sede"));
        campos.put("nivel_formacion", fila.valor("nivel_formacion"));
        campos.put("modalidad", fila.valor("modalidad"));
        campos.put(
                "jornadas",
                jornadas(fila).stream().map(Enum::name).collect(Collectors.joining("; ")));
        campos.put("campo_amplio", fila.valor("campo_amplio"));
        campos.put("campo_especifico", fila.valor("campo_especifico"));
        campos.put("campo_detallado", fila.valor("campo_detallado"));
        campos.put("numero_ciclos", String.valueOf(Integer.parseInt(fila.valor("numero_ciclos"))));
        campos.put("estado", fila.valor("estado"));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        DatosDeCarrera datos = datos(fila);
        mantener.registrarCarrera(
                Carrera.nueva(
                        clave(fila),
                        datos.nombre(),
                        datos.siglas(),
                        datos.sede(),
                        datos.nivelFormacion(),
                        datos.modalidad(),
                        datos.jornadas(),
                        datos.campo(),
                        datos.campoDetallado(),
                        datos.numeroCiclos()),
                quien);
        EstadoDeCarrera estado = EstadoDeCarrera.valueOf(fila.valor("estado"));
        if (estado != EstadoDeCarrera.VIGENTE) {
            mantener.cambiarEstadoCarrera(clave(fila), estado, quien);
        }
    }

    @Override
    public void corregir(FilaDelLote fila, Carrera actual, String quien) {
        Map<String, String> ahora = campos(actual);
        Map<String, String> archivo = campos(fila);
        ahora.remove("estado");
        archivo.remove("estado");
        if (!ahora.equals(archivo)) {
            mantener.corregirCarrera(clave(fila), datos(fila), quien);
        }
        EstadoDeCarrera estado = EstadoDeCarrera.valueOf(fila.valor("estado"));
        if (actual.estado() != estado) {
            mantener.cambiarEstadoCarrera(clave(fila), estado, quien);
        }
    }

    private static DatosDeCarrera datos(FilaDelLote fila) {
        return new DatosDeCarrera(
                fila.valor("nombre"),
                fila.valor("siglas"),
                fila.valor("codigo_unidad_sede"),
                NivelDeFormacion.valueOf(fila.valor("nivel_formacion")),
                Modalidad.valueOf(fila.valor("modalidad")),
                jornadas(fila),
                new CampoConocimiento(fila.valor("campo_amplio"), fila.valor("campo_especifico")),
                nulo(fila.valor("campo_detallado")),
                Integer.parseInt(fila.valor("numero_ciclos")));
    }

    private static List<Jornada> jornadas(FilaDelLote fila) {
        return Arrays.stream(fila.valor("jornadas").split(";"))
                .map(String::strip)
                .filter(jornada -> !jornada.isEmpty())
                .map(Jornada::valueOf)
                .toList();
    }
}
