package ec.mdd.academico.aplicacion.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Matricula;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.entrada.MantenerMatriculas;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Los traductores de las cargas 03, 04, 05 y 10 (F01-H04, FR-012), con Mockito. */
class TraductoresAcademicoBTest {

    private static final String HASH = "a".repeat(64);

    private static FilaDelLote fila(int numero, Map<String, String> valores) {
        return new FilaDelLote(numero, valores);
    }

    private static LoteDeCarga lote(String carga, FilaDelLote... filas) {
        return new LoteDeCarga(carga, HASH, List.of(filas));
    }

    @Test
    void enLosPeriodos_soloCuentanElNombreYLasFechas() {
        RepositorioPeriodos repositorio = mock(RepositorioPeriodos.class);
        RegistrarPeriodo registrar = mock(RegistrarPeriodo.class);
        TraductorPeriodos periodos =
                new TraductorPeriodos(repositorio, registrar, mock(CorregirPeriodo.class));
        when(repositorio.todos())
                .thenReturn(
                        List.of(
                                new PeriodoAcademico(
                                        "2026-I",
                                        "Periodo académico 2026-I",
                                        LocalDate.of(2026, 4, 6),
                                        LocalDate.of(2026, 8, 7),
                                        EstadoPeriodo.PLANIFICACION)));
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_periodo", "2026-I");
        valores.put("nombre", "Periodo académico 2026-I");
        valores.put("fecha_inicio", "2026-04-06");
        valores.put("fecha_fin", "2026-08-07");
        valores.put("semanas", "18");
        valores.put("resolucion_aprobacion", "RES-1");
        valores.put("estado", "APROBADO");

        assertThat(
                        MotorDelLote.previsualizar(
                                        periodos, lote("ciclos_academicos", fila(2, valores)))
                                .cuantos(TipoDeRegistro.IGUAL))
                .as("semanas, resolución y estado no cuentan")
                .isOne();
        valores.put("fecha_fin", "2026-08-14");
        assertThat(
                        MotorDelLote.previsualizar(
                                        periodos, lote("ciclos_academicos", fila(2, valores)))
                                .registros()
                                .getFirst()
                                .tipo())
                .isEqualTo(TipoDeRegistro.CAMBIA);
        valores.put("codigo_periodo", "2026-II");
        periodos.registrar(fila(2, valores), "datos.demo");
        verify(registrar)
                .registrar(
                        "2026-II",
                        "Periodo académico 2026-I",
                        LocalDate.of(2026, 4, 6),
                        LocalDate.of(2026, 8, 14),
                        "datos.demo");
    }

    private static Map<String, String> asignatura(String horas) {
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_carrera", "C1");
        valores.put("codigo_asignatura", "DS-B-I-MD-101");
        valores.put("nombre", "MATEMÁTICA DISCRETA");
        valores.put("ciclo", "1");
        valores.put("unidad_organizacion_curricular", "BASICA");
        valores.put("horas_semanales_clase", horas);
        valores.put("horas_practicas_semanales", "0");
        valores.put("creditos", "");
        valores.put("tronco_comun", "S");
        valores.put("campo_amplio", "05");
        valores.put("campo_especifico", "054");
        valores.put("palabras_clave", "matemática; lógica;conjuntos");
        valores.put("codigo_version_malla", "DS-2019");
        valores.put("estado", "ACTIVA");
        return valores;
    }

    @Test
    void enLasAsignaturas_3Y3_0SonIguales_yLaMallaSeRegistraAntes() {
        RepositorioCarreras repositorio = mock(RepositorioCarreras.class);
        MantenerCarreras mantener = mock(MantenerCarreras.class);
        TraductorAsignaturas asignaturas = new TraductorAsignaturas(repositorio, mantener);
        Asignatura registrada =
                Asignatura.nueva(
                        "DS-B-I-MD-101",
                        "C1",
                        "DS-2019",
                        "MATEMÁTICA DISCRETA",
                        1,
                        UnidadDeOrganizacionCurricular.BASICA,
                        new BigDecimal("3"),
                        BigDecimal.ZERO,
                        null,
                        true,
                        new CampoConocimiento("05", "054"),
                        List.of("matemática", "lógica", "conjuntos"));
        when(repositorio.asignaturas()).thenReturn(List.of(registrada));

        assertThat(
                        MotorDelLote.previsualizar(
                                        asignaturas,
                                        lote("asignaturas_malla", fila(2, asignatura("3.0"))))
                                .cuantos(TipoDeRegistro.IGUAL))
                .isOne();

        when(repositorio.mallas()).thenReturn(List.of());
        Map<String, String> otra = asignatura("3");
        otra.put("codigo_asignatura", "DS-B-I-MD-102");
        asignaturas.registrar(fila(2, otra), "datos.demo");
        verify(mantener).registrarMalla("C1", "DS-2019", "datos.demo");
        verify(mantener).registrarAsignatura(any(), eq("datos.demo"));

        when(repositorio.mallas()).thenReturn(List.of(Malla.nueva("DS-2019", "C1")));
        otra.put("codigo_asignatura", "DS-B-I-MD-103");
        asignaturas.registrar(fila(3, otra), "datos.demo");
        verify(mantener).registrarMalla(anyString(), anyString(), anyString());

        Map<String, String> otraCarrera = asignatura("3");
        otraCarrera.put("codigo_carrera", "C2");
        assertThatThrownBy(
                        () -> asignaturas.corregir(fila(2, otraCarrera), registrada, "datos.demo"))
                .isInstanceOfSatisfying(
                        DatoInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("codigo_carrera"));
        verify(mantener, never()).corregirAsignatura(anyString(), any(), anyString());
    }

    @Test
    void enLaOferta_lasBajasSoloEnLosPeriodosDelArchivo_yLasFranjasNoCuentan() {
        RepositorioOferta repositorio = mock(RepositorioOferta.class);
        TraductorOferta oferta = new TraductorOferta(repositorio, mock(MantenerOferta.class));
        AsignaturaParalelo a =
                AsignaturaParalelo.nueva(
                        "2026-I",
                        "A1",
                        "A_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        null,
                        16,
                        null);
        AsignaturaParalelo b =
                AsignaturaParalelo.nueva(
                        "2026-I",
                        "A2",
                        "A_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        null,
                        20,
                        null);
        when(repositorio.delPeriodo("2026-I")).thenReturn(List.of(a, b));
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_periodo", "2026-I");
        valores.put("codigo_asignatura", "A1");
        valores.put("paralelo", "A_NOCTURNA");
        valores.put("jornada", "NOCTURNA");
        valores.put("modalidad", "PRESENCIAL");
        valores.put("codigo_unidad_sede", "");
        valores.put("numero_estudiantes", "16");
        valores.put("horas_semanales", "");
        valores.put("franjas_horarias", "LUN|18:00|20:00");

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(oferta, lote("oferta_periodo", fila(2, valores)));

        assertThat(vista.cuantos(TipoDeRegistro.IGUAL)).isOne();
        assertThat(vista.cuantos(TipoDeRegistro.BAJA)).isOne();
        verify(repositorio, never()).delPeriodo("2026-II");
    }

    @Test
    void enLosMatriculados_laClaveEsPeriodoYCarrera() {
        RepositorioMatriculas repositorio = mock(RepositorioMatriculas.class);
        MantenerMatriculas mantener = mock(MantenerMatriculas.class);
        TraductorMatriculas matriculados = new TraductorMatriculas(repositorio, mantener);
        when(repositorio.deLosPeriodos(List.of("2026-I")))
                .thenReturn(
                        List.of(
                                new Matricula(
                                        "2026-I", "C1", 80, LocalDate.of(2026, 4, 30), "SIGA")));
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_periodo", "2026-I");
        valores.put("codigo_carrera", "C1");
        valores.put("total_matriculados", "85");
        valores.put("fecha_corte", "2026-04-30");
        valores.put("fuente", "SIGA");

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        matriculados, lote("matriculados_carrera", fila(2, valores)));

        assertThat(vista.registros().getFirst().clave()).isEqualTo("2026-I · C1");
        assertThat(vista.registros().getFirst().tipo()).isEqualTo(TipoDeRegistro.CAMBIA);
        matriculados.corregir(fila(2, valores), null, "datos.demo");
        verify(mantener)
                .corregir(
                        new Matricula("2026-I", "C1", 85, LocalDate.of(2026, 4, 30), "SIGA"),
                        "datos.demo");
    }
}
