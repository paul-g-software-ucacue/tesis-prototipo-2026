package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.DatosDeAsignatura;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La carga 04, las asignaturas y su malla (F01-H04): sus columnas, traducidas a las asignaturas de
 * Académico (FR-012). Una malla que no existe se registra antes que su primera asignatura (research
 * §4). Registra y corrige con {@link MantenerCarreras}, así que se aplican sus reglas. La carrera y
 * la malla de una asignatura no cambian.
 */
final class TraductorAsignaturas extends TraductorAcademico<Asignatura> {

    private final RepositorioCarreras repositorio;
    private final MantenerCarreras mantener;

    TraductorAsignaturas(RepositorioCarreras repositorio, MantenerCarreras mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "asignaturas_malla";
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, Asignatura> existentes(LoteDeCarga lote) {
        Map<String, Asignatura> existentes = new LinkedHashMap<>();
        repositorio
                .asignaturas()
                .forEach(asignatura -> existentes.put(asignatura.codigo(), asignatura));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("codigo_asignatura");
    }

    @Override
    public Map<String, String> campos(Asignatura asignatura) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("codigo_carrera", asignatura.carrera());
        campos.put("codigo_version_malla", texto(asignatura.malla()));
        campos.put("nombre", asignatura.nombre());
        campos.put("ciclo", String.valueOf(asignatura.ciclo()));
        campos.put(
                "unidad_organizacion_curricular", asignatura.unidadOrganizacionCurricular().name());
        campos.put("horas_semanales_clase", numero(asignatura.horasSemanalesClase()));
        campos.put("horas_practicas_semanales", numero(asignatura.horasPracticasSemanales()));
        campos.put("creditos", numero(asignatura.creditos()));
        campos.put("tronco_comun", asignatura.troncoComun() ? "S" : "N");
        campos.put("campo_amplio", asignatura.campo().amplio());
        campos.put("campo_especifico", asignatura.campo().especifico());
        campos.put("palabras_clave", String.join("; ", asignatura.palabrasClave()));
        campos.put("estado", asignatura.activa() ? "ACTIVA" : "INACTIVA");
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("codigo_carrera", fila.valor("codigo_carrera"));
        campos.put("codigo_version_malla", fila.valor("codigo_version_malla"));
        campos.put("nombre", fila.valor("nombre"));
        campos.put("ciclo", String.valueOf(Integer.parseInt(fila.valor("ciclo"))));
        campos.put("unidad_organizacion_curricular", fila.valor("unidad_organizacion_curricular"));
        campos.put("horas_semanales_clase", numero(fila.valor("horas_semanales_clase")));
        campos.put("horas_practicas_semanales", numero(fila.valor("horas_practicas_semanales")));
        campos.put("creditos", numero(fila.valor("creditos")));
        campos.put("tronco_comun", fila.valor("tronco_comun"));
        campos.put("campo_amplio", fila.valor("campo_amplio"));
        campos.put("campo_especifico", fila.valor("campo_especifico"));
        campos.put("palabras_clave", String.join("; ", palabras(fila)));
        campos.put("estado", fila.valor("estado"));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        String carrera = fila.valor("codigo_carrera");
        String malla = fila.valor("codigo_version_malla");
        boolean hayMalla =
                !malla.isEmpty()
                        && repositorio.mallas().stream().map(Malla::codigo).anyMatch(malla::equals);
        if (!malla.isEmpty() && !hayMalla) {
            mantener.registrarMalla(carrera, malla, quien);
        }
        DatosDeAsignatura datos = datos(fila);
        mantener.registrarAsignatura(
                Asignatura.nueva(
                        clave(fila),
                        carrera,
                        nulo(malla),
                        datos.nombre(),
                        datos.ciclo(),
                        datos.unidadOrganizacionCurricular(),
                        datos.horasSemanalesClase(),
                        datos.horasPracticasSemanales(),
                        datos.creditos(),
                        datos.troncoComun(),
                        datos.campo(),
                        datos.palabrasClave()),
                quien);
        if ("INACTIVA".equals(fila.valor("estado"))) {
            mantener.cambiarEstadoAsignatura(clave(fila), false, quien);
        }
    }

    @Override
    public void corregir(FilaDelLote fila, Asignatura actual, String quien) {
        Map<String, String> ahora = campos(actual);
        Map<String, String> archivo = campos(fila);
        for (String campo : List.of("codigo_carrera", "codigo_version_malla")) {
            if (!ahora.get(campo).equals(archivo.get(campo))) {
                throw new DatoInvalido(
                        campo,
                        "La carrera y la malla de una asignatura no cambian: regístrala con otro"
                                + " código o corrige el archivo.");
            }
        }
        ahora.remove("estado");
        archivo.remove("estado");
        if (!ahora.equals(archivo)) {
            mantener.corregirAsignatura(clave(fila), datos(fila), quien);
        }
        boolean activa = !"INACTIVA".equals(fila.valor("estado"));
        if (actual.activa() != activa) {
            mantener.cambiarEstadoAsignatura(clave(fila), activa, quien);
        }
    }

    private static DatosDeAsignatura datos(FilaDelLote fila) {
        return new DatosDeAsignatura(
                fila.valor("nombre"),
                Integer.parseInt(fila.valor("ciclo")),
                UnidadDeOrganizacionCurricular.valueOf(
                        fila.valor("unidad_organizacion_curricular")),
                decimal(fila.valor("horas_semanales_clase")),
                decimal(fila.valor("horas_practicas_semanales")),
                decimal(fila.valor("creditos")),
                "S".equals(fila.valor("tronco_comun")),
                new CampoConocimiento(fila.valor("campo_amplio"), fila.valor("campo_especifico")),
                palabras(fila));
    }

    private static List<String> palabras(FilaDelLote fila) {
        return Arrays.stream(fila.valor("palabras_clave").split(";"))
                .map(String::strip)
                .filter(palabra -> !palabra.isEmpty())
                .toList();
    }
}
